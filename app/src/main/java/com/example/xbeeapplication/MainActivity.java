package com.example.xbeeapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.print.PrintAttributes;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.felhr.utils.ProtocolBuffer;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "com.example.xbeeapplication.USB_PERMISSION";
    Button startButton, sendButton, stopButton,clearButton,panButton;
    TextView textView;
    EditText editText;
    ImageView image;
    ScrollView sc;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    LinearLayout ll;
    Context context;
    private Toast toast;
    String s;
    byte[] b = new byte[100];
    int i =0;
    byte[][] pan = new byte[20][16];
    int x=0,y=0;
    int[] pan_index = new int[20];

    Button connect, scan_pan;


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            int k;

            for (int j = 0; j < arg0.length; j++) {

                if(i==0&&arg0[j]!=0x7E)
                    continue;
                 b[i] = arg0[j];
                 i++;
            }

            if(b[2]+0x04==i)
            {

                if (b[2]==0x15) {
                    //tvAppend(textView, "a\n");
                    for (int j = 0; j < 25; j++) {
                        s = Integer.toHexString(b[j]);
                        CharSequence c = s + " ";
                        //tvAppend(textView, c);

                    }
                    //tvAppend(textView,"\n");
                    PANconnect(b);
                    for(int j=0;j<25;j++)
                        b[j] = 0;
                    b[2]=0x6F;
                }

                else if(b[2] == 0x05)
                {
                    for (int j = 0; j < 9; j++) {
                        s = Integer.toHexString(b[j]);
                        CharSequence c = s + " ";
                        //tvAppend(textView, c);
                    }
                    for(int j=0;j<9;j++)
                        b[j] = 0;

                }
                else if(b[2] > 0x19)
                {
                    //tvAppend(textView, "\nCOR\n");
                    for (int j = 18; j < i; j++) {
                        if(b[j]>64&&b[j]<=90||b[j]>=48&&b[j]<58) {
                            s = Character.toString((char) b[j]);
                            CharSequence c = s + " ";
                            //tvAppend(textView, c);
                        }
                        if(b[j]==0x00)
                            break;
                    }
                    for(int j=0;j<b[2]+4;j++)
                        b[j]=0;
                    //tvAppend(textView, "\n");

                }

                i=0;
            }

//
//            try {
//                data = new String(arg0, "UTF-8");
//                data.concat("/n");
//               // tvAppend(textView, data);
////                tvAppend(textView, Integer.toString(i) + " ");
//                //toast(data);
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
        }
    };
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
//                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
//                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
//                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
//                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);

                            toast("Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(connect);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xbee_connection);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        //textView = (TextView) findViewById(R.id.textView);

        connect = (Button) findViewById(R.id.connect);
        scan_pan = (Button) findViewById(R.id.scan_pan);
//        TextView textView = ()
//        sc = (ScrollView) findViewById(R.id.scroll1);

//        image = (ImageView)findViewById(R.id.image);




//        image.setBackgroundResource(R.drawable.ic_vector);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);


    }

    public void setUiEnabled(boolean bool) {
        connect.setEnabled(!bool);
//        sendButton.setEnabled(bool);
//        stopButton.setEnabled(bool);
////        textView.setEnabled(bool);
//
    }

    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (true)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                    setContentView(R.layout.scan_pan);

                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }

    }

    public void onClickPan(int index){

        byte[] id  = new byte[16];
        int j=0;
        byte[] nd = {0x7E, 0x00, 0x04, 0x08, 0x01, 0x4E, 0x44, 0x64};

        for (j = 0; j < 16; j++) {
            id[j] = (byte) pan[index][j];
        }
        serialPort.write(id);//change pan id


    }
    public void onClickscan(View view){//nd command to get node identifier of all devices on same (same pan id)network
        byte[] nd = {0x7E, 0x00, 0x04, 0x08, 0x01, 0x4E, 0x44, 0x64};
        serialPort.write(nd);
    }

    public void func(){

        //toast("sent");
        String panid = new String();
        int arri1, arri2=0,flag_array;
        String[] pan_array = new String[20];
        ll = (LinearLayout) findViewById(R.id.linear1);
//        TextView tt = (TextView) findViewById(R.id.panid);
//        tt.setText("changed");

       for(int k=0;k<x;k++) {
           toast(Integer.toString(k));
           int flag =0;
           panid = "";
           for (int j = 7; j < 15; j++) {
               if (pan[k][j] != 0 || flag == 1) {
                   panid += Integer.toHexString(pan[k][j]);
                   flag = 1;
               }
           }
            flag_array = 0;
           for(arri1 =0;arri1<arri2;arri1++)
           {
               if(pan_array[arri1].equals(panid))
               {
                   flag_array =1;
                   break;
               }

           }
           if(flag_array==0) {
               pan_array[arri2] = panid;
               pan_index[arri2] = k;
               arri2++;
               TextView tv = new TextView(this);
               tv.setBackgroundResource(R.drawable.text_background);

               ll.addView(tv);
               tv.setPadding(20, 20, 30, 30);
               tv.setText("PAN - " + panid );
               tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
               tv.setTextSize(30);

               final int finalK = k;
               tv.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       onClickPan(finalK);
                       //toast(Integer.toString(finalK));
                   }
               });
           }
       }
