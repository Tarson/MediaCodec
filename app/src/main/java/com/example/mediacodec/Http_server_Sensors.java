package com.example.mediacodec;



import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import static com.example.mediacodec.MainActivity.LOG_TAG;

/**
 * Created by m on 25.03.2019.
 */

public class Http_server_Sensors extends Thread {
    Socket socket=null;
    PrintWriter pw;


    Http_server_Sensors(Socket s) {








        socket = s;
        setPriority(MAX_PRIORITY);
        start();




    }







    public void run() {



            try {


                pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);// отправляем показания датчиков
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));// получаеи информацию о состоянии фары



                pw.println(MainActivity.Sensors);//Показания датчиков
                pw.flush();


                String FaraInfo = br.readLine();
                boolean flashlight;
                if (FaraInfo.equals("FARAON"))
                {
                    flashlight=true;
                    MainActivity.myCameras[MainActivity.CAMERA1].Toggle_light(flashlight);;

                    Log.i(LOG_TAG, FaraInfo);


                }
                if (FaraInfo.equals("FARAOFF")) {
                    flashlight = false;
                    MainActivity.myCameras[MainActivity.CAMERA1].Toggle_light(flashlight);
                    Log.i(LOG_TAG, FaraInfo);


                }

                br.close();
                pw.close();



                socket.close();

                Log.i(MainActivity.LOG_TAG, MainActivity.Sensors);

            } catch (Exception e) {
                Log.i(LOG_TAG," "+ e );

            }



    }
}



