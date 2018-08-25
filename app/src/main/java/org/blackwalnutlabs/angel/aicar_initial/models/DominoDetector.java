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

public class DominoDetector {

    private static final String LOG_TAG = DominoDetector.class.getSimpleName();

    private Mat emptyMat;
    private Mat zeroMat;
    private Mat tmp;
    private Mat dash;
    private Mat mask;

    private static int AREA_THRESHOLD = 20;

    private Mat kernel = Imgproc.getStructuringElement(MORPH_RECT, new Size(4, 4));

    public DominoResult detect(Mat input, Mat dst){
        DominoResult result = new DominoResult();

        input.copyTo(dst);

        getHsvMask(input, mask, new Scalar(0, 43, 46), new Scalar(10, 255, 255));
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
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(dst, contours, dash ,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);

    }

    private Point getMaxAreaCenter(Mat src) {

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, dash,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);
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
            if (maxAreaIndex > AREA_THRESHOLD) {
                Rect rect = Imgproc.boundingRect(contours.get(maxAreaIndex));
                return new Point(rect.x + 0.5 * rect.width, rect.y + 0.5 * rect.height);
            } else {
                return null;
            }

        } else {
            return null;
        }



    }


    public class DominoResult {
        public Point red = null;
        public Point blue = null;
        public Point green = null;
        public Point purple = null;
    }


}
