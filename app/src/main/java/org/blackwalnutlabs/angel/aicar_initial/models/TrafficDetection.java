package org.blackwalnutlabs.angel.aicar_initial.models;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.provider.ContactsContract;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.SeekBar;

import org.blackwalnutlabs.angel.aicar_initial.setting.ImageSetting;
import org.blackwalnutlabs.angel.aicar_initial.setting.TensorFlowSetting;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrafficDetection {

    private static final String TAG = "traffic";


    private TensorFlowInferenceInterface inferenceInterface;

    public TrafficDetection(Map<String, Object> tmpMap,
                            Map<String, Object> funMap,
                            Map<String, SeekBar> debugMap,
                            Map<String, Object> othersMap) {
        inferenceInterface = new TensorFlowInferenceInterface((AssetManager) othersMap.get("assets"),
                TensorFlowSetting.MODELFILE);
    }

    public Map<String, Object> detectTrafficSign(Mat src) {
        if (false) {
            Mat rightMat = new Mat(src, new Rect(new Point(ImageSetting.MAXWIDTH - ImageSetting.MAXHEIGHT, 0),
                    new Point(ImageSetting.MAXWIDTH, ImageSetting.MAXHEIGHT)));

            Bitmap bmp = Bitmap.createBitmap(rightMat.width(), rightMat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rightMat, bmp, true);


            int[] intValues = new int[rightMat.width() * rightMat.height()];
            byte[] byteValues = new byte[rightMat.width() * rightMat.height() * 3];
            bmp.getPixels(intValues, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                byteValues[i * 3] = (byte) ((val >> 16) & 0xFF);
                byteValues[i * 3 + 1] = (byte) ((val >> 8) & 0xFF);
                byteValues[i * 3 + 2] = (byte) ((val & 0xFF));
            }

            String[] outputNames = TensorFlowSetting.OUTPUTNAMES;
            float[] detection_boxes = new float[400];
            float[] detection_scores = new float[100];
            float[] detection_classes = new float[100];
            float[] num_detections = new float[1];

            // Copy the input data into TensorFlow.
            inferenceInterface.feed(TensorFlowSetting.INPUTNAME, byteValues, 1, rightMat.height(), rightMat.width(), 3);

            // Run the inference call.
            inferenceInterface.run(outputNames, false);

            inferenceInterface.fetch(outputNames[0], detection_boxes);
            inferenceInterface.fetch(outputNames[1], detection_scores);
            inferenceInterface.fetch(outputNames[2], detection_classes);
            inferenceInterface.fetch(outputNames[3], num_detections);

            Map<String, Object> resultObj = new ArrayMap<>();
            List<Rect> boxes = new ArrayList<>();
            List<String> classes = new ArrayList<>();
            resultObj.put("boxes", boxes);
            resultObj.put("classes", classes);

            Log.e(TAG, Arrays.toString(detection_scores));

            for (int i = 0; i < 100; i++) {
                if (detection_scores[i] > 0.4) {
                    Rect objRect = new Rect();
                    objRect.x = (int)detection_boxes[i * 4];
                    objRect.y = (int)detection_boxes[i * 4 + 1];
                    objRect.width = (int)detection_boxes[i * 4 + 2];
                    objRect.height = (int)detection_boxes[i * 4 + 3];
//                    boxes.add(detection_boxes[i * 4]);
//                    boxes.add(detection_boxes[i * 4 + 1]);
//                    boxes.add(detection_boxes[i * 4 + 2]);
//                    boxes.add(detection_boxes[i * 4 + 3]);
                    boxes.add(objRect);
                    classes.add(TensorFlowSetting.HANDMAP[((int) detection_classes[i]) - 1] + ": " + String.format(Locale.getDefault(), "%.2f", detection_scores[i]) + "\n");
                }
            }

            return resultObj;
        }
        return null;
    }

    private Bitmap convertMatToBitmap(Mat mat) {
        Bitmap bmp = null;
        try {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGBA, 4);
            bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bmp);
        }
        catch (CvException e){Log.d("Exception",e.getMessage());}
        return bmp;
    }
}
