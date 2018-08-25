package org.blackwalnutlabs.angel.aicar_initial.models;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import android.widget.SeekBar;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;
import static org.opencv.imgproc.Imgproc.resize;

public class MnistClassifier {

    private static final String LOG_TAG = MnistClassifier.class.getSimpleName();

    private static final String MODEL_PATH = "mnist.tflite";

    private static final int DIM_BATCH_SIZE = 1;
    public static final int DIM_IMG_SIZE_HEIGHT = 28;
    public static final int DIM_IMG_SIZE_WIDTH = 28;
    private static final int DIM_PIXEL_SIZE = 1;
    private static final int CATEGORY_COUNT = 10;
    private static final int BELIEVE_COUNT = 20;


    private LinkedList<Integer> numberQueue;
    private int[] numberCounter;
    private Interpreter mInterpreter = null;
    private final ByteBuffer mImgData;
    private final int[] mImagePixels = new int[DIM_IMG_SIZE_HEIGHT * DIM_IMG_SIZE_WIDTH];
    private final float[][] mResult = new float[1][CATEGORY_COUNT];

    private Mat[] tmpMats;
    private Mat emptyMat;
    private Mat zeroMat;

    private MatOfPoint[] tmpMatOfPoints;
    private MatOfPoint emptyMatOfPoint;


    private Mat kernel;

    public  MnistClassifier(Activity activity, Map<String, Object> tmpMap, Map<String, Object> funMap, Map<String, Object> othersMap)  {
        tmpMats = (Mat[]) tmpMap.get("Mat");
        tmpMatOfPoints = (MatOfPoint[]) tmpMap.get("MatOfPoint");

        emptyMat = (Mat) funMap.get("EmptyMat");
        emptyMatOfPoint = (MatOfPoint) funMap.get("EmptyMatOfPoint");
        zeroMat = (Mat) funMap.get("ZeroMat");

        kernel = (Mat) othersMap.get("kernel");

        try {
            mInterpreter = new Interpreter(loadModelFile(activity));
        } catch (IOException e) {
            Log.d("mnistClassifier", "模型读取出错");
        }

        mImgData = ByteBuffer.allocateDirect(
                4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_HEIGHT * DIM_IMG_SIZE_WIDTH * DIM_PIXEL_SIZE);
        mImgData.order(ByteOrder.nativeOrder());

        numberQueue = new LinkedList<>();
        numberCounter = new int[10];
        for (int i = 0; i < numberCounter.length; i++) {
            numberCounter[i] = 0;
        }
    }

    public int recognize(Mat src, Mat dst){
        Mat digit = pickNumber(src, dst);
        Mat resized = new MatOfPoint();
        if (digit != null) {
            Imgproc.resize(digit, resized , new Size(DIM_IMG_SIZE_WIDTH, DIM_IMG_SIZE_HEIGHT));
            Bitmap  digitBitmap = convertMatToBitmap(resized);
            Result result = classify(digitBitmap);
            int currentResult =  result.getNumber();

            numberCounter[currentResult]++;
            numberQueue.offer(currentResult);
            if (numberQueue.size() > BELIEVE_COUNT) {
                numberCounter[numberQueue.poll()] --;
            }

            int currentMax = 0;
            int currentMaxResult = 0;
            for (int i = 0; i < numberCounter.length; i++) {
                if (numberCounter[i] > currentMax) {
                    currentMax = numberCounter[i];
                    currentMaxResult = i;
                }
            }

            return currentMaxResult;
        } else {
            // 不存在的时候清空统计
            for (int i = 0; i < numberCounter.length; i++) {
                numberCounter[i] = 0;
            }
            numberQueue.clear();
            return -1;
        }

    }
    public Result classify(Bitmap bitmap) {

        // Step.2 If exist, try to recognize digit.
        convertBitmapToByteBuffer(bitmap);
        long startTime = SystemClock.uptimeMillis();
        mInterpreter.run(mImgData, mResult);
        long endTime = SystemClock.uptimeMillis();
        long timeCost = endTime - startTime;
        Log.v(LOG_TAG, "run(): result = " + Arrays.toString(mResult[0])
                + ", timeCost = " + timeCost);
        return new Result(mResult[0], timeCost);
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

    public Mat pickNumber(Mat src, Mat dst) {
        src.copyTo(dst);

        // 识别蓝色
        Mat maskBlue = tmpMats[1];
        emptyMat.copyTo(maskBlue);
        Imgproc.cvtColor(src, maskBlue, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(maskBlue, maskBlue, COLOR_RGB2HSV);
        Scalar lowerBlue = new Scalar(100, 45, 50);
        Scalar upperBlue = new Scalar(124, 255, 255);
        Core.inRange(maskBlue, lowerBlue, upperBlue, maskBlue);
        Imgproc.erode(maskBlue, maskBlue, kernel);
        Imgproc.dilate(maskBlue, maskBlue, kernel);
        //maskBlue.copyTo(dst);

        // 查找轮廓
        List<MatOfPoint> contours = new ArrayList<>();
        Mat _1 = tmpMats[5];
        Imgproc.findContours(maskBlue, contours, _1,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);


        if ( contours.size() > 0 ) {
            // 查找最大轮廓
            int maxAreaIndex = 0;
            double maxArea = Imgproc.contourArea(contours.get(0));
            for (int i = 1; i < contours.size(); i++) {
                double currentArea = Imgproc.contourArea(contours.get(i));
                if (currentArea > maxArea) {
                    maxAreaIndex = i;
                    maxArea = currentArea;
                }
            }

            Rect rect = Imgproc.boundingRect(contours.get(maxAreaIndex));
            Rect rect1V1 = new Rect();
            rect1V1.x = rect.x + rect.width / 2 - Integer.max(rect.height, rect.width)/2;
            rect1V1.y = rect.y + rect.height / 2 - Integer.max(rect.height, rect.width)/2;
            rect1V1.height = Integer.max(rect.height, rect.width);
            rect1V1.width = Integer.max(rect.height, rect.width);
            if (rect1V1.x > 0 && rect1V1.y > 0 && rect1V1.x + rect1V1.width < 640 && rect1V1.y + rect1V1.height < 480) {
                Core.rectangle(dst, new Point(rect1V1.x, rect1V1.y), new Point(rect1V1.x + rect1V1.width, rect1V1.y + rect1V1.height), new Scalar(0, 0, 255), 2);
                return new Mat(maskBlue, rect1V1);
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (mImgData == null) {
            return;
        }
        mImgData.rewind();

        bitmap.getPixels(mImagePixels, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_WIDTH; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_HEIGHT; ++j) {
                final int val = mImagePixels[pixel++];
                mImgData.putFloat(convertToGreyScale(val));
            }
        }
    }

    private float convertToGreyScale(int color) {
        return (((color >> 16) & 0xFF) + ((color >> 8) & 0xFF) + (color & 0xFF)) / 3.0f / 255.0f;
    }
}


class Result {

    private final int mNumber;
    private final float mProbability;
    private final long mTimeCost;

    public Result(float[] result, long timeCost) {
        mNumber = argmax(result);
        mProbability = result[mNumber];
        mTimeCost = timeCost;
    }

    public int getNumber() {
        return mNumber;
    }

    public float getProbability() {
        return mProbability;
    }

    public long getTimeCost() {
        return mTimeCost;
    }

    private static int argmax(float[] probs) {
        int maxIdx = -1;
        float maxProb = 0.0f;
        for (int i = 0; i < probs.length; i++) {
            if (probs[i] > maxProb) {
                maxProb = probs[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

}