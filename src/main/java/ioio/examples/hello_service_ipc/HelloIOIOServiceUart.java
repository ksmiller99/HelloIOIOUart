package ioio.examples.hello_service_ipc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

import static ioio.examples.hello_service_ipc.MyApp.*;

public class HelloIOIOServiceUart extends IOIOService {
    //MyApp myApp = MyApp.getInstance();

    final Messenger messenger = new Messenger(new IncomingHandler());

    public static final int IOIO_STATUS_REQUEST = 0;    //request IOIO Status
    public static final int IOIO_STATUS_REPLY   = 1;    //request IOIO Status
    public static final int ERROR_REPLY         = 2;    //TODO determine error details
    public static final int LED_BLINK_REQUEST   = 3;    //request blinking status LED
    public static final int LED_OFF_REQUEST     = 4;    //request turning off status LED
    public static final int LED_BLINK_REPLY     = 5;    //LED was set to BLINK
    public static final int LED_OFF_REPLY       = 6;    //LED was turned off
    public static final int LED_STATUS_REQUEST  = 7;    //Status of LED request
    public static final int LED_STATUS_REPLY    = 8;    //arg1 == 1 if true

    private static final int UART_1_TX_PIN = 10;
    private static final int UART_1_RX_PIN = 11;
    private static final int UART_2_TX_PIN = 12;
    private static final int UART_2_RX_PIN = 13;

    public static boolean led_blink = false;    //true if LED is in BLINK mode
    private static boolean led_status = false;   //true if LED is on at this moment
    private static boolean ledTimerFlag = false;
    public static boolean ioio_state = false;

    //declare Intents for broadcast messages
    Intent setupIntent;             //IOIO has connected
    Intent disconnectedIntent;      //IOIO has disconnected
    Intent inQueue1Intent;          //external data from inQueue1
    Intent inQueue2Intent;          //external data from inQueue2
    Intent exitEverythingIntent;    //close app

    private static Uart uart1;
    private static Uart uart2;

    private static InputStream in1;
    private static OutputStream out1;
    private static InputStream in2;
    private static OutputStream out2;

    @Override
    public void onCreate() {
        super.onCreate();
        //MyApp myApp = MyApp.getInstance();

        //create Intents for broadcast messages
        Log.d("KSM","Service onCreate");

        setupIntent = new Intent("IOIO_CONNECTED");
        disconnectedIntent = new Intent("IOIO_DISCONNECTED");
        inQueue1Intent = new Intent("INPUT_QUEUE_1");
        inQueue2Intent = new Intent("INPUT_QUEUE_2");
        exitEverythingIntent = new Intent("EXIT_EVERYTHING");

    }

    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            Messenger rmsgr = msg.replyTo;
            Message rmsg;

