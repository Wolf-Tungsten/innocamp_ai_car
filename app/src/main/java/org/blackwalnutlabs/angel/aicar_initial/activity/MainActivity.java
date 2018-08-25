package org.blackwalnutlabs.angel.aicar_initial.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.blackwalnutlabs.angel.aicar_initial.R;
import org.blackwalnutlabs.angel.aicar_initial.bth.BluetoothService;
import org.blackwalnutlabs.angel.aicar_initial.bth.conn.BleCharacterCallback;
import org.blackwalnutlabs.angel.aicar_initial.bth.exception.BleException;
import org.blackwalnutlabs.angel.aicar_initial.models.FollowDetection;
import org.blackwalnutlabs.angel.aicar_initial.models.MnistClassifier;
import org.blackwalnutlabs.angel.aicar_initial.models.TrafficDetection;
import org.blackwalnutlabs.angel.aicar_initial.setting.MQTTSetting;
import org.blackwalnutlabs.angel.aicar_initial.util.BWMQTTClient;
import org.blackwalnutlabs.angel.aicar_initial.util.PermissionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.blackwalnutlabs.angel.aicar_initial.setting.ImageSetting.MAXHEIGHT;
import static org.blackwalnutlabs.angel.aicar_initial.setting.ImageSetting.MAXWIDTH;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;

