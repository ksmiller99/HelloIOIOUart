package ioio.examples.hello_service_ipc;

import android.app.Application;
import android.util.Log;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Created by Kevin on 10/6/2016.
 */

public class MyApp extends Application {

    //for UARTs
    //takes data from UART1 RX, data taken
    public static Queue<byte[]> outQueue1  = new ConcurrentLinkedQueue<byte[]>();
    public static Queue<byte[]> inQueue1   = new ConcurrentLinkedQueue<byte[]>();
    public static Queue<byte[]> outQueue2  = new ConcurrentLinkedQueue<byte[]>();
    public static Queue<byte[]> inQueue2   = new ConcurrentLinkedQueue<byte[]>();

    //save recieve textview data when activity pauses
    //and restore when it resumes
    public static String tvRxDataSave1 = "";
    public static String tvRxDataSave2 = "";

}
