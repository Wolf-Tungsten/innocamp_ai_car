package org.blackwalnutlabs.angel.aicar_initial.util;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.blackwalnutlabs.angel.aicar_initial.setting.MQTTSetting;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


/**
 * Created by MichaelJiang on 2017/3/16.
 */

public class BWMQTTClient {
    /**
     * DeBug用信息
     **/
    private String Tag = "BWMQTTClient";
    private boolean isDebug = false;

    /**
     * MQTT配置参数
     **/
    public static String host = "47.99.76.88";
    public static String port = "1883";
    public static String userID = "";
    public static String passWord = "";
    public static String clientID = "InnoCamp/01-08/AICar";

    /**
     * MQTT状态信息
     **/
    private static boolean isConnect = false;

    /**
     * 系统变量
     **/
    private Handler fatherHandle;

    /**
     * MQTT支持类
     **/
    private MqttAsyncClient mqttClient = null;

    /**
     * 构造函数，默认的Mqtt信息内容，直接在Log中输出相关信息
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public BWMQTTClient() {
        fatherHandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MQTTSetting.MQTT_STATE_CONNECTED) {
                    Log.d(Tag, "连接成功");
                } else if (msg.what == MQTTSetting.MQTT_STATE_LOST) {
                    Log.d(Tag, "连接丢失，进行重连");
                } else if (msg.what == MQTTSetting.MQTT_STATE_FAIL) {
                    Log.d(Tag, "连接失败");
                } else if (msg.what == MQTTSetting.MQTT_STATE_RECEIVE) {
                    Log.d(Tag, (String) msg.obj);
                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 构造函数，如果需要在其他类中调用Mqtt信息的话，需要传入一个Handle
     *
     * @param uiHandle
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public BWMQTTClient(Handler uiHandle) {
        fatherHandle = uiHandle;
    }

    /**
     * 判断现在Mqtt信息是否连上
     *
     * @return 连上了或者没有
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public static boolean isConnect() {
        return isConnect;
    }

    /**
     * 进行Mqtt连接
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public void connectMqtt() {
        try {
            mqttClient = new MqttAsyncClient("tcp://" + this.host, "ClientID/" + this.clientID, new MemoryPersistence());
            mqttClient.connect(getOptions(), null, mqttActionListener);
            mqttClient.setCallback(callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开Mqtt连接重新连接
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public void reStartMqtt() {
        disConnectMqtt();
        connectMqtt();
    }

    /**
     * 断开Mqtt连接
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public void disConnectMqtt() {
        try {
            mqttClient.disconnect();
            mqttClient = null;
            BWMQTTClient.isConnect = false;
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向Mqtt服务器发送数据
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public void pubMsg(String Topic, String Msg) {
        if (!isConnect) {
            Log.d(Tag, "Mqtt连接未打开");
            return;
        }
        try {
            /** Topic,Msg,Qos,Retained**/
            mqttClient.publish(Topic, Msg.getBytes(), 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向Mqtt服务器发送数据
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public void pubMsg(String Topic, byte[] Msg) {
        if (!isConnect) {
            Log.d(Tag, "Mqtt连接未打开");
            return;
        }
        try {
            /** Topic,Msg,Qos,Retained**/
            mqttClient.publish(Topic, Msg, 1, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向Mqtt服务器订阅某一个Topic
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    public void subTopic(String Topic) {
        if (!isConnect) {
            Log.d(Tag, "Mqtt连接未打开");
            return;
        }
        try {
            mqttClient.subscribe(Topic, 2);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置Mqtt的连接信息
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    private MqttConnectOptions getOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);//重连不保持状态
        if (this.userID != null && this.userID.length() > 0 && this.passWord != null && this.passWord.length() > 0) {
            options.setUserName(this.userID);//设置服务器账号密码
            options.setPassword(this.passWord.toCharArray());
        }
        options.setConnectionTimeout(10);//设置连接超时时间
        options.setKeepAliveInterval(30);//设置保持活动时间，超过时间没有消息收发将会触发ping消息确认
        return options;
    }

    /**
     * 自带的监听类，判断Mqtt活动变化
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    private IMqttActionListener mqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            //连接成功处理
            Message msg = new Message();
            msg.what = MQTTSetting.MQTT_STATE_CONNECTED;
            BWMQTTClient.isConnect = true;
            fatherHandle.sendMessage(msg);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            exception.printStackTrace();
            //连接失败处理
            Message msg = new Message();
            msg.what = MQTTSetting.MQTT_STATE_FAIL;
            BWMQTTClient.isConnect = false;
            fatherHandle.sendMessage(msg);
            new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(300);
                        connectMqtt();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };

    /**
     * 自带的监听回传类，向UiHandle发送Message
     *
     * @State Finish
     * @Version 1.0
     * @User MichaelJiang
     */
    private MqttCallback callback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            //连接断开
            Message msg = new Message();
            msg.what = MQTTSetting.MQTT_STATE_LOST;
            BWMQTTClient.isConnect = false;
            fatherHandle.sendMessage(msg);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //消息到达
            Message msg = new Message();
            msg.what = MQTTSetting.MQTT_STATE_RECEIVE;
            msg.obj = new String(message.getPayload()) + "\n";
            BWMQTTClient.isConnect = true;
            fatherHandle.sendMessage(msg);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //消息发送完成
        }
    };

}
