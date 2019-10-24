package com.example.mediacodec;



import android.util.Log;

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
      //  setPriority(MAX_PRIORITY);
        start();




    }







    public void run() {



        try {

            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);// отправляем показания датчиков
            System.out.println(MainActivity.Sensors);



            pw.println(MainActivity.Sensors);//Показания датчиков
            pw.flush();
            pw.close();


            socket.close();

            Log.i(MainActivity.LOG_TAG, MainActivity.Sensors);

        } catch (Exception e) {
            System.out.println(e);

        }


    }
}



