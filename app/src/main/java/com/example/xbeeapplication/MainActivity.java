package com.example.xbeeapplication;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "com.example.xbeeapplication.USB_PERMISSION";
    Button stopButton;
    ImageView image;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    LinearLayout ll;
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

            if(b[2]+0x04==i)//condition for complete data frames
            {

                if (b[2]==0x15) {//receive pan id
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

                else if(b[2] == 0x05)//receive ok status on connection
                {
                    for (int j = 0; j < 9; j++) {
                        s = Integer.toHexString(b[j]);
                        CharSequence c = s + " ";
                    }
                    //toast("select node");
                    for(int j=0;j<9;j++)
                        b[j] = 0;

                }

                else if(b[2] == 0x0D)//receive current pan
                {
                    String temp = new String();
                    String panid ="";
                    int flag = 0;
                    for (int j = 8; j < 16; j++) {
                        if (b[j] != 0 || flag == 1) {
                            temp = Integer.toHexString(b[j]);
                            if (temp.length()==1)
                                temp = "0"+temp;
                            flag = 1;
                            panid += temp;

                        }
                    }
                    toast(panid);

                }
                else if(b[2] == 0x10)
                {
                    byte rec_data[] = {b[17], b[18]};
                    send_data(rec_data);

                    for(int j=0;j<b[2]+4;j++)
                        b[j]=0;
                }

                else if(b[2] > 0x19)
                {
                    for (int j = 5; j < i; j++) {
//                        if(b[j]>64&&b[j]<=90||b[j]>=48&&b[j]<58) {
//                            s = Character.toString((char) b[j]);
//                           nd = nd + s;
//                        }
                        if(b[j]==0x00&&(int)b[j+1]==(int)0x13)
                        {
                            toast("aaa");
                            nd_command(b, j);
                            break;
                        }
                    }
                    for(int j=0;j<b[2]+4;j++)
                        b[j]=0;

                }
                i=0;
            }


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
        connect = (Button) findViewById(R.id.connect);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
    }

    public void setUiEnabled(boolean bool) {
        connect.setEnabled(!bool);
    }

    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (true)//add Vendor ID for
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                    if (readSavedData().equals(""))
                        setContentView(R.layout.scan_pan);
                    else
                        setContentView(R.layout.display_main);

                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
        else
        {
            toast("connect your device first.");
        }

    }





//------------------------------code for scan pan layout---------------------------//


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //toast("hahaha");
            func();
        }
    };
    public void onClickSend(View view) throws IOException {

        byte frame1[] = {0x7E, 0x00, 0x04, 0x08, 0x01, 0x41, 0x53, 0x62};//frame for active scan command
        x=0;
        y=0;

        serialPort.write(frame1);
        i=0;

        //delay of 10s
        Runnable runnable = new Runnable() {
            public void run() {

                long endTime = System.currentTimeMillis() + 2*1000;//delay of 10 sec

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
        toast("wait for few seconds");
        setContentView(R.layout.pan_ids);

        Thread mythread = new Thread(runnable);
        mythread.start();

    }
    public void func(){//function to not show repeated pan ids and create a textview of each

        // toast("sent");
        String panid = "";
        String temp = "";
        int arri1, arri2=0,flag_array;
        String[] pan_array = new String[20];
        ll = (LinearLayout) findViewById(R.id.linear1);

        for(int k=0;k<x;k++) {//to iterate all panids in pan 2d array
            toast(Integer.toString(k));
            int flag =0;// flag to indicate that copying of panid has started
            panid = "";//string to store panid in string format
            for (int j = 7; j < 15; j++) {
                if (pan[k][j] != 0 || flag == 1) {
                    temp = Integer.toHexString(pan[k][j]);
                    if (temp.length()==1)
                        temp = "0"+temp;
                    panid += temp;
                    flag = 1;
                }
            }
            flag_array = 0;
            for(arri1 =0;arri1<arri2;arri1++)//check for duplicate pan ids
            {
                if(pan_array[arri1].equals(panid))
                {
                    flag_array = 1;
                    break;
                }

            }
            if(flag_array==0) {//if there is no duplicates
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
                        toast(Integer.toString(finalK)+" - command sent");
                        onClickPan(finalK);
                        //toast(Integer.toString(finalK));
                    }
                });

            }
        }
        toast(Integer.toString(arri2));
    }




