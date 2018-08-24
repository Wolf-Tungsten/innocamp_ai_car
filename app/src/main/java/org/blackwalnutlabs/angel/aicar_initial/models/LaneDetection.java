package org.blackwalnutlabs.angel.aicar_initial.models;

import android.widget.SeekBar;

import org.blackwalnutlabs.angel.aicar_initial.setting.ImageSetting;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.blackwalnutlabs.angel.aicar_initial.setting.ImageSetting.MAXHEIGHT;
import static org.blackwalnutlabs.angel.aicar_initial.setting.ImageSetting.MAXWIDTH;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;
import static org.opencv.imgproc.Imgproc.RETR_TREE;


public class LaneDetection {
    private static final String TAG = "LaneDetection";

    private Mat[] tmpMats;
    private Mat emptyMat;
    private Mat zeroMat;

    private MatOfPoint[] tmpMatOfPoints;
    private MatOfPoint emptyMatOfPoint;

    private SeekBar threshold1;
    private SeekBar threshold2;

    private Mat kernel;

    public LaneDetection(Map<String, Object> tmpMap, Map<String, Object> funMap, Map<String, SeekBar> debugMap, Map<String, Object> othersMap) {
        tmpMats = (Mat[]) tmpMap.get("Mat");
        tmpMatOfPoints = (MatOfPoint[]) tmpMap.get("MatOfPoint");

        emptyMat = (Mat) funMap.get("EmptyMat");
        emptyMatOfPoint = (MatOfPoint) funMap.get("EmptyMatOfPoint");
        zeroMat = (Mat) funMap.get("ZeroMat");

        kernel = (Mat) othersMap.get("kernel");

        threshold1 = debugMap.get("threshold1");
        threshold2 = debugMap.get("threshold2");
    }