//        TransitionManager.beginDelayedTransition();




    }

//    public void spin()
//    {
//        String[] paths = new String[] {"aaa", "bbb", "ccc"} ;
//        Spinner spinner = new Spinner(this);
//
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
//                android.R.layout.simple_spinner_item,paths);
//        ll.addView(spinner);
//
////        spinner.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                toast("onclick activated");
////            }
////        });
//
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);
//
//        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view,
//                                       int position, long id) {
//
//                switch (position) {
//                    case 0:
//                        toast("aaa");
//                        // Whatever you want to happen when the first item gets selected
//                        break;
//                    case 1:
//                        toast("bbb");
//                        break;
//                    case 2:
//                        toast("ccc");
//                        break;
//
//                }
//
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Auto-generated method stub
//            }
//        });
//
//    }







   public void PANconnect(byte[] b) {

        int add=0;
        int j;
        byte[] id = new byte[16];
        id[0] = 0x7E;   id[3] =0x08;
        id[1] = 0x00;   id[4] =0x01;
        id[2] = 0x0C;   id[5] =0x49;
                        id[6] =0x44;

        add = id[3]+id[4]+id[5]+id[6];


        for (int i=12;i<=19;i++)//copy pan id to id
        {
            id[7+i-12] =b[i];
            add+=b[i];
        }

        add = 0xFF-add;
        id[15] = (byte)add;


        for(j=0;j<16;j++)
            pan[x][j] = id[j];
        toast("pan");
        x++;
        y=0;

   }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            toast("hahaha");
            func();
        }
    };

    public void onClickSend(View view) throws IOException {
//        String string = editText.getText().toString();
//        switch (string)
//        {
//            case "1":
//                image.setBackgroundResource(R.drawable.ic_vector);
//                break;
//            case "2":
//                image.setBackgroundResource(R.drawable.ic_vector1);
//                break;
//            case "3":
//                image.setBackgroundResource(R.drawable.ic_vector2);
//                break;
//            case "4":
//                image.setBackgroundResource(R.drawable.ic_vector3);
//                break;
//            case "5":
//                image.setBackgroundResource(R.drawable.ic_vector4);
//                break;
//            case "6":
//                image.setBackgroundResource(R.drawable.ic_vector5);
//                break;
//            case "7":
//                image.setBackgroundResource(R.drawable.ic_vector6);
//                break;
//            case "8":
//                image.setBackgroundResource(R.drawable.ic_vector7);
//                break;
//
//        }

        byte frame1[] = {0x7E, 0x00, 0x04, 0x08, 0x01, 0x41, 0x53, 0x62};//frame for active scan command
        x=0;
        y=0;

        serialPort.write(frame1);
        i=0;
//        sc.setVisibility(View.VISIBLE);
//        scan_pan.setVisibility(View.GONE);

        Runnable runnable = new Runnable() {
            public void run() {

                long endTime = System.currentTimeMillis() + 10*1000;//delay of 10 sec

                while (System.currentTimeMillis() < endTime) {
                    synchronized (this) {
                        try {
                            wait(endTime -
                                    System.currentTimeMillis());
                        } catch (Exception e) {}
                    }

                }
                handler.sendEmptyMessage(0);
//                toast("bbb");
            }
        };
        setContentView(R.layout.pan_ids);

        Thread mythread = new Thread(runnable);
        mythread.start();

        func();

    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        toast("\nSerial Connection Closed! \n");
        serialPort.close();
        unregisterReceiver(broadcastReceiver);
      //  toast("\nSerial Connection Closed! \n");

    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    private void toast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast != null) {
                    toast.cancel();
                    toast = null;
                }
                toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

}