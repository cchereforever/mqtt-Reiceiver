package com.example.mqttreceiver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mqttreceiver.adapter.LogListAdapter;
import com.example.mqttreceiver.mqttutils.AiotMqttOption;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;


import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "MqttReceiverdemo";
    private MqttThread mqttThread;
    final private String PRODUCTKEY = "k0rfe5Cztgk";//"a11xsrWmW14";  csy:"k0pv916lPRF"   cjy:"k0rfe5Cztgk"
    final private String DEVICENAME = "device2";//"paho_android";   csy:"device1"
    final private String DEVICESECRET = "2c4d3a0447fd0e17d086e3ed324eb7fa";  //csy_device1:"da12cdf222fe5ae23879d7946ea1deeb"  cjy_device2:"2c4d3a0447fd0e17d086e3ed324eb7fa"
    private String clientId;
    private String userName;
    private String passWord;
    MqttAndroidClient mqttAndroidClient;
    MqttConnectOptions mqttConnectOptions;
    final String host = "tcp://" + PRODUCTKEY + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";
    private ScheduledExecutorService scheduler;
    private Handler handler;
    /*用于属性上报*/
    final private String PUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/update";
    /* 自动Topic, 用于接受消息 */
    final private String SUB_TOPIC = "/" + PRODUCTKEY + "/" + DEVICENAME + "/user/get";
    private static final int UPLOAD_SUCCESS = 1;

    private EditText subTopic;
    private Button btSubscribe;
    private TextView tvSubTopic;
    private TextView tvReceivedMessages;
    private TextView tvMessageDetails;
    private LogListAdapter logListAdapter;
    private RecyclerView recy;

    static int count = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the views
        subTopic = (EditText) findViewById(R.id.subTopic);
        btSubscribe = (Button) findViewById(R.id.subscribe);
        tvSubTopic = (TextView) findViewById(R.id.tvSubTopic);
        //tvReceivedMessages = (TextView) findViewById(R.id.tvReceivedMessages);
        //tvMessageDetails = (TextView) findViewById(R.id.tvMessageDetails);

        recy = findViewById(R.id.recyclerView);


        logListAdapter = new LogListAdapter(null);
        recy.setLayoutManager(new LinearLayoutManager(this));
        recy.setAdapter(logListAdapter);
        recy.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        btSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Code to subscribe to a topic
                mqttThread = new MqttThread();
                mqttThread.start();
            }
        });

        // 创建并启动 MQTT 连接以及重连线程

        // 初始化全局 Handler
        initHandler();

    }

    private void subscribeToTopic(String topic) {
        // Implementation of subscription logic
        // This is where you might handle MQTT subscription, for example

        // Update the UI to reflect the subscription
        tvSubTopic.setText("Subscribed to: " + topic);
    }
    private void initHandler() {
        handler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPLOAD_SUCCESS:
                        // 处理上传成功的消息
                        //String message = (String) msg.obj;
                        Log.d(TAG, "mainactivity+onUploadSuccess\n");
                        // 在这里执行你的逻辑
                        break;
                    /*case 1: //开机校验更新回传
                        break;*/
                    /*case 2:  //1.收到警报，上传服务端
                        publishMessage(PUB_TOPIC, "FELL ATTENTION!");
                        break;*/
                    case 3:  //MQTT 收到消息回传   UTF8Buffer msg=new UTF8Buffer(object.toString());
                        System.out.println(msg.obj.toString());   // 显示MQTT数据
                        //parseJsonobj(msg.obj.toString());
                        break;
                    case 30:  //连接失败
                        //m_mqtt.setText("连接失败");
                        //Toast.makeText(MainActivity.this,"连接失败" ,Toast.LENGTH_SHORT).show();
                        System.out.println("MQTT服务器连接失败");
                        break;
                    case 31:   //连接成功
                        //m_mqtt.setText("连接成功");
                        //Toast.makeText(MainActivity.this,"连接成功" ,Toast.LENGTH_SHORT).show();
                        System.out.println("MQTT服务器连接成功");
                        try {
                            //publishMessage(WILL_TOPIC, "ONLINE");
                            mqttAndroidClient.subscribe(SUB_TOPIC, 1);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }

            }
        };
    }
    /**
     * 向默认的主题/user/update发布消息
     * @param payload 消息载荷
     */
    public void publishMessage(String Topic,String payload) {

        try {
            if (mqttAndroidClient.isConnected() == false) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(Topic, message,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "post succeed!");
                    Log.d(TAG,"1111");                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "publish failed!");
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private class MqttThread extends Thread {
        @Override
        public void run() {
            // 在这里执行 MQTT 的连接、订阅等操作
            Mqtt_init();
            startReconnect();
        }
        public void Mqtt_init(){
            /* 获取Mqtt建连信息clientId, username, password */
            AiotMqttOption aiotMqttOption = new AiotMqttOption().getMqttOption(PRODUCTKEY, DEVICENAME, DEVICESECRET);
            if (aiotMqttOption == null) {
                Log.e(TAG, "device info error");
            } else {
                clientId = aiotMqttOption.getClientId();
                userName = aiotMqttOption.getUsername();
                passWord = aiotMqttOption.getPassword();
            }

            /* 创建MqttConnectOptions对象并配置username和password */
            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setUserName(userName);
            mqttConnectOptions.setPassword(passWord.toCharArray());


            /* 创建MqttAndroidClient对象, 并设置回调接口 */
            mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), host, clientId);
            mqttAndroidClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.i(TAG, "connection lost");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    //subscribe主题后，收到消息执行到这里
                    /*logListAdapter.addData(SUB_TOPIC + count++);
                    if (logListAdapter.getData() != null && logListAdapter.getData().size() > 0) {
                        recy.smoothScrollToPosition(logListAdapter.getData().size());
                    }*/
                    /*try {
                        JSONObject jsonObject = new JSONObject(message.toString());// 解析JSON数据
                        Log.i(TAG, "消息到达,message: " + message);
                        JSONObject paramsObject = jsonObject.getJSONObject("params");// 获取params字段中的JSONObject

                        int Humidity = paramsObject.getInt("Humidity");
                        int LightLux = paramsObject.getInt("LightLux");
                        int temperature = paramsObject.getInt("temperature");
                        humidityTextView.setText(String.valueOf(Humidity));
                        lightluxTextView.setText(String.valueOf(LightLux));
                        temperatureTextView.setText(String.valueOf(temperature));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }*/
                    /*如何可以将我的testactivity中数据上传成功的消息传送过来*/
                    String receivedMessage = topic + " Receive " + new String(message.getPayload());
                    Log.i(TAG+1, receivedMessage);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            logListAdapter.addData(receivedMessage);
                            Log.d(TAG + 1, "Data added to adapter: " + receivedMessage);
                            if (logListAdapter.getData() != null && logListAdapter.getData().size() > 0) {
                                recy.smoothScrollToPosition(logListAdapter.getData().size() - 1); // 滚动到最新的项
                                Log.d(TAG + 1, "RecyclerView scrolled to position: " + (logListAdapter.getData().size() - 1));
                            }
                        }
                    });

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.i(TAG, "msg delivered");
                }
            });

        }
        private void startReconnect() {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (!mqttAndroidClient.isConnected()) {
                        Mqtt_connect();
                    }
                }
            }, 0*1000, 10 * 1000, TimeUnit.MILLISECONDS);
        }
        private void Mqtt_connect() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(!(mqttAndroidClient.isConnected()) )  //如果还未连接
                        {
                            /* Mqtt建连 */
                            mqttAndroidClient.connect(mqttConnectOptions,null, new IMqttActionListener() {
                                @Override
                                public void onSuccess(IMqttToken asyncActionToken) {
                                    Log.i(TAG, "connect succeed");
                                    subscribeTopic(SUB_TOPIC);
                                    Message msg = new Message();
                                    msg.what = 31;
                                    handler.sendMessage(msg);
                                }

                                @Override
                                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                    Log.i(TAG, "connect failed");
                                    Log.e(TAG,"connnect failed" + exception);
                                }
                            });


                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Message msg = new Message();
                        msg.what = 30;
                        handler.sendMessage(msg);
                    }
                }
            }).start();
        }
        public void subscribeTopic(String topic) {
            try {
                mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i(TAG+1, topic + "subscribed succeed");
                        tvSubTopic.setText(topic);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.i(TAG, "subscribed failed");
                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

}
