package org.blackwalnutlabs.angel.aicar_initial.models;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;

//private void intro(Mat src, Mat dst) {
//        src.copyTo(dst);
//        DominoDetector.DominoResult result = dominoDetector.detect(src);
//        if (result.red != null) {
//        Core.rectangle(dst, new Point(result.red.x, result.red.y), new Point(result.red.x + result.red.width, result.red.y + result.red.height), new Scalar(255, 0, 0), 2);
//        }
//        if (result.green != null) {
//        Core.rectangle(dst, new Point(result.green.x, result.green.y), new Point(result.green.x + result.green.width, result.green.y + result.green.height), new Scalar(0, 255, 0), 2);
//        }
//        if (result.blue != null) {
//        Core.rectangle(dst, new Point(result.blue.x, result.blue.y), new Point(result.blue.x + result.blue.width, result.blue.y + result.blue.height), new Scalar(255, 0, 0), 2);
//        }
//        if (result.purple != null) {
//        Core.rectangle(dst, new Point(result.purple.x, result.purple.y), new Point(result.purple.x + result.purple.width, result.purple.y + result.purple.height), new Scalar(125, 0, 255), 2);
//        }
//        }

public class DominoDetector {

    private static final String LOG_TAG = DominoDetector.class.getSimpleName();

    private Mat emptyMat;
    private Mat zeroMat;
    private Mat tmp;
    private Mat dash;
    private Mat mask;
    private Mat mask2;

    private static int AREA_THRESHOLD = 20;

    private Mat kernel = Imgproc.getStructuringElement(MORPH_RECT, new Size(4, 4));

    public DominoDetector (){

        emptyMat = new Mat();
        tmp = new Mat();
        mask = new Mat();
        mask2 = new Mat();
        dash = new Mat();

    }
    public DominoResult detect(Mat input){

        DominoResult result = new DominoResult();

        emptyMat.copyTo(tmp);

        getHsvMask(input, mask, new Scalar(0, 43, 46), new Scalar(10, 255, 255));
        getHsvMask(input, mask2, new Scalar(156, 43, 46), new Scalar(180, 255, 255));
        Core.bitwise_or(mask, mask2, mask);
        result.red = getMaxAreaCenter(mask);

        getHsvMask(input, mask, new Scalar(35, 43, 46), new Scalar(77, 255, 255));
        result.green = getMaxAreaCenter(mask);

        getHsvMask(input, mask, new Scalar(100, 43, 46), new Scalar(124, 255, 255));
        result.blue = getMaxAreaCenter(mask);

        getHsvMask(input, mask, new Scalar(125, 43, 46), new Scalar(155, 255, 255));
        result.purple = getMaxAreaCenter(mask);

        return result;

    }

    private void getHsvMask(Mat src, Mat dst, Scalar lower, Scalar upper) {

        src.copyTo(tmp);
        emptyMat.copyTo(dst);
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(tmp, tmp, COLOR_RGB2HSV);
        Core.inRange(tmp, lower, upper, dst);
        Imgproc.erode(dst, dst, kernel);
        Imgproc.dilate(dst, dst, kernel);

    }

    private Rect getMaxAreaCenter(Mat src) {

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, dash, Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);

        double area = 0;
        int maxAreaIndex = 0;

        if (contours.size() > 0) {

            for (MatOfPoint contour : contours) {
                double currentArea = Imgproc.contourArea(contour);
                if (currentArea > area) {
                    area = currentArea;
                    maxAreaIndex = contours.indexOf(contour);
                }
            }

            if (area > AREA_THRESHOLD) {
                Rect rect = Imgproc.boundingRect(contours.get(maxAreaIndex));
                return rect;
            } else {
                return null;
            }

        } else {
            return null;
        }



    }


    public class DominoResult {
        public Rect red = null;
        public Rect blue = null;
        public Rect green = null;
        public Rect purple = null;
    }


}