//---------------------------------code for pan_ids layout -------------------------//




    public void PANconnect(byte[] b) {//redirected from broadcast receiver to copy pan ids and create ID command for each pan id

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


        for(j=0;j<16;j++)//store pan id in pan 2d array
            pan[x][j] = id[j];
        toast("scanning networks");
        x++;
        y=0;
    }

    @SuppressLint("HandlerLeak")
    Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            toast("wait for 5 sec");
            setContentView(R.layout.devices_nd);

        }
    };

    public void onClickPan(int index) {//to change pan id (main command)

        byte[] id  = new byte[16];
        String temp = "";
        int j=0;
        // byte[] nd = {0x7E, 0x00, 0x04, 0x08, 0x01, 0x4E, 0x44, 0x64};

        for (j = 0; j < 16; j++) {
            id[j] = (byte) pan[index][j];
            // temp = temp + Integer.toHexString(id[j]);
        }
        // toast(temp);
        serialPort.write(id);;//change pan id


        //delay of 5sec
        Runnable runnable = new Runnable() {
            public void run() {

                long endTime = System.currentTimeMillis() + 3*1000;//delay of 5 sec

                while (System.currentTimeMillis() < endTime) {
                    synchronized (this) {
                        try {
                            wait(endTime -
                                    System.currentTimeMillis());
                        } catch (Exception e) {}
                    }

                }
                handle.sendEmptyMessage(0);
//                toast("bbb");
            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }






//----------------------------code for devices_nd layout starts------------------------//



    public void current_pan(View view) {//to show current panid
        byte id[] = {0x7E, 0x00, 0x04, 0x08, 0x01, 0x49, 0x44, 0x69};
        serialPort.write(id);
    }

    //node discovery button function//
    public void onClickscan(View view){//nd command to get node identifier of all devices on same (same pan id)network
        byte[] nd = {0x7E, 0x00, 0x04, 0x08, 0x01, 0x4E, 0x44, 0x64};
        serialPort.write(nd);
    }

    public void nd_command(byte[] b, int j)//called from broadcast receiver to show all node identifier
    {
        String ni = new String();
        String mac = new String();
        String temp = new String();
        int k;

        for(k = j;k<j+8;k++)
        {
            temp = Integer.toHexString((int)b[k]);
            if(temp.length()>2)
                temp = temp.substring(6);
            else if (temp.length() == 1)
                temp = "0"+temp;

            mac = mac + temp;
        }
        j=k;
        for(j=j;b[j]!=0x00;j++)
        {
            ni = ni + Character.toString((char)(b[j]));
        }

        toast(mac+"\n"+ni);


        final String finalNI = ni;
        final String finalMAC = mac;
        final byte data[] = b;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                ll = (LinearLayout) findViewById(R.id.linearlay);
                TextView tv = new TextView(MainActivity.this);
                tv.setBackgroundResource(R.drawable.text_background);
                ll.addView(tv);
                tv.setPadding(20, 20, 30, 30);
                tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setTextSize(25);
                tv.setText("NI - "+finalNI + "\n" + "MAC - "+finalMAC);

                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        try {
                            storedata(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            storedata(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setContentView(R.layout.display_main);
                        image = (ImageView)findViewById(R.id.image);
                        image.setBackgroundResource(R.drawable.ic_vector);

                    }
                });
            }
        });
    }





   //-------------------------------code for display main layout----------------------//





    public void tempclick(View view) {//on clicking next button,open display_main layout and add water tank image

//        byte id[] = {0x7E, 0x00, 0x0C, 0x08, 0x01, 0x49, 0x44, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x55, 0x55, (byte)0xBF};
//        serialPort.write(id);;//change pan id

        setContentView(R.layout.display_main);
        image = (ImageView)findViewById(R.id.image);
        image.setBackgroundResource(R.drawable.ic_vector);
    }

    public void demosend(View view) {//demo button to send value for water tank levels

//        EditText ed = (EditText)findViewById(R.id.edit_demo);
//        String s = ed.getText().toString();
        image = (ImageView)findViewById(R.id.image);
        image.setBackgroundResource(R.drawable.ic_vector);

        TextView tv = (TextView)findViewById(R.id.text_data);
        byte[] b = readSavedData().getBytes();
        String string = new String();
        String temp = "";
        for(int k =0;k<b.length;k++) {
            temp = Integer.toHexString((int)b[k]);
            if(temp.length()>2)
                temp = temp.substring(6);
            else if (temp.length() == 1)
                temp = "0"+temp;

            string = string + temp + " ";
        }

        tv.setText(string);

    }
    
    public void send_data(byte[] data){//not tested
        
        int x = data[0]<<8 + data[1];
            if(x<100)
                image.setBackgroundResource(R.drawable.ic_vector1);
            else if(x<200)
                image.setBackgroundResource(R.drawable.ic_vector2);
            else if(x<300)
                image.setBackgroundResource(R.drawable.ic_vector3);
            else if(x<400)
                image.setBackgroundResource(R.drawable.ic_vector4);
            else if(x<500)
                image.setBackgroundResource(R.drawable.ic_vector5);
            else if(x<600)
                image.setBackgroundResource(R.drawable.ic_vector6);
            else if(x<700)
                image.setBackgroundResource(R.drawable.ic_vector7);
            else if(x<800)
                image.setBackgroundResource(R.drawable.ic_vector8);
            else if(x>800)
                image.setBackgroundResource(R.drawable.ic_vector9);

    }
    

    public void storedata(byte[] data) throws IOException {//to store nd command response containing mac address and node identifier.
        FileOutputStream fOut = openFileOutput("NI_data",Context.MODE_PRIVATE);
        fOut.write(data);
        fOut.close();
    }

    public String readSavedData ( ) {
        StringBuffer datax = new StringBuffer("");
        try {
            FileInputStream fIn = openFileInput ( "NI_data" ) ;
            InputStreamReader isr = new InputStreamReader ( fIn ) ;
            BufferedReader buffreader = new BufferedReader ( isr ) ;

            String readString = buffreader.readLine ( ) ;
            while ( readString != null ) {
                datax.append(readString);
                readString = buffreader.readLine ( ) ;
            }

            isr.close ( ) ;
        } catch ( IOException ioe ) {
            ioe.printStackTrace ( ) ;
        }
        return datax.toString() ;
    }

    
    public void onClickStop(View view) {
        setUiEnabled(false);
        toast("\nSerial Connection Closed! \n");
        serialPort.close();
        unregisterReceiver(broadcastReceiver);
      //  toast("\nSerial Connection Closed! \n");

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
