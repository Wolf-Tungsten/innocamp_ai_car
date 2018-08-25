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

public class FollowDetection {

    private static final String LOG_TAG = FollowDetection.class.getSimpleName();


    private Mat[] tmpMats;
    private Mat emptyMat;
    private Mat zeroMat;

    private MatOfPoint[] tmpMatOfPoints;
    private MatOfPoint emptyMatOfPoint;


    private Mat kernel;

    public  FollowDetection(Activity activity, Map<String, Object> tmpMap, Map<String, Object> funMap, Map<String, Object> othersMap)  {
        tmpMats = (Mat[]) tmpMap.get("Mat");
        tmpMatOfPoints = (MatOfPoint[]) tmpMap.get("MatOfPoint");

        emptyMat = (Mat) funMap.get("EmptyMat");
        emptyMatOfPoint = (MatOfPoint) funMap.get("EmptyMatOfPoint");
        zeroMat = (Mat) funMap.get("ZeroMat");

        kernel = (Mat) othersMap.get("kernel");
    }


    public FollowDetectResult detect(Mat src, Mat dst) {

        src.copyTo(dst);
        // 识别绿色
        Mat mask = tmpMats[1];
        emptyMat.copyTo(mask);
        Imgproc.cvtColor(src, mask, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(mask, mask, COLOR_RGB2HSV);
        Scalar lower = new Scalar(35, 45, 50);
        Scalar upper = new Scalar(77, 255, 255);
        Core.inRange(mask, lower, upper, mask);
        Imgproc.erode(mask, mask, kernel);
        Imgproc.dilate(mask, mask, kernel);
        mask.copyTo(dst);

        // 查找轮廓
        List<MatOfPoint> contours = new ArrayList<>();
        Mat _1 = tmpMats[5];
        Imgproc.findContours(mask, contours, _1,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);


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

            FollowDetectResult result = new FollowDetectResult();
            result.area = maxArea;
            Rect rect = Imgproc.boundingRect(contours.get(maxAreaIndex));
            result.center = new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);

            return result;
        } else {
            return null;
        }

    }

    public class FollowDetectResult {
        public Point center = null;
        public double area = 0;
    }

}