/**
 * Created by Angel Zheng on 2018/8/22.
 */

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    /**
     * Config
     */
    private String carID = "";
    private String track = "";

    private int rightSpeed = 0;
    private int leftSpeed = 0;
    private int mode = 0;
    /**
     * System
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        initBlueTooth();
        initDebug();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        } else {
            initCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
        unbindService();
    }

    /**
     * Debug
     */
    private SeekBar threshold1;
    private SeekBar threshold2;
    private TextView areaTextView;
    private TextView xTextView;
    private TextView yTextView;

    private void initDebug() {
        threshold1 = findViewById(R.id.threshold1);
        TextView thresholdView1 = findViewById(R.id.thresholdView1);
        threshold1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdView1.setText(String.valueOf(progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        threshold1.setMax(254);
        threshold1.setProgress(50);

        TextView thresholdView2 = findViewById(R.id.thresholdView2);
        threshold2 = findViewById(R.id.threshold2);
        threshold2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdView2.setText(String.valueOf(progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        threshold2.setMax(254);
        threshold2.setProgress(50);

        areaTextView = (TextView) findViewById(R.id.area);
        xTextView = (TextView) findViewById(R.id.center_x);
        yTextView = (TextView) findViewById(R.id.center_y);
    }

    /**
     * Permission
     */
    private void requestPermission() {
        PermissionUtils.requestMultiPermissions(this, mPermissionGrant);
    }

    private PermissionUtils.PermissionGrant mPermissionGrant = requestCode -> {
        switch (requestCode) {
            default:
                Toast.makeText(MainActivity.this, "Result Permission Grant CODE_MULTI_PERMISSION", Toast.LENGTH_SHORT).show();
                break;
        }
    };

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionUtils.requestPermissionsResult(this, requestCode, permissions, grantResults, mPermissionGrant);
        initCamera();
    }

    /**
     * BlueTooth
     */
    private BluetoothService mBluetoothService;
    private BluetoothGattService service;

    private void initBlueTooth() {
        bindService();
    }

    private Handler bthHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                getService();
                BLE_start_listener();
            } else if (msg.what == 2) {
                BLE_start_writer();
            } else if (msg.what == 3) {
                writer(mode, leftSpeed, rightSpeed);
            }
            super.handleMessage(msg);
        }
    };

    public String protocol(int protocol_data) {
        String protocol_msg = "";
        if (protocol_data >= 0 && 100 > protocol_data) {
            protocol_msg = Integer.toHexString(30) + Integer.toHexString(protocol_data + 30);
        } else if (protocol_data < 10000 && protocol_data >= 100) {
            protocol_msg = Integer.toHexString(protocol_data / 100 + 30) + Integer.toHexString(protocol_data % 100 + 30);
        }
        return protocol_msg;
    }

    private void writer(int controlMode, int targetL, int targetR) { //仅仅用于发送小车动作控制命令
        final BluetoothGattCharacteristic characteristic = mBluetoothService.getCharacteristic();
        mBluetoothService.write(
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                String.valueOf(protocol(controlMode) + protocol(targetL) + protocol(targetR) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(4321)),//发送10进制比特
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        //成功写入操作
                    }

                    @Override
                    public void onFailure(final BleException exception) {
                        StartBLEWriterAfter(150);
                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }  //写数据

    private void writer(int targetL, int targetR) { //仅仅用于发送小车动作控制命令
        final BluetoothGattCharacteristic characteristic = mBluetoothService.getCharacteristic();
        mBluetoothService.write(
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                String.valueOf(protocol(5) + protocol(targetL) + protocol(targetR) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(4321)),//发送10进制比特
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        //成功写入操作
                    }

                    @Override
                    public void onFailure(final BleException exception) {
                        StartBLEWriterAfter(150);
                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }  //写数据

    private void StartBLEListenerAfter(int time) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                Message msg = new Message();
                msg.what = 1;
                bthHandler.sendMessage(msg);
            }
        };
        timer.schedule(task, time);
    }

    private void StartBLEWriterAfter(int time) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                Message msg = new Message();
                msg.what = 2;
                bthHandler.sendMessage(msg);
            }
        };
        timer.schedule(task, time);
    }

    private void getService() {
        BluetoothGatt gatt = mBluetoothService.getGatt();
        mBluetoothService.setService(gatt.getServices().get(gatt.getServices().size() - 1));
    }

    private void BLE_start_writer() {
        service = mBluetoothService.getService();
        mBluetoothService.setCharacteristic((service.getCharacteristics().get(service.getCharacteristics().size() - 2)));
        mBluetoothService.setCharaProp(1);
    }

    private void BLE_start_listener() {
        service = mBluetoothService.getService();
        mBluetoothService.setCharacteristic((service.getCharacteristics().get(service.getCharacteristics().size() - 1)));
        mBluetoothService.setCharaProp(2);
        final BluetoothGattCharacteristic characteristic = mBluetoothService.getCharacteristic();
        mBluetoothService.notify(
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                new BleCharacterCallback() {

                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        MainActivity.this.runOnUiThread(() -> {
                        });
                    }

                    @Override
                    public void onFailure(final BleException exception) {
                        MainActivity.this.runOnUiThread(() -> {
                            StartBLEListenerAfter(100);//重新开始监听
                        });
                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }

    private void bindService() {
        Intent bindIntent = new Intent(this, BluetoothService.class);
        this.bindService(bindIntent, mFhrSCon, Context.BIND_AUTO_CREATE);
        StartBLEListenerAfter(100);
        StartBLEWriterAfter(150);
    }

    private ServiceConnection mFhrSCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothService = ((BluetoothService.BluetoothBinder) service).getService();
            mBluetoothService.setConnectCallback(callback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
        }
    };

    private BluetoothService.Callback2 callback = this::finish;

    private void unbindService() {
        this.unbindService(mFhrSCon);
    }

    private Handler controlHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == -1) {
                writer(0, 0);
            } else if (msg.what == 0) {
                writer(50, 50);
            } else if (msg.what == 1) {
                writer(50, 100);
            } else if (msg.what == 2) {
                writer(100, 50);
            }
            super.handleMessage(msg);
        }
    };

    /**
     * MQTT
     */
    private BWMQTTClient bwmqttClient;

    private void initMQTT() {
        Handler mqttConfig = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MQTTSetting.MQTT_STATE_CONNECTED) {
                    Log.d(TAG, "连接成功");
                } else if (msg.what == MQTTSetting.MQTT_STATE_LOST) {
                    Log.d(TAG, "连接丢失，进行重连");
                } else if (msg.what == MQTTSetting.MQTT_STATE_FAIL) {
                    Log.d(TAG, "连接失败");
                } else if (msg.what == MQTTSetting.MQTT_STATE_RECEIVE) {
                    Log.d(TAG, "收到数据:" + msg.obj);
                }
                super.handleMessage(msg);
            }
        };
        bwmqttClient = new BWMQTTClient(mqttConfig);
    }

    private void sendPlaceMsg(int d) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("carID", carID);
            jsonObject.put("d", d);
            jsonObject.put("track", track);
