package com.example.shouchougen.sepw_control;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.UUID;

public class MainActivity extends Activity {
    // variables for bluetooth
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private String address = null;// =  "98:D3:31:B3:EE:2E";
    public BluetoothAdapter mBluetoothAdapter = null;
    public static BluetoothSocket btSocket = null;
    public static OutputStream outStream = null;
    public static int imageResolution = 25;
    public TextView receiveKey;// = (TextView) findViewById(R.id.textReceive);
    public Button bt_speed_up, bt_speed_down, bt_speed_reset, bt_duration_up, bt_duration_down, bt_duration_reset,bt_allstart,bt_allstop,bt_getstate;
    public ImageButton bt_forward, bt_backward, bt_left, bt_right, bt_stop;
    public ImageButton bt_actuator_1_up, bt_actuator_1_stop,bt_actuator_1_down, bt_actuator_2_up, bt_actuator_2_stop, bt_actuator_2_down;
    public ImageView leftImage, rightImage;
    public ImageView ivWarning;
    public TextView speedView;
    public ScrollView scr;
    public int start_auto = 0;

    int screenWidth, screenHeight;

    // Command of wheelchair movement
    public final char turnOn = 'm';
    public final char turnOFF = 'q';
    public final char getState = 't';
    public final char moveForward = 'f';
    public final char moveBackward = 'b';
    public final char moveLeft = 'l';
    public final char moveRight = 'r';
    public final char moveStop = 's';
    public final char speedUp = 'p';
    public final char speedDown = 'z';
    public final char speedReset = 'h';
    public final char DurationUp = 'x';
    public final char DurationDown = 'v';
    public final char DurationReset = 'o';

    // Command of linear actuator 1 & 2
    public final char linearActuator1_up = 'n';
    public final char linearActuator1_down = 'd';
    public final char linearActuator1_stop = 'a';
    public final char linearActuator2_up = 'u';
    public final char linearActuator2_down = 'k';
    public final char linearActuator2_stop = 'w';

    LinearLayout myLayout;

    // For camera image transmission
    SurfaceView sView;
    SurfaceHolder surfaceHolder;
    Camera camera;                    //define the camera of phone
    boolean isPreview = false;        //check if previewing
    private String ipname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set to full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // set the system not to sleep
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set the main layout
        setContentView(R.layout.activity_main);

        //got the ip information from GetIP.class
        Intent intent = getIntent();
        Bundle data = intent.getExtras();
        ipname = data.getString("ipname");
        address = data.getString("btname");
        imageResolution = data.getInt("resolution");

        // initial bluetooth
        initial_bluetooth();

        // find the views from the layout.xml
        findviews();

        setOrientationButtonListener();
        setActuatorButtonListener();