            switch (msg.what) {
                case LED_BLINK_REQUEST:
                    Log.d("KSM", "LED_BLINK_REQUEST message handled");

                    if (!ioio_state){
                        rmsg = Message.obtain(null, ERROR_REPLY, msg.what, 0);
                        Log.d("KSM", "Sending reply message ERROR_REPLY ");
                    }else {
                        rmsg = Message.obtain(null, LED_BLINK_REPLY, 0, 0);
                        Log.d("KSM", "Sending reply message LED_BLINK_REPLY ");
                        led_blink = true;
                    }

                    try {
                        rmsgr.send(rmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

                case LED_OFF_REQUEST:
                    Log.d("KSM","LED_OFF message handled");

                    if (!ioio_state){
                        rmsg = Message.obtain(null, ERROR_REPLY, msg.what, 0);
                        Log.d("KSM", "Sending reply message ERROR_REPLY ");
                    }else {
                        rmsg = Message.obtain(null, LED_OFF_REPLY, 0, 0);
                        Log.d("KSM", "Sending reply message LED_OFF_REQUEST");
                        led_blink = false;
                    }

                    try {
                        rmsgr.send(rmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

                case LED_STATUS_REQUEST:
                    Log.d("KSM", "LED_STATUS_REQUEST message handled");

                    rmsg = Message.obtain(null, LED_STATUS_REPLY, led_blink ?1:0, 0);
                    Log.d("KSM", "Sending LED_STATUS_REPLY");
                    try {
                        rmsgr.send(rmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

                case IOIO_STATUS_REQUEST:
                    Log.d("KSM", "IOIO_STATUS_REQUEST message handled");

                    rmsg = Message.obtain(null, IOIO_STATUS_REPLY, ioio_state?1:0, 0);
                    Log.d("KSM", "Sending reply message IOIO_STATUS_REPLY");
                    try {
                        rmsgr.send(rmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            private DigitalOutput led_;
            private Timer ledTimer_;

            //temp storage from UARTs
            private byte inBytes1[] = new byte[256];
            private byte inBytes2[] = new byte[256];

            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {
                ledTimer_ = new Timer();
                ledTimer_.schedule(new TimerTask() {
                    @Override
                    public void run(){
                        setLedTimerFlag();
                    }
                }, 0, 500);

                uart1 = ioio_.openUart(UART_1_RX_PIN,UART_1_TX_PIN,9600, Uart.Parity.NONE, Uart.StopBits.ONE);
                in1 = uart1.getInputStream();
                out1 = uart1.getOutputStream();

                uart2 = ioio_.openUart(UART_2_RX_PIN,UART_2_TX_PIN,9600, Uart.Parity.NONE, Uart.StopBits.ONE);
                in2 = uart2.getInputStream();
                out2 = uart2.getOutputStream();

                ioio_state = true;
                led_ = ioio_.openDigitalOutput(IOIO.LED_PIN);
                sendBroadcast(setupIntent);
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                if(ledTimerFlag) {
                    led_.write(!led_status);
                    ledTimerFlag = false;
                }

                try {
                    if (in1.available() > 0) {
                        int len = in1.read(inBytes1);
                        inQueue1.add(Arrays.copyOfRange(inBytes1,0,len));
                        sendBroadcast(inQueue1Intent);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }

                if(!outQueue1.isEmpty()){
                    while(true){
                        byte ba[] = outQueue1.poll();
                        if (ba == null)
                            break;

                        try {
                            out1.write(ba);
                        }catch (IOException e){
                            Log.d("loop","out1.write IO exception:\n"+e.getMessage());
                            e.printStackTrace();
                        }
                        catch (Exception e){
                            Log.d("loop","Write outQueue1: "+e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    if (in2.available() > 0) {
                        int len = in2.read(inBytes2);
                        inQueue2.add(Arrays.copyOfRange(inBytes2,0,len));
                        sendBroadcast(inQueue2Intent);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }

                if(!outQueue2.isEmpty()){
                    while(true){
                        byte ba[] = outQueue2.poll();
                        if (ba == null)
                            break;

                        try {
                            out2.write(ba);
                        }catch (IOException e){
                            Log.d("loop","out2.write IO exception:\n"+e.getMessage());
                            e.printStackTrace();
                        }
                        catch (Exception e){
                            Log.d("loop","Write outQueue2: "+e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

            }

            @Override
            public void disconnected() {
                Log.d("KSM","IOIO Disconnect");
                ioio_state = false;
                ledTimer_.cancel();
                ledTimer_=null;

                uart1 = null;
                uart2 = null;
                sendBroadcast(disconnectedIntent);
            }

        };
    }

    private void setLedTimerFlag(){
        if(led_blink){
            led_status = !led_status;
        }else{
            led_status = false;
        }
        ledTimerFlag = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int result = super.onStartCommand(intent, flags, startId);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent != null && intent.getAction() != null
                && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            nm.cancel(0);
            stopSelf();
        } else {
            // Service starting. Create a notification.
            Notification notification = new Notification(
                    R.drawable.ic_launcher, "IOIO IPC service running",
                    System.currentTimeMillis());
            notification
                    .setLatestEventInfo(this, "IOIO IPC Service", "Running",
                            PendingIntent.getService(this, 0, new Intent(
                                    "stop", null, this, this.getClass()), 0));
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            nm.notify(0, notification);
        }
        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("KSM", "Service.onBind");
        return messenger.getBinder();
    }
}
