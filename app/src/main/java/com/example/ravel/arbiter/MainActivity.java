package com.example.ravel.arbiter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.MediaStore.Files.FileColumns;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;


public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = "getPrivateAlbumStorageDirTAG";
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String TAG = "MainActivity";
    private CameraPreview mPreview;
    private Camera mCamera;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        private  Uri getOutputMediaFileUri(int type){
            return Uri.fromFile(getOutputMediaFile(type));
        }

        /** Create a File for saving an image or video */
        private  File getOutputMediaFile(int type){
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.

            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Arbiter");
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (! mediaStorageDir.exists()){
                if (! mediaStorageDir.mkdirs()){
                    Log.d("MyCameraApp", "failed to create directory");
                    return null;
                }
            }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File mediaFile;
            if (type == MEDIA_TYPE_IMAGE){
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "IMG_"+ timeStamp + ".jpeg");
            } else if(type == MEDIA_TYPE_VIDEO) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_"+ timeStamp + ".mp4");
            } else {
                return null;
            }

            return mediaFile;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(FileColumns.MEDIA_TYPE_IMAGE);



            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                ecrireDansLog("\nCréation d'un socket sur l'@ : 192.168.0.50 et port : 5001");
                sockettest(pictureFile);

            } catch (FileNotFoundException e) {
                Log.d(TAG, "Erreur onPictureTaken - File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Erreur onPictureTaken - Error accessing file: " + e.getMessage());
            }
        }
    };

    public File getPublicAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return file;
    }

    private boolean continuer = true;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final NumberPicker np = (NumberPicker) findViewById(R.id.np);

        //Populate NumberPicker values from minimum and maximum value range
        //Set the minimum value of NumberPicker
        np.setMinValue(1);
        //Specify the maximum value/number of NumberPicker
        np.setMaxValue(10);

        //Gets whether the selector wheel wraps when reaching the min/max value.
        np.setWrapSelectorWheel(true);
        np.setValue(10);

        final TextView textView = (TextView) findViewById(R.id.logs);
        //textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setBackgroundColor(Color.BLACK);
        textView.setTextColor(Color.WHITE);
        textView.setText("Init. du programme...");

        mCamera = getCameraInstance();


        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        final Button boutonOk = (Button) findViewById(R.id.OKBouton);
        boutonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boutonOk.setEnabled(false);

                prendrePhotos(np.getValue() , savedInstanceState);

            }
        });

        Button boutonSTOP = (Button) findViewById(R.id.STOP);

        boutonSTOP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });

        Switch boutonPause = (Switch) findViewById(R.id.startstop);

        boutonPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    continuer = false;
                    textView.append("\nPAUSE=ON");
                }else{
                    continuer = true;
                    textView.append("\nPAUSE=OFF");
                }
            }
        });


    }

    private void prendrePhotos(int freq, final Bundle savedInstanceState)  {

        final TextView texteView = findViewById(R.id.logs);
        texteView.setText(texteView.getText() + "\nFrequence messages : toutes les " + freq + " secondes");

        Context context = getApplicationContext();

        Toast toast = Toast.makeText(context, "Frequence messages : " + freq, Toast.LENGTH_SHORT);
        toast.show();



        //On initialise la caméra -> on regarde si la caméra est accessible

            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
                // this device has a camera
                texteView.setText(texteView.getText() + "\nCaméra accessible : initialisation...");

                new CountDownTimer(999999999,freq*1000){

                    @Override
                    public void onFinish() {
                        mCamera.stopPreview();
                        mCamera.setPreviewCallback(null);

                        mCamera.release();
                        mCamera = null;
                    }

                    @Override
                    public void onTick(long millisUntilFinished) {

                        if(continuer) {

                            long seconds = System.currentTimeMillis() / 1000;
                            long minutes = seconds / 60;
                            long hours = (minutes / 60) + 2;
                            String time = hours % 24 + ":" + minutes % 60 + ":" + seconds % 60;

                            mCamera.startPreview();

                            mCamera.takePicture(null, null, mPicture);

                            texteView.append("\nPhoto prise à " + time);

                            //Server server = new Server(savedInstanceState);


/*
                            try {
                                envoyerPhoto(mPicture);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
*/
                        }

                        //sockettest(mPicture);
                    }

                }.start();


            } else {
                // no camera on this device
                texteView.setText(texteView.getText() + "\nCaméra inaccessible.");
            }


    }

    public void sockettest(final File f) {
        Thread t = new Thread(){

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {


                    try {

                        Socket s = new Socket("192.168.0.50", 5001);
                        //BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
                        s.setSendBufferSize(5000000);

                        //byte[] bytes = new byte[(int) f.length()];
                        //bis.read(bytes, 0, bytes.length);
                        //ecrireDansLog("image vers bytes : "+bytes.toString());

                        try {

                            Bitmap bm = BitmapFactory.decodeFile(f.getPath());

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
                            byte[] b = baos.toByteArray();
                            String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                            //ecrireDansLog("encodedImage : "+encodedImage);
                            System.out.println("encodedImage : "+encodedImage);
                            Log.d("encodedImage : ",encodedImage);


                            //FileOutputStream fos = new FileOutputStream(encodedImage);

                            //ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                            //oos.writeUTF(encodedImage);
                            DataOutputStream dataOutputStream = new DataOutputStream(
                                    s.getOutputStream());
                            //dataOutputStream.writeUTF(encodedImage);
                            writeUTF8(encodedImage, dataOutputStream);
                            dataOutputStream.flush();

                            //read input stream
                    /*
                    DataInputStream dis2 = new DataInputStream(s.getInputStream());
                    InputStreamReader disR2 = new InputStreamReader(dis2);
                    BufferedReader br = new BufferedReader(disR2);//create a BufferReader object for input
                    */
                            //print the input to the application screen
                            //final TextView receivedMsg = (TextView) findViewById(R.id.logs);
                            //receivedMsg.setText(br.toString());
                            //System.out.println("br : "+br);
                            //System.out.println("br.tostring : "+br.toString());
                            //ecrireDansLog(br.toString());

                            //dis2.close();
                            dataOutputStream.close();
                        } catch (Exception e) {
                            System.err.println("Erreur :" + e.getLocalizedMessage());
                            //ecrireDansLog("Erreur :" + e.getLocalizedMessage()+"thread PID : "+currentThread().getId());
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur sockettest : ");
                        e.printStackTrace();
                        ecrireDansLog("Erreur sockettest : "+e.getLocalizedMessage());
                    }


            }
        };
        t.start();
        Toast.makeText(this, "The message has been sent", Toast.LENGTH_SHORT).show();
        t.interrupt();
    }

    public void writeUTF8(String s, DataOutput out) throws IOException {
        byte [] encoded = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(encoded.length);
        out.write(encoded);
    }

    private void ecrireDansLog(String s) {
        TextView l = findViewById(R.id.logs);
        l.append("\n" + s);
    }



    private void envoyerPhoto(Camera.PictureCallback mPicture) throws IOException {

        //Socket socket = new Socket(InetAddress.getLocalHost(),5001);
/*
        ImageView iv= (ImageView) mPicture;
        Bitmap bmp=((BitmapDrawable)iv.getDrawable()).getBitmap();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte array [] = baos.toByteArray();

        int start=0;
        int len=array.length;
        if (len < 0)
            throw new IllegalArgumentException("Negative length not allowed");
        if (start < 0 || start >= array.length)
            throw new IndexOutOfBoundsException("Out of bounds: " + start);

        OutputStream out = socket.getOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeInt(len);
        if (len > 0) {
            dos.write(array, start, len);
        }
*/



    }

    private void traiterPhoto(Camera.PictureCallback mPicture) {

        TextView textView = (TextView) findViewById(R.id.logs);

            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {

                textView.append("\nStorage externe accessible en lecture & écriture");

                getPublicAlbumStorageDir("Arbiter");





            }else{
                textView.append("\nStorage externe non accessible");
            }



    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                System.err.println("Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                System.err.println("Error starting camera preview: " + e.getMessage());
            }
        }


    }


}