        // add a callback for surfaceview
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
            }
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera();                                            // initialize the camera
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // if camera != null release the camera
                if (camera != null) {
                    if (isPreview)
                        camera.stopPreview();
                    camera.release();
                    camera = null;
                }
                System.exit(0);
            }
        });

        new Thread(new Runnable() {
            public void run(){
    	    	/*------------------Receive the message from bluetooth and display ----------*/
                while(true){
                    InputStream tmpIn = null;
                    int write_count = 0;
                    //Final String message;

                    // Get the BluetoothSocket input and output streams
                    try {
                        while(true){
                            byte[] rebyte = new byte[100];
                            tmpIn = MainActivity.btSocket.getInputStream();
                            tmpIn.read(rebyte);
                            final String message = new String(new String(rebyte));
                            write_count ++;

                            if(write_count>100){
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        speedView.setText("");
                                    }
                                });
                                write_count = 0;
                            }
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    speedView.append(message);
                                    scr.fullScroll(ScrollView.FOCUS_DOWN);
                                    try{
                                        FileWriter fw = new FileWriter("/storage/sdcard0/Download/output.txt", true);
                                        BufferedWriter bw = new BufferedWriter(fw);
                                        bw.write(message);
                                        //bw.newLine();
                                        bw.close();
                                    }catch(IOException e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {}
                }
            }
        }).start();

        new Thread(new Runnable() {
            public int counter = 1;
            public int first = 1;
            public void run(){
                try{
                    FileWriter fw = new FileWriter("/storage/sdcard0/Download/output.txt", true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write("======================================");
                    bw.write("==============New Start===============");
                    bw.write("======================================");
                    //bw.write("\n" + "Times : " + counter + "\n");
                    bw.close();
                    counter ++;
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void findviews(){
        myLayout = (LinearLayout) findViewById(R.id.my_layout);
        //receiveKey = (TextView) findViewById(R.id.textReceive);
        sView = (SurfaceView) findViewById(R.id.frontImage);                  // got the surfaceview
        surfaceHolder = sView.getHolder();                               // got surface holder

        bt_forward = (ImageButton) findViewById(R.id.btOrientationForward);
        bt_backward = (ImageButton) findViewById(R.id.btOrientationBackward);
        bt_left = (ImageButton) findViewById(R.id.btOrientationLeft);
        bt_right = (ImageButton) findViewById(R.id.btOrientationRight);
        bt_stop = (ImageButton) findViewById(R.id.btOrientationStop);

        bt_speed_up = (Button) findViewById(R.id.btSpeedUp);
        bt_speed_down = (Button) findViewById(R.id.btSpeedDown);
        bt_speed_reset = (Button) findViewById(R.id.Speed_reset);

        bt_duration_up = (Button) findViewById(R.id.btDurationUp);
        bt_duration_down = (Button) findViewById(R.id.btDurationDown);
        bt_duration_reset = (Button) findViewById(R.id.Duration_reset);

        bt_actuator_1_up = (ImageButton) findViewById(R.id.actuator_1_Up);
        bt_actuator_1_stop = (ImageButton) findViewById(R.id.actuator_1_Stop);
        bt_actuator_1_down = (ImageButton) findViewById(R.id.actuator_1_Down);
        bt_actuator_2_up = (ImageButton) findViewById(R.id.actuator_2_Up);
        bt_actuator_2_stop = (ImageButton) findViewById(R.id.actuator_2_Stop);
        bt_actuator_2_down = (ImageButton) findViewById(R.id.actuator_2_Down);

        bt_allstart = (Button) findViewById(R.id.allstart);
        bt_allstop = (Button) findViewById(R.id.allstop);
        bt_getstate = (Button) findViewById(R.id.getstate);

        //ivWarning = (ImageView) findViewById(R.id.ivWarning);
        speedView = (TextView) findViewById(R.id.Speed);
        scr = (ScrollView) findViewById(R.id.scroll);
    }

    private void setOrientationButtonListener(){
        //Send command to wheelchair
        bt_forward.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(moveForward);
            }
        });

        bt_backward.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command( moveBackward);
            }
        });

        bt_left.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(moveLeft);
            }
        });

        bt_right.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(moveRight);
            }
        });

        bt_stop.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command( moveStop);
            }
        });

        bt_speed_up.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(speedUp);
            }
        });

        bt_speed_down.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(speedDown);
            }
        });

        bt_speed_reset.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(speedReset);
            }
        });

        bt_duration_up.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(DurationUp);
            }
        });

        bt_duration_down.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(DurationDown);
            }
        });

        bt_duration_reset.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {send_command(DurationReset);
            }
        });

        bt_allstart.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {send_command(turnOn);
            }
        });

        bt_allstop.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {send_command(turnOFF);
            }
        });

        bt_getstate.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {send_command(getState);
            }
        });

    }

    private void setActuatorButtonListener(){
        //Send Command to Linear Acuator
        bt_actuator_1_stop.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(linearActuator1_stop);
            }
        });

        bt_actuator_1_up.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(linearActuator1_up);
            }
        });

        bt_actuator_1_down.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(linearActuator1_down);
            }
        });

        bt_actuator_2_stop.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(linearActuator2_stop);
            }
        });

        bt_actuator_2_up.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(linearActuator2_up);
            }
        });

        bt_actuator_2_down.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                send_command(linearActuator2_down);
            }
        });
    }
    //KeyListener
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

            return true;
        }
        return super.dispatchKeyEvent(e);
    };



    //Let user can control the sepw with the Keyboard mode by morse switch
    @Override
    public boolean onKeyDown(int keyCode , KeyEvent event){
        switch (keyCode) {
            // Forward
            case KeyEvent.KEYCODE_I:
                Toast.makeText(this, " forward ", Toast.LENGTH_SHORT).show();
                send_command(moveForward);
                break;
            // Backward
            case KeyEvent.KEYCODE_M:
                Toast.makeText(this, " backward ", Toast.LENGTH_SHORT).show();
                send_command(moveBackward);
                break;
            // Left
            case KeyEvent.KEYCODE_T:
                Toast.makeText(this, " left ", Toast.LENGTH_SHORT).show();
                send_command(moveLeft);
                break;
            // Right
            case KeyEvent.KEYCODE_E:
                Toast.makeText(this, " right ", Toast.LENGTH_SHORT).show();
                send_command(moveRight);
                break;
            //Stop
            case KeyEvent.KEYCODE_S:
                Toast.makeText(this, " stop ", Toast.LENGTH_SHORT).show();
                send_command(moveStop);
                break;

            //LA_STOP
            case KeyEvent.KEYCODE_A:
                Toast.makeText(this, "LA_ stop ", Toast.LENGTH_SHORT).show();
                send_command(linearActuator1_stop);
                break;

            //LA_UP
            case KeyEvent.KEYCODE_N:
                Toast.makeText(this, "LA_ up ", Toast.LENGTH_SHORT).show();
                send_command(linearActuator1_up);
                break;

            //LA_DOWN
            case KeyEvent.KEYCODE_D:
                Toast.makeText(this, "LA_ down ", Toast.LENGTH_SHORT).show();
                send_command(linearActuator1_down);
                break;

            //LB_STOP
            case KeyEvent.KEYCODE_W:
                Toast.makeText(this, "LB_ stop ", Toast.LENGTH_SHORT).show();
                send_command(linearActuator2_stop);
                break;

            //LB_UP
            case KeyEvent.KEYCODE_U:
                Toast.makeText(this, "LB_ up ", Toast.LENGTH_SHORT).show();
                send_command(linearActuator2_up);
                break;

            //LB_DOWN
            case KeyEvent.KEYCODE_K:
                Toast.makeText(this, "LA_ down ", Toast.LENGTH_SHORT).show();
                send_command(linearActuator2_down);
                break;

            /*
            //speed_up
            case KeyEvent.KEYCODE_E:
                Toast.makeText(this, "Speed up", Toast.LENGTH_SHORT).show();
                send_command(speedUp);
                break;

            //speed_up
            case KeyEvent.KEYCODE_Z:
                Toast.makeText(this, "Speed up", Toast.LENGTH_SHORT).show();
                send_command(speedDown);
                break;

            //speed_reset
            case KeyEvent.KEYCODE_H:
                Toast.makeText(this, "Speed up", Toast.LENGTH_SHORT).show();
                send_command(speedReset);
                break;

            //duration_up
            case KeyEvent.KEYCODE_X:
                Toast.makeText(this, "Speed up", Toast.LENGTH_SHORT).show();
                send_command(DurationUp);
                break;

            //duration_down
            case KeyEvent.KEYCODE_V:
                Toast.makeText(this, "Speed up", Toast.LENGTH_SHORT).show();
                send_command(DurationDown);
                break;

            //duration_reset
            case KeyEvent.KEYCODE_O:
                Toast.makeText(this, "Speed up", Toast.LENGTH_SHORT).show();
                send_command(DurationReset);
                break;
            */
            default:
                break;
        }
        // Get back the focus from button. First button will be focused without this function.
        myLayout.requestFocus();

        // Keep listen to another event
        return super.onKeyDown(keyCode, event);
    }

    public void initial_bluetooth(){
        /*------------------BT Socket initial----------------*/
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(this.address);

        while(mBluetoothAdapter == null);
        mBluetoothAdapter.cancelDiscovery();

        try {
            btSocket = device.createRfcommSocketToServiceRecord(this.MY_UUID);
            btSocket.connect();
        } catch (IOException e) {}
    	/*------------------BT Socket initial----------------*/
    }

    // Command format : "group" "thing" "action"
    public void send_command(char b){
        try{
            outStream = btSocket.getOutputStream();
            outStream.write(b);
        }catch(IOException e){

        }
        send_end(); /* end after  'd'  Edited by jssmile*/
    }

    public void send_end(){
        try {
            outStream = btSocket.getOutputStream();
            char bt_cmd = 'd';
            outStream.write(bt_cmd);
        } catch (IOException e) {}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initCamera() {
        if (!isPreview) {
            //front carmera = 1  default = back camera
            camera = Camera.open();
        }
        if (camera != null && !isPreview) {
            try{
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewSize(screenWidth, screenHeight);    // set the size of preview image
                parameters.setPreviewFpsRange(10,20);                    // set the preview fps
                parameters.setPictureFormat(ImageFormat.NV21);           // set image format to yuv
                parameters.setPictureSize(screenWidth, screenHeight);    // set the picture size
                camera.setPreviewDisplay(surfaceHolder);                 // set the surface to display
                camera.setDisplayOrientation(0);
                camera.setPreviewCallback(new StreamIt(ipname));         // set the callback function
                camera.startPreview();                                   // start to preview
                camera.autoFocus(null);                                  // set to auto focus
            } catch (Exception e) {
                e.printStackTrace();
            }
            isPreview = true;
        }
    }

    public class UpdateImagesTask extends AsyncTask<ImageView, Bitmap, Bitmap> {

        ImageView imageView = null;
        Bitmap Bitmap_temp =null;
        int frame_count=0; /*calculate the frame count.*/
        double current_time=0;
        @Override
        protected Bitmap doInBackground(ImageView... imageViews) {
            this.imageView = imageViews[0];
            try {
                    /*loop read the webcam of image from the url.*/
                while (true){
                    Bitmap_temp = getBitmapFromURL((String)imageView.getTag());
                    publishProgress(Bitmap_temp); /*update the ImageView.*/
                    frame_count++;
                    Thread.sleep(50); /*sampling period, unit : ms */
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return Bitmap_temp; /*Last Bitmap image result when stop to looping.*/
        }

        @Override
        protected void onProgressUpdate(Bitmap... frame) {
            super.onProgressUpdate(frame);
            imageView.setImageBitmap(frame[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
        }
        /*read the picture from url, the type is Bitmap*/
        private Bitmap getBitmapFromURL(String imageUrl)
        {
            try
            {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                return bitmap;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }

    public class UpdateImagesTask2 extends AsyncTask<ImageView, Bitmap, Bitmap> {

        ImageView imageView = null;
        Bitmap Bitmap_temp =null;
        int frame_count=0; /*calculate the frame count.*/
        double current_time=0;
        @Override
        protected Bitmap doInBackground(ImageView... imageViews) {
            this.imageView = imageViews[0];
            try {
                    /*loop read the webcam of image from the url.*/
                while (true){
                    Bitmap_temp = getBitmapFromURL((String)imageView.getTag());
                    publishProgress(Bitmap_temp); /*update the ImageView.*/
                    frame_count++;
                    Thread.sleep(100); /*sampling period, unit : ms */
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return Bitmap_temp; /*Last Bitmap image result when stop to looping.*/
        }

        @Override
        protected void onProgressUpdate(Bitmap... frame) {
            super.onProgressUpdate(frame);
            imageView.setImageBitmap(frame[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
        }
        /*read the picture from url, the type is Bitmap*/
        private Bitmap getBitmapFromURL(String imageUrl)
        {
            try
            {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                return bitmap;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }

}

class StreamIt implements Camera.PreviewCallback {
    private String ipname;
    public StreamIt(String ipname){
        this.ipname = ipname;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        try{
            //Transfer the image data to JPG
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            if(image!=null){
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), MainActivity.imageResolution, outstream); //12
                outstream.flush();

                //use a new thread to send the jpg
                Thread th = new ImageThread(outstream,ipname);
                th.start();
            }
        }catch(Exception ex){
            Log.e("Sys", "Error:" + ex.getMessage());
        }
    }
}

class ImageThread extends Thread{
    private byte byteBuffer[] = new byte[1024];
    private OutputStream outsocket;
    private ByteArrayOutputStream myoutputstream;
    private String ipname;

    public ImageThread(ByteArrayOutputStream myoutputstream, String ipname){
        this.myoutputstream = myoutputstream;
        this.ipname = ipname;
        try {
            myoutputstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try{
            //send the img through socket
            Socket tempSocket = new Socket(ipname, 6000);
            outsocket = tempSocket.getOutputStream();
            ByteArrayInputStream inputstream = new ByteArrayInputStream(myoutputstream.toByteArray());
            int amount;
            while ((amount = inputstream.read(byteBuffer)) != -1) {
                outsocket.write(byteBuffer, 0, amount);
            }
            myoutputstream.flush();
            myoutputstream.close();
            tempSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class setCommandEnd extends Thread implements Runnable{
    public void run(){
        while(true){
        }
    }
}