//            bwmqttClient.pubMsg("InnoCamp18/Place", jsonObject.toString());
            Log.e(TAG, jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Computer Vision
     */
    private CameraBridgeViewBase openCvCameraView;
    private MnistClassifier mnistClassifier;
    private TrafficDetection trafficDetection;
    private FollowDetection followDetection;

    private Mat[] tmpMats;
    private Mat emptyMat;
    private Mat zeroMat;

    private MatOfPoint[] tmpMatOfPoints;
    private MatOfPoint emptyMatOfPoint;

    private Mat kernel;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    private void initCamera() {
        openCvCameraView = findViewById(R.id.HelloOpenCvView);
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.setMaxFrameSize(MAXWIDTH, MAXHEIGHT);
        openCvCameraView.enableFpsMeter();
        openCvCameraView.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        int cnt = 7;
        tmpMats = new Mat[cnt];
        for (int i = 0; i < cnt; i++) {
            tmpMats[i] = new Mat();
        }

        cnt = 1;
        tmpMatOfPoints = new MatOfPoint[cnt];
        for (int i = 0; i < cnt; i++) {
            tmpMatOfPoints[i] = new MatOfPoint();
        }

        emptyMat = new Mat();
        emptyMatOfPoint = new MatOfPoint();
        zeroMat = new Mat(new Size(width, height), CvType.CV_8U);

        kernel = Imgproc.getStructuringElement(MORPH_RECT, new Size(4, 4));

        initModel();
    }

    private void initModel() {
        Map<String, Object> tmpMap = new HashMap<>();
        tmpMap.put("Mat", tmpMats);
        tmpMap.put("MatOfPoint", tmpMatOfPoints);

        Map<String, Object> funMap = new HashMap<>();
        funMap.put("EmptyMat", emptyMat);
        funMap.put("EmptyMatOfPoint", emptyMatOfPoint);
        funMap.put("ZeroMat", zeroMat);

        Map<String, SeekBar> debugMap = new HashMap<>();
        debugMap.put("threshold1", threshold1);
        debugMap.put("threshold2", threshold2);

        Map<String, Object> othersMap = new HashMap<>();
        othersMap.put("kernel", kernel);
        othersMap.put("assets", getAssets());

        followDetection = new FollowDetection(this, tmpMap, funMap, othersMap);

    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat input = inputFrame.rgba();
        Mat dst = tmpMats[0];
        emptyMat.copyTo(dst);
        follow(input, dst);
        return dst;
    }

    private void follow(Mat src, Mat dst) {
        int upperArea = 0;
        int lowerArea = 0;
        FollowDetection.FollowDetectResult detectionResult = followDetection.detect(src, dst);
        if (detectionResult != null) {
            areaTextView.setText(String.format("面积：%.02f", detectionResult.area));
            xTextView.setText(String.format("x：%d", (int)detectionResult.center.x));
            yTextView.setText(String.format("y：%d", (int)detectionResult.center.y));
            if (detectionResult.area > 150000) {
                mode = 5;
                leftSpeed = 0;
                rightSpeed = 0;
            } else{
                if (detectionResult.center.x > 320 + 50) {
                    mode = 5;
                    leftSpeed = 120;
                    rightSpeed = 70;
                } else if (detectionResult.center.x < 320 - 50) {
                    mode = 5;
                    leftSpeed = 70;
                    rightSpeed = 120;
                } else {
                    mode = 5;
                    leftSpeed = 120;
                    rightSpeed = 120;
                }
            }

            Message msg = new Message();
            msg.what = 3;
            bthHandler.sendMessage(msg);

        } else {
            mode = 6;
            leftSpeed = 120;
            rightSpeed = 120;
            Message msg = new Message();
            msg.what = 3;
            bthHandler.sendMessage(msg);
        }
    }
}