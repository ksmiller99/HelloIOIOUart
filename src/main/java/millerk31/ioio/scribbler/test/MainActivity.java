package millerk31.ioio.scribbler.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import millerk31.myro.Scribbler;

//import static millerk31.ioio.scribbler.test.MyApp.s2InQueue;
//import static millerk31.ioio.scribbler.test.MyApp.s2OutQueue;
import static millerk31.ioio.scribbler.test.MyApp.tvRxDataSave1;

public class MainActivity extends Activity {

    Scribbler scribbler = Scribbler.getInstance();

    private ToggleButton toggleButton_;
    private TextView tvRxData;
    private Button btnGetInfo;
    private Button btnMotorsOn;
    private Button btnMotorsOff;
    private Button btn05;
    private Button btn00;
    private Button btn0F;
    private RadioGroup rgRepeat;

    boolean isIoioBound = false;
    Messenger messenger = null;

    //create IntentFilters for receiving broadcast messages
    IntentFilter connectFilter = new IntentFilter("IOIO_CONNECTED");
    IntentFilter disconnectFilter = new IntentFilter("IOIO_DISCONNECTED");
    IntentFilter scribblerConnectFilter = new IntentFilter(IOIOScribblerService.SCRIBBLER_CONNECTED_INTENT_MSG);
    //IntentFilter inQueue1Filter = new IntentFilter("INPUT_QUEUE_1");

    private boolean repeatFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //start the IOIO Service
        startService(new Intent(this, IOIOScribblerService.class));

        toggleButton_ = (ToggleButton) findViewById(R.id.ToggleButton);
        tvRxData = (TextView) findViewById(R.id.tvRxData1);
        btnGetInfo = (Button) findViewById(R.id.btnGetInfo);
        btnMotorsOn = (Button) findViewById(R.id.btnMotorsOn);
        btnMotorsOff = (Button) findViewById(R.id.btnMotorsOff);
        btn05 = (Button) findViewById(R.id.btn05);
        btn00 = (Button) findViewById(R.id.btn00);
        btn0F = (Button) findViewById(R.id.btn0F);
        rgRepeat = (RadioGroup)findViewById(R.id.rgRepeat);
        rgRepeat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rbRepeatOn){
                    repeatFlag = true;
                } else
                if(checkedId == R.id.rbRepeatOff){
                    repeatFlag = false;
                }
            }
        });

        //assume IOIO is disconnected at start
        enableUi(false);

        //bind to  the IOIO service
        Intent intent = new Intent(this, IOIOScribblerService.class);
        bindService(intent, serviceIoioConnection, Context.BIND_AUTO_CREATE);
    }

    //Outbound IOIO messages go through ServiceConnection
    private ServiceConnection serviceIoioConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("KSM", "Main.onServiceConnected");
            isIoioBound = true;

            // Create the Messenger object
            messenger = new Messenger(service);

            //update UI elements to match IOIO state
            Message msg = Message.obtain(null, IOIOScribblerService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            msg = Message.obtain(null, IOIOScribblerService.LED_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("KSM", "activity_main.onServiceDisconnect");

            // unbind or process might have crashes
            messenger = null;
            isIoioBound = false;
        }
    };

    @Override
    protected void onResume() {
        //setup broadcast receivers
        registerReceiver(myReceiver, connectFilter);
        registerReceiver(myReceiver, disconnectFilter);
        registerReceiver(myReceiver, scribblerConnectFilter);
        //registerReceiver(myReceiver, inQueue1Filter);

        //update UI elements to match IOIO state
        if (isIoioBound) {
            Message msg = Message.obtain(null, IOIOScribblerService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            msg = Message.obtain(null, IOIOScribblerService.LED_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        //restore data to view that was erased when view paused
        tvRxData.setText(tvRxDataSave1);

        Log.d("KSM", "Main.onResume completed");
        super.onResume();
    }

    @Override
    //make sure service is disconnected from activity
    protected void onDestroy() {
        unbindService(serviceIoioConnection);
        messenger = null;
        isIoioBound = false;

        super.onDestroy();
    }

    @Override
    //disable broadcast receiver when activity is not active
    protected void onPause() {
        try {
            unregisterReceiver(myReceiver);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Receiver not registered")) {
                // Ignore this exception. This is exactly what is desired
                Log.w("KSM", "Tried to unregister the receiver when it's not registered");
            } else {
                // unexpected, re-throw
                throw e;
            }
        }

        tvRxDataSave1 = tvRxData.getText().toString();

        super.onPause();
    }

    //create handler for incoming messages (not broadcasts)
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case IOIOScribblerService.LED_BLINK_REPLY:
                    Log.d("KSM", "LED_BLINK_REPLY message handled");
                    toggleButton_.setChecked(true);
                    break;

                case IOIOScribblerService.LED_OFF_REPLY:
                    Log.d("KSM", "LED_OFF_REPLY message handled");
                    toggleButton_.setChecked(false);
                    break;

                case IOIOScribblerService.LED_STATUS_REPLY:
                    toggleButton_.setChecked(msg.arg1 == 1);
                    Log.d("KSM", "LED_STATUS_REPLY: " + msg.arg1 + " message handled");
                    break;

                case IOIOScribblerService.IOIO_STATUS_REPLY:
                    enableUi(msg.arg1 == 1);
                    Log.d("KSM", "IOIO_STATUS_REPLY: " + msg.arg1 + " message handled");
                    break;

                case IOIOScribblerService.ERROR_REPLY:
                    Log.d("KSM", "ERROR_REPLY to message type: " + msg.arg1 + " message handled");
                    break;

                default:
                    Log.d("KSM", "UNKNOWN MESSAGE TYPE: " + msg.what);
                    super.handleMessage(msg);
            }
        }
    }

    public void tglOnClick(View v) {
        Log.d("KSM", "MAIN Toggle Button pressed.");
        ToggleButton tgl = (ToggleButton) v;
        int msgType;

        //set message type based on toggle status after clicking
        if (tgl.isChecked())
            msgType = IOIOScribblerService.LED_BLINK_REQUEST;
        else
            msgType = IOIOScribblerService.LED_OFF_REQUEST;

        //revert button state so that IOIO can control it via the reply message in case
        //there is some unknown reason in the service that would prevent the state change
        tgl.setChecked(!tgl.isChecked());

        Message msg = Message.obtain(null, msgType, 0, 0);
        msg.replyTo = new Messenger(new IncomingHandler());

        Log.d("KSM", "Toggle Message " + msgType + " sending...");

        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //go to Second activity
    public void btnSecondOnClick(View v) {
        Intent intent = new Intent(this, SecondActivity.class);
        startActivity(intent);
    }

    //to receive broadcasts from IOIO
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("KSM", "Broadcast intent received");
            if (intent.getAction().equals("IOIO_DISCONNECTED")) {
                enableUi(false);
                Log.d("KSM", "Broadcast DISCONNECTED intent received");

            } else if (intent.getAction().equals("IOIO_CONNECTED")) {
                enableUi(true);
                Log.d("KSM", "Broadcast CONNECTED intent received");

            } else if(intent.getAction().equals(IOIOScribblerService.SCRIBBLER_CONNECTED_INTENT_MSG)){
                Toast.makeText(getApplicationContext(),"S2 Connected",Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void enableUi(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggleButton_.setEnabled(enable);
            }
        });
    }

    public void btnGetInfoOnClick(View v){
        String info;
        if (scribbler.scribblerConnected()) {
            info = scribbler.getInfo();
        }else{
            info = "Error: Scribbler not connected";
        }

        tvRxData.setText(tvRxData.getText()+"\n"+info);

        if(repeatFlag){
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run(){
                    btnGetInfoOnClick(btnGetInfo);
                }
            },500);
        }
    }

    public void btnMotorsOnOnClick(View v){
        if(scribbler.scribblerConnected()){
            scribbler.motors(0.3,0.3);
        }else{
            tvRxData.setText(tvRxData.getText()+"\nError: Scribbler not connected");
        }
    }
    public void btnMotorsOffOnClick(View v){
        if(scribbler.scribblerConnected()){
            scribbler.motors(0,0);
        }else{
            tvRxData.setText(tvRxData.getText()+"\nError: Scribbler not connected");
        }
    }

    public void btn05OnClick(View v){

    }

    public void btnCloseOnClick(View v){
        //IOIOScribblerService.s2Handler.closeScribbler();
    }

    public void btn0FOnClick(View v){
     }

    public void btnClearOnClick(View v){
        tvRxData.setText("");
    }

    final protected static char[] decimalArray = "0123456789".toCharArray();
    public static String bytesToDecimal(byte[] bytes) {
        char[] decimalChars = new char[bytes.length * 4];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            decimalChars[j * 4] = decimalArray[v / 100];
            decimalChars[j * 4 + 1] = decimalArray[(v / 10) % 10];
            decimalChars[j * 4 + 2] = decimalArray[v % 10];
            decimalChars[j * 4 + 3] = ' ';
        }
        return new String(decimalChars);
    }

    public void btnSongOnClick(View v){
        if (scribbler.scribblerConnected()) {
            scribbler.playSong("A 1; F# 1; E 1; F# 1; A 1;", .05);
        }else{
            tvRxData.setText(tvRxData.getText()+"\nError: Scribbler not connected");
        }
    }
 }


