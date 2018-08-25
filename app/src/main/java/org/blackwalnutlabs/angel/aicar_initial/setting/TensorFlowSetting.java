package org.blackwalnutlabs.angel.aicar_initial.setting;

public class TensorFlowSetting {
    public static final String INPUTNAME = "image_tensor";
    public static final String[] OUTPUTNAMES = {"detection_boxes", "detection_scores", "detection_classes", "num_detections"};
    public static final String MODELFILE = "file:///android_asset/ssd_mobile_v2.pb";
    public static final String HANDMAP[] = {"gostraightturnright", "gostraight", "hornplease", "turnaround", "turnright", "sidewalk", "pedestrian", "slow", "turnleft", "vehicles", "greenlight","leftsign","people","rightsign","redlight","stopsign","straightsign"};
    public static final String PEOPLEMAP[] = {"red-domino", "blue-domino", "purple-domino", "green-domino"};
}