    public int process(Mat src, Mat dst) {
        src.copyTo(dst);

        // 识别黄色
        Mat maskYellow = tmpMats[1];
        emptyMat.copyTo(maskYellow);
        Imgproc.cvtColor(src, maskYellow, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(maskYellow, maskYellow, COLOR_RGB2HSV);
        Scalar lowerYellow = new Scalar(11, threshold1.getProgress() + 1,
                threshold1.getProgress() + 1);
        Scalar upperYellow = new Scalar(34, 255, 255);
        Core.inRange(maskYellow, lowerYellow, upperYellow, maskYellow);
        Imgproc.erode(maskYellow, maskYellow, kernel);
        Imgproc.dilate(maskYellow, maskYellow, kernel);

        // 识别白色
        Mat maskWhite = tmpMats[2];
        emptyMat.copyTo(maskWhite);
        Imgproc.cvtColor(src, maskWhite, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(maskWhite, maskWhite, COLOR_RGB2HSV);
        Scalar lowerWhite = new Scalar(0, 0, 255 - (threshold2.getProgress() + 1));
        Scalar upperWhite = new Scalar(255, threshold2.getProgress() + 1, 255);
        Core.inRange(maskWhite, lowerWhite, upperWhite, maskWhite);
        Imgproc.erode(maskWhite, maskWhite, kernel);
        Imgproc.dilate(maskWhite, maskWhite, kernel);

        // ROI
        Mat maskROI = tmpMats[3];
        zeroMat.copyTo(maskROI);

        List<Point> roiPoints = new ArrayList<>();
        roiPoints.add(new Point(0, MAXHEIGHT));
        roiPoints.add(new Point(MAXWIDTH, MAXHEIGHT));
        roiPoints.add(new Point(MAXWIDTH / 2 + MAXWIDTH / 8, MAXHEIGHT / 2 - MAXHEIGHT / 9));
        roiPoints.add(new Point(MAXWIDTH / 2 - MAXWIDTH / 8, MAXHEIGHT / 2 - MAXHEIGHT / 9));
        List<MatOfPoint> pts = new ArrayList<>();
        MatOfPoint roiMat = tmpMatOfPoints[0];
        emptyMatOfPoint.copyTo(roiMat);
        roiMat.fromList(roiPoints);
        pts.add(roiMat);
        Core.fillPoly(maskROI, pts, new Scalar(255));
        Core.bitwise_and(maskYellow, maskROI, maskYellow);
        Core.bitwise_and(maskWhite, maskROI, maskWhite);

        Mat lineImg = tmpMats[4];
        emptyMat.copyTo(lineImg);

        // Yellow Lane
        double targetLX1 = -1;
        double avgDetY = 0;
        Imgproc.HoughLinesP(maskYellow, lineImg, 4, Math.PI / 180, 30, 60, 180);
        if (lineImg.cols() > 0) {
            double det_slope = 0.4;
            int cnt = 0;
            for (int i = 0; i < lineImg.cols(); i++) {
                double[] line = lineImg.get(0, i);
                if (line[0] != line[2] && line[1] != line[3]) {
                    double det = (line[3] - line[1]) / (line[2] - line[0]);
                    if (Math.abs(det) > det_slope) {
                        avgDetY += det;
                        cnt++;
                    }
                }
            }
            if (cnt != 0) {
                avgDetY /= cnt;
                List<MatOfPoint> contours = new ArrayList<>();
                Mat _1 = tmpMats[5];
                Imgproc.findContours(maskYellow, contours, _1, RETR_TREE, CHAIN_APPROX_SIMPLE);
                if (contours.size() > 0) {
                    int mID = 0;
                    double mA = Imgproc.contourArea(contours.get(0));
                    for (int i = 1; i < contours.size(); i++) {
                        double a = Imgproc.contourArea(contours.get(i));
                        if (mA < a) {
                            mA = a;
                            mID = i;
                        }
                    }
                    Moments M = Imgproc.moments(contours.get(mID));
                    int cx = (int) (M.get_m10() / M.get_m00());
                    int cy = (int) (M.get_m01() / M.get_m00());

                    double b = cy - avgDetY * cx;
                    Core.line(dst, new Point((ImageSetting.MAXHEIGHT - b) / avgDetY, ImageSetting.MAXHEIGHT),
                            new Point(-b / avgDetY, 0), new Scalar(0, 255, 0), 2);

                    targetLX1 = (ImageSetting.MAXHEIGHT - b) / avgDetY;

                    for (int i = 0; i < contours.size(); i++) {
                        contours.get(i).release();
                    }
                }
            }
        }

        // White Lane
        double targetRX1 = -1;
        double avgDetW = 0;
        Imgproc.HoughLinesP(maskWhite, lineImg, 4, Math.PI / 180, 30, 60, 180);
        if (lineImg.cols() > 0) {
            double det_slope = 0.4;
            int cnt = 0;
            for (int i = 0; i < lineImg.cols(); i++) {
                double[] line = lineImg.get(0, i);
                if (line[0] != line[2] && line[1] != line[3]) {
                    double det = (line[3] - line[1]) / (line[2] - line[0]);
                    if (Math.abs(det) > det_slope) {
                        avgDetW += det;
                        cnt++;
                    }
                }
            }
            if (cnt != 0) {
                avgDetW /= cnt;
                List<MatOfPoint> contours = new ArrayList<>();
                Mat _1 = tmpMats[5];
                Imgproc.findContours(maskWhite, contours, _1, RETR_TREE, CHAIN_APPROX_SIMPLE);
                if (contours.size() > 0) {
                    int mID = 0;
                    double mA = Imgproc.contourArea(contours.get(0));
                    for (int i = 1; i < contours.size(); i++) {
                        double a = Imgproc.contourArea(contours.get(i));
                        if (mA < a) {
                            mA = a;
                            mID = i;
                        }
                    }
                    Moments M = Imgproc.moments(contours.get(mID));
                    int cx = (int) (M.get_m10() / M.get_m00());
                    int cy = (int) (M.get_m01() / M.get_m00());

                    double b = cy - avgDetW * cx;
                    Core.line(dst, new Point((ImageSetting.MAXHEIGHT - b) / avgDetW,
                            ImageSetting.MAXHEIGHT), new Point(-b / avgDetW, 0), new Scalar(0, 0, 255), 2);
                    targetRX1 = (ImageSetting.MAXHEIGHT - b) / avgDetW;
                }
            }
        }

        if (targetLX1 == -1 && targetRX1 != -1) {
            return 1;
        }

        if (targetRX1 == -1 && targetLX1 != -1) {
            return 2;
        }

        return 0;
    }
}
