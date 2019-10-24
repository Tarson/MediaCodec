package com.example.mediacodec;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;


import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;

import android.hardware.camera2.CaptureRequest;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity  implements SensorEventListener {


    public static final String LOG_TAG = "myLogs";
    public static Surface surface = null;

    CameraService[] myCameras = null;

    private CameraManager mCameraManager = null;
    private final int CAMERA1 = 0;


    private Button mButtonOpenCamera1 = null;
    private Button mButtonStreamVideo = null;
    private Button mButtonToMakeShot = null;
    public static TextureView mImageView = null;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler = null;

    private MediaCodec mCodec = null; // кодер
    Surface mEncoderSurface; // Surface как вход данных для кодера
    BufferedOutputStream outputStream;
    ByteBuffer outPutByteBuffer;
    byte[] outDate = null;
    DatagramSocket udpSocket;
    String ip_address = "192.168.1.84";
    InetAddress address;
    int port = 40001;
    static boolean isStreaming = false;
    boolean bufferReady = false;


    public static ServerSocket ss2;

    public SensorManager mSensorManager;
    public Sensor mAxeleration, mLight,mRotation,mHumidity,mPressure,mTemperature;



    public int ax;
    public int az;
    public double light;


    public int x;
    public int y;
    public int z;

    public double hum;
    public double press;
    public double tempr;

    public static String Sensors;




















    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров



        mAxeleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // Получаем датчик положения
        mSensorManager.registerListener(this, mAxeleration, SensorManager.SENSOR_DELAY_NORMAL);


        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);


        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_NORMAL);

        mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);

        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);


        mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);









        setContentView(R.layout.activity_main);


        Log.d(LOG_TAG, "Запрашиваем разрешение");
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        ) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }


        mButtonOpenCamera1 = findViewById(R.id.button1);
        mButtonStreamVideo = findViewById(R.id.button2);
        mButtonToMakeShot = findViewById(R.id.button3);
        mImageView = findViewById(R.id.textureView);

        mButtonOpenCamera1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                try {
                    udpSocket = new DatagramSocket();

                    Log.i(LOG_TAG, "  создали udp сокет");

                } catch (
                        SocketException e) {
                    Log.i(LOG_TAG, " не создали udp сокет");
                }

                try {
                    address = InetAddress.getByName(ip_address);
                    Log.i(LOG_TAG, "  есть адрес");
                } catch (Exception e) {


                }

                if(mCodec==null){
                    setUpMediaCodec();
                }
                if (myCameras[CAMERA1] != null) {
                    if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openCamera();
                }


            }
        });

        mButtonStreamVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                {
                    isStreaming = true;

                    new ServerCreation().execute();// запускаем сервер для отправки файлов

                    Toast.makeText(MainActivity.this, " стримим видео", Toast.LENGTH_SHORT).show();
                }


            }
        });


        mButtonToMakeShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isStreaming = false;
                if (myCameras[CAMERA1] != null) {
                    myCameras[CAMERA1].stopStreamingVideo();
                }


            }
        });








        setUpMediaCodec();


        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Получение списка камер с устройства


            myCameras = new CameraService[mCameraManager.getCameraIdList().length];


            for (String cameraID : mCameraManager.getCameraIdList()) {
                Log.i(LOG_TAG, "cameraID: " + cameraID);
                int id = Integer.parseInt(cameraID);


                // создаем обработчик для камеры
                myCameras[id] = new CameraService(mCameraManager, cameraID);


            }
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }


    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { //Изменение точности показаний датчика
    }




    @Override
    public void onSensorChanged(SensorEvent event) { //Изменение показаний датчиков


        switch (event.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:

                ax = (int)(event.values[0] * 9); //крен +влево - вправо

                az = (int)(event.values[2] * 9);// тангаж + вперед  - назад

                //      System.out.println("крен = "+  ax  + "  тангаж =  " +  az);
                break;

            case Sensor.TYPE_LIGHT:

                light = event.values[0];
                //  System.out.println("свет = "+  light);
                break;

            case Sensor.TYPE_ORIENTATION:

                x = (int)event.values[0];
                y = (int)event.values[1]+90; // тангаж + вперед  - назад
                z = (int)event.values[2];  //крен +влево - вправо
                //       System.out.println("x = "+  x  + "  y =  " +  y+ "  z= "+ z);
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:


                hum = event.values[2];
                int k = (int)(hum*100);
                hum = - (double)k;//ускорение вперед положительное , назад отрицательное в см/с2

                //        System.out.println(hum);
                break;

            case  Sensor.TYPE_PRESSURE:
                press = event.values[0]*760/10.1325;

                int i = (int) press;
                press = (double)i/100;

                //        System.out.println(press);
                break;


            case  Sensor.TYPE_AMBIENT_TEMPERATURE:
                tempr = event.values[0];
                System.out.println(tempr);
                break;

        }


        Sensors = " tangaz_1 "+ az+ " kren_1 " + ax + " tangaz_2 "+  y + " kren_2 " + z + " forvard_accel "+ hum +
                " light " + light+ "  ";


        // System.out.println(Sensors);

    }



    public class CameraService {


        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mSession;
        private CaptureRequest.Builder mPreviewBuilder;


        public CameraService(CameraManager cameraManager, String cameraID) {

            mCameraManager = cameraManager;
            mCameraID = cameraID;

        }


        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:" + mCameraDevice.getId());

                startCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraDevice.close();

                Log.i(LOG_TAG, "disconnect camera  with id:" + mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:" + camera.getId() + " error:" + error);
            }
        };

        private void startCameraPreviewSession() {
            SurfaceTexture texture = mImageView.getSurfaceTexture();
            texture.setDefaultBufferSize(320, 240);
            surface = new Surface(texture);


            try {

                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.addTarget(surface);
                mPreviewBuilder.addTarget(mEncoderSurface);


                mCameraDevice.createCaptureSession(Arrays.asList(surface, mEncoderSurface),

                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mSession = session;

                                try {
                                    mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                            }
                        }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }


        public void stopStreamingVideo() {


           if (mCameraDevice!=null & mCodec!= null) {
               try {
                   mSession.stopRepeating();
                   mSession.abortCaptures();
               } catch (CameraAccessException e) {
                   e.printStackTrace();
               }
               // Stop recording
               //
               mCodec.stop();
               mCodec.release();
               //mEncoderSurface.release();

               udpSocket.close();
           }
        }


        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }


        public void openCamera() {
            try {

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {


                    mCameraManager.openCamera(mCameraID, mCameraCallback, mBackgroundHandler);

                }


            } catch (CameraAccessException e) {
                Log.i(LOG_TAG, e.getMessage());

            }
        }

        public void closeCamera() {

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }


    }


    private void setUpMediaCodec() {

        /*
        File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "test3.h264");;

        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(mFile));
            Log.i("Encoder", "outputStream initialized");
        } catch (Exception e){
            e.printStackTrace();
        }
*/

        try {
            mCodec = MediaCodec.createEncoderByType("video/avc"); // H264 кодек

        } catch (Exception e) {
            Log.i(LOG_TAG, "нету кодека");

        }

        int width = 320; // ширина видео
        int height = 240; // высота видео
        int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface; // формат ввода цвета
        int videoBitrate = 500000; // битрейт видео в bps (бит в секунду)
        int videoFramePerSecond = 20; // FPS
        int iframeInterval = 3; // I-Frame интервал в секундах

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFramePerSecond);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);


        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); // конфигурируем кодек как кодер
        mEncoderSurface = mCodec.createInputSurface(); // получаем Surface кодера

        mCodec.setCallback(new EncoderCallback());
        mCodec.start(); // запускаем кодер
        Log.i(LOG_TAG, "запустили кодек");


    }

    private class EncoderCallback extends MediaCodec.Callback {

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {

        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {

            outPutByteBuffer = mCodec.getOutputBuffer(index);
            outDate = new byte[info.size];
            outPutByteBuffer.get(outDate);
            bufferReady=true;
            // Log.i(LOG_TAG, " outDate.length : " + outDate.length);

            try {
                DatagramPacket packet = new DatagramPacket(outDate, outDate.length, address, port);
                udpSocket.send(packet);
            } catch (IOException e) {
                Log.i(LOG_TAG, " не отправился UDP пакет");
            }


            /*
            try {
                Log.d(LOG_TAG, " outDate.length : " + outDate.length);
                outputStream.write(outDate, 0, outDate.length);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            */
            mCodec.releaseOutputBuffer(index, false);
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.i(LOG_TAG, "Error: " + e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            Log.i(LOG_TAG, "encoder output format changed: " + format);
        }
    }

















    @Override
    public void onPause() {
        if (myCameras[CAMERA1].isOpen()) {
            myCameras[CAMERA1].closeCamera();
        }

        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();


    }




}



class ServerCreation extends AsyncTask<Void, Void, Void> {

    @Override
    protected void onPreExecute() {
        //super.onPreExecute();





    }

    @Override
    protected Void doInBackground(Void... params) {



        try
        {




            MainActivity.ss2 = new ServerSocket(40002);// порт для передачи показаний датчиков


            Log.i(MainActivity.LOG_TAG, "Пробуем сервак");


        }catch (Exception e)

        {
            System.out.println(e);
            Log.i(MainActivity.LOG_TAG, " НЕ Запустили сервак  " + e);
            System.out.println();
        }




        new HTTP_Server_Calling2().start();




        return null;
    }

    @Override
    protected void onPostExecute(Void result) {


    }
}



class HTTP_Server_Calling2 extends Thread// раз в секунду дергаем данные с сенсоров и запускаем слушатель
{

    public void run() {

      while (MainActivity.isStreaming)

        {
            try {



                Thread.sleep(500);// скорость смены данных с сенсоров
               Log.i(MainActivity.LOG_TAG, MainActivity.Sensors);


              new Http_server_Sensors(MainActivity.ss2.accept());

            } catch (Exception e) {
                System.out.println(e);
            }


        }

    }





}