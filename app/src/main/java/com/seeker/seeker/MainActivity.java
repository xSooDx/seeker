package com.seeker.seeker;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.app.Service;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener{

    private final String PREFS= "SEEKER_SETTINGS";
    private final String PENDING= "PENDING";
    private final String RUNN= "RunN";

    private StorageReference mStorageRef;
    private ArrayList<String> filesToUpload;
    private Iterator<String> UploadIterator;
    private Boolean upCompleted;
    private Button mBtnLogout;
    private Button mBtnUpload;
    private PowerManager.WakeLock mWakeLock = null;
    private PowerManager powerManager;
    private LocationListener mLocationListener;
    private boolean compromised = false;

    STimer timer;

    SharedPreferences settings;
    SharedPreferences.Editor prefEditor;

    ToggleButton mTb;
//    ToggleButton mBad;
    EditText mSRate;
    SensorManager sensorManager;
    private LocationManager locationManager;
    private String provider;

    float[] accSample = new float[3];
    float[] gyroSample = new float[3];
    double[] gpsSample = new double[2];

    private final Object locka = new Object();
    private final Object lockgy = new Object();
    private final Object lockgp = new Object();
    int sampleRate = 4; // Times a second
    long sampleInterval;
//    boolean isBad = false;

    private final Handler mHandler = new Handler();

    FileOutputStream fo;
    SampleTask sampleTask;

    GraphView accGraph;
    LineGraphSeries<DataPoint> accX;
    LineGraphSeries<DataPoint> accY;
    LineGraphSeries<DataPoint> accZ;

    GraphView gyroGraph;
    LineGraphSeries<DataPoint> gyroX;
    LineGraphSeries<DataPoint> gyroY;
    LineGraphSeries<DataPoint> gyroZ;

    BarGraphSeries<DataPoint> GB;

    public BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                unregisterListeners();
                registerListeners();

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settings = getSharedPreferences(PREFS, 0);
        acquireWakeLock();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        filesToUpload = new ArrayList<>();
        prefEditor = settings.edit();
        if(!settings.contains(RUNN)){
            prefEditor.putLong(RUNN,0);
            prefEditor.apply();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timer = new STimer();



//---------------------------------------------------------------------GRAPH RELATED STUFF START
        final int LINE_T = 2;
        accGraph = (GraphView)findViewById(R.id.acc_graph);
        gyroGraph = (GraphView)findViewById(R.id.gyro_graph);

        GB = new BarGraphSeries<>();
        GB.setColor(Color.RED);
        GB.setSpacing(0);
        GB.setDataWidth(1);
        gyroGraph.addSeries(GB);
        accGraph.addSeries(GB);

//        IntentFilter f = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
//        f.setPriority(1000000);
//        registerReceiver(new MediaButtonIntentReceiver(),f);


        accGraph.getViewport().setXAxisBoundsManual(true);
        accGraph.getViewport().setMinX(0);
        accGraph.getViewport().setMaxX(10);
        accGraph.getViewport().setMaxY(15);
        accGraph.getViewport().setMinY(-10);
        accGraph.getViewport().setYAxisBoundsManual(true);
        accGraph.getViewport().setMaxYAxisSize(25);
        accX= new LineGraphSeries<>();
        accY= new LineGraphSeries<>();
        accZ= new LineGraphSeries<>();
        accX.setColor(Color.RED);
        accY.setColor(Color.GREEN);
        accZ.setColor(Color.BLUE);
        accX.setTitle("X");
        accY.setTitle("Y");
        accZ.setTitle("Z");
        accX.setThickness(LINE_T);
        accY.setThickness(LINE_T);
        accZ.setThickness(LINE_T);
        accGraph.addSeries(accX);
        accGraph.addSeries(accY);
        accGraph.addSeries(accZ);
        accGraph.setTitle("Accel");

        gyroGraph.getViewport().setXAxisBoundsManual(true);
        gyroGraph.getViewport().setMinX(0);
        gyroGraph.getViewport().setMaxX(10);
        gyroGraph.getViewport().setMinY(-2);
        gyroGraph.getViewport().setMaxY(2);
        gyroGraph.getViewport().setYAxisBoundsManual(true);
        gyroGraph.getViewport().setMaxYAxisSize(4);
        gyroX= new LineGraphSeries<>();
        gyroY= new LineGraphSeries<>();
        gyroZ= new LineGraphSeries<>();
        gyroX.setColor(Color.RED);
        gyroY.setColor(Color.GREEN);
        gyroZ.setColor(Color.BLUE);
        gyroX.setTitle("X");
        gyroY.setTitle("Y");
        gyroZ.setTitle("Z");
        gyroX.setThickness(LINE_T);
        gyroY.setThickness(LINE_T);
        gyroZ.setThickness(LINE_T);
        gyroGraph.addSeries(gyroX);
        gyroGraph.addSeries(gyroY);
        gyroGraph.addSeries(gyroZ);
        gyroGraph.setTitle("Gyro");

        mTb = (ToggleButton) findViewById(R.id.scanner_toggle);
        mSRate = (EditText) findViewById(R.id.sample_rate);

//---------------------------------------------------------------------GRAPH RELATED STUFF END


//---------------------------------------------------------------------------LOCATION CODE START
        mBtnLogout = (Button) findViewById(R.id.logout);
        mBtnUpload = (Button) findViewById(R.id.upload);
        mBtnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpload(true);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED    )
        {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},0);
        }
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
       // provider = LocationManager.GPS_PROVIDER;


        Location location;
        try{
            location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                onLocationChanged(location);
            }
        }
        catch (Exception e){
            Toast.makeText(this, "ALLOW LOCATION", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

//----------------------------------------------------------------------------------LOCATION CODE END
        mTb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if( isChecked){
//                    mBad.setEnabled(true);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    Log.d("TB","Checked");
                    StringBuilder b = new StringBuilder();
                    GB.resetData(new DataPoint[0]);
                    accX.resetData(new DataPoint[0]);
                    accY.resetData(new DataPoint[0]);
                    accZ.resetData(new DataPoint[0]);
                    gyroX.resetData(new DataPoint[0]);
                    gyroY.resetData(new DataPoint[0]);
                    gyroZ.resetData(new DataPoint[0]);
                    long n =settings.getLong(RUNN,0);

                    SimpleDateFormat formatter = new SimpleDateFormat("dd-M-yyyy-HH-mm-ss");
                    String strDate = formatter.format(new Date());

                    Log.d("GH", "onCheckedChanged: CURRENT TIME: "+ strDate);

                    b.append("r").append(strDate).append(".dat");
                    String currFname = b.toString();
                    startLog(currFname);
                    filesToUpload.add(strDate);
                    try {
                        File f = new File(getExternalFilesDir(null),currFname);
                        //filesToUpload.add(f.getAbsolutePath());
                        Log.d("ADD", "onCheckedChanged: started "+f.getAbsolutePath());
                        Log.d("LIST", "filesToUpload:"+filesToUpload);
                        fo = new FileOutputStream(f);

                        if(f.exists()){Log.i("e","e");} else {Log.i("ne","ne");}
                        Log.i("FP",f.getAbsolutePath());

                        sampleTask = new SampleTask(fo);
                        sampleRate = Integer.parseInt(mSRate.getText().toString());
                        sampleInterval = 1000 / sampleRate;
                        mHandler.postDelayed(sampleTask,sampleInterval);
                        prefEditor.putLong(RUNN,n+1);
                        JSONObject arr = new JSONObject(settings.getString(PENDING,"{}"));
                        arr.put(currFname,true);
                        prefEditor.putString(PENDING,arr.toString());
                        prefEditor.apply();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getBaseContext(),"Error",Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }

                }else{
                    Log.d("TB","UNChecked");
//                    mBad.setChecked(false);
//                    mBad.setEnabled(false);
                    mHandler.removeCallbacks(sampleTask);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    timer.stop();
                    StringBuilder lsb = new StringBuilder();

                    lsb.append("compromised:");
                    lsb.append(String.valueOf(compromised));
                    lsb.append(" \n");
                    Log.d("COMP", "onCheckedChanged: "+lsb.toString());
                    try {
                        fo.write(lsb.toString().getBytes());
                        fo.flush();
                        fo.close();
                        fo=null;
                        stopLog();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    startUpload(false);
                }
            }
        });

        mBtnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            }
        });
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(br,filter);


    }

    private void unregisterListeners(){
        locationManager.removeUpdates(this);
        sensorManager.unregisterListener(this);
    }

    private void registerListeners(){
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try{
            locationManager.requestLocationUpdates(provider, 400, 1, this);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
       registerListeners();

    }

    @Override
    protected void onDestroy() {
        releaseWakeLock();
        unregisterReceiver(br);
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        Log.d("PAU", "onPause: paused");
        unregisterListeners();

        super.onPause();

        //locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 400, 1, this);

//        locationManager.removeUpdates(this);
//        sensorManager.unregisterListener(this);
    }
    @Override
    protected void onStop() {
        super.onStop();

        Log.d("BAD", "onStop: APP STOPPED DUE to SLEEP (?)");
        compromised = true;
    }

    public void startLog(String currFname) {
        File logFile = new File( getExternalFilesDir(null), "logcat" + currFname+".txt" );
        Log.d("LOG", "startLog: logfile made "+logFile.toString());
        try {
            Process process = Runtime.getRuntime().exec("logcat -c"); //clear logcat
            process = Runtime.getRuntime().exec("logcat -f " + logFile);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public void stopLog(){
        try {
            Process process = Runtime.getRuntime().exec("logcat -c"); //clear logcat
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startUpload(Boolean btnClick){

        ConnectivityManager cm;
        NetworkInfo info = null;
        try
        {
            cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                info = cm.getActiveNetworkInfo();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        if (info != null)
        {
            Log.d("NW", "startUpload: network connected");
            Log.d("UP", "startUpload: starting upload");

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String uname = user.getEmail().split("@")[0];
                Log.d("hi", "startUpload: Username:" + uname);

                JSONObject arr1;
                try{
                    arr1 = new JSONObject(settings.getString(PENDING,"{}"));
                }
                catch (JSONException e){
                    arr1 = new JSONObject();
                }
                final JSONObject arr = arr1;
                UploadIterator = arr.keys();

                Boolean more = UploadIterator.hasNext();
                Log.d("", "hasNext?: "+more.toString());
                if(more == false && btnClick==true){
                    Toast.makeText(this, "Nothing left to upload now", Toast.LENGTH_SHORT).show();
                }
                do{
                    upCompleted = false;
                    try {
                        final String path = UploadIterator.next();
                        Log.d("PATH", "startUpload: @" + path);
                        File f = new File(getExternalFilesDir(null) + "/" + path);
                       // Toast.makeText(this, uname, Toast.LENGTH_SHORT).show();
                       // Toast.makeText(this, f.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        Uri file = Uri.fromFile(f);
                        final ProgressDialog progressDialog = new ProgressDialog(this);
                        Log.d("UP", "startUpload: uploading: " + path);
                        progressDialog.setTitle("Uploading...");
                        progressDialog.show();
                        StorageReference mRef = mStorageRef.child("traces/" + uname + "/" + path);
                        final UploadTask uploadTask = mRef.putFile(file);
                        uploadTask
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        Log.d("good", "onSuccess: Upload successful");
                                        Toast.makeText(MainActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show();
                                        arr.remove(path);
                                        prefEditor.putString(PENDING,arr.toString());
                                        prefEditor.apply();
                                        progressDialog.hide();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        exception.printStackTrace();
                                        Log.d("bad", "onFailure: failed");
                                        Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                                        progressDialog.hide();
                                    }
                                })
                                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                   @Override
                                   public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                       ConnectivityManager cm1;
                                       NetworkInfo info1 = null;
                                       try
                                       {
                                           cm1 = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                           if (cm1 != null) {
                                               info1 = cm1.getActiveNetworkInfo();
                                           }
                                       }
                                       catch (Exception e)
                                       {
                                           e.printStackTrace();
                                       }

                                       if (info1 == null)
                                       {
                                           uploadTask.cancel();
                                           Toast.makeText(MainActivity.this, "Upload cancelled due to connectivity issues", Toast.LENGTH_SHORT).show();
                                       }
                                   }
                                });
                        if(uploadTask.isComplete())
                        {
                            upCompleted =true;
                        }
                    }
                    catch (Exception e) {
                        Log.d("up", "startUpload: couldn't open file");
                        e.printStackTrace();
                    }
                }while ( UploadIterator.hasNext());
            }
            else{
                Toast.makeText(this, "Error with user", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Toast.makeText(this, "Network unavailable, will try uploading later..", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            synchronized (locka) {
                System.arraycopy(event.values, 0, accSample, 0, 3);
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            synchronized (lockgy) {
                System.arraycopy(event.values, 0, gyroSample, 0, 3);
            }
        }
    }
    public void acquireWakeLock() {
        /*
         Trying to keep acc, gyro, location sensors active even if locked
        */
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        releaseWakeLock();
        //Acquire new wake lock
        if (powerManager != null) {
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PARTIAL_WAKE_LOCK");
        }
        mWakeLock.acquire(10);
        Log.d("WL", "acquireWakeLock: partial wakelock acquired");
    }

    public void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
        Log.d("WL", "releaseWakeLock: partial wakelock released");
    }



    @Override
    public void onLocationChanged(Location location) {
        synchronized (lockgp) {
            gpsSample[0] = location.getLatitude();
            gpsSample[1] = location.getLongitude();
        }
        Log.d("LOC", "onLocationChanged: loc ch");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class SampleTask implements Runnable{
        FileOutputStream fos;
        String acc;
        String gyr;
        String gps;
        DataPoint ax;
        DataPoint ay;
        DataPoint az;
        DataPoint gx;
        DataPoint gy;
        DataPoint gz;

        final int MAX_DP = sampleRate*10;

        SampleTask(FileOutputStream f){
            super();
            fos = f;
            timer.start();
        }
        @Override
        public void run() {
            long lTime = timer.getTime();
            double time = ((double)lTime)/1000.0;
            StringBuilder sb = new StringBuilder();
            sb.append(time).append(" ");
            Log.i(Long.toString(lTime),Double.toString(time));
            synchronized (locka) {
                ax = new DataPoint(time,accSample[0]);
                ay = new DataPoint(time,accSample[1]);
                az = new DataPoint(time,accSample[2]);
                acc = Arrays.toString(accSample);
                sb.append(accSample[0]).append(" ").append(accSample[1]).append(" ").append(accSample[2]).append(" ");
            }
            synchronized (lockgy) {
                gx = new DataPoint(time,gyroSample[0]);
                gy = new DataPoint(time,gyroSample[1]);
                gz = new DataPoint(time,gyroSample[2]);
                gyr = Arrays.toString(gyroSample);
                sb.append(gyroSample[0]).append(" ").append(gyroSample[1]).append(" ").append(gyroSample[2]).append(" ");
            }
            synchronized (lockgp) {
                gps = Arrays.toString(gpsSample);
                sb.append(gpsSample[0]).append(" ").append(gpsSample[1]).append(" ");
            }
            accX.appendData(ax,true,MAX_DP);
            accY.appendData(ay,true,MAX_DP);
            accZ.appendData(az,true,MAX_DP);
            gyroX.appendData(gx,true,MAX_DP);
            gyroY.appendData(gy,true,MAX_DP);
            gyroZ.appendData(gz,true,MAX_DP);

//            if(isBad){
//                sb.append("BAD");
//                GB.appendData(new DataPoint(time,10.0),true, MAX_DP);
//
//            }else{
//                sb.append("GOOD");
//
//            }
            GB.appendData(new DataPoint(time,0.0),true, MAX_DP);
            sb.append(" \n");

            try {
                fo.write(sb.toString().getBytes());
                fo.flush();
                mHandler.postDelayed(this,sampleInterval);
                Log.i("Saved",sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class MediaButtonIntentReceiver extends BroadcastReceiver {

        public MediaButtonIntentReceiver(){super();}

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = (KeyEvent) intent .getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                if (event == null) {
                    return;
                }

//                if (event.getAction() == KeyEvent.ACTION_DOWN) {
//                    isBad = ! isBad;
//                    mBad.setChecked(isBad);
//                }
            }
        }
    }

}
