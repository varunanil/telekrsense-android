package com.varunanil.telekrsense;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractCollection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int REQUEST_ENABLE_BT = 1;
    TextView tv1 = null;
    TextView tv3 = null;
    //ToggleButton tb1 = null;
    TextView sensor_data = null;
    //static int sensor_data_orient = 0;
    private SensorManager mSensorManager;
    private Sensor mLight;
    float lux = 0;
    private LooperThread looperThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        //BluetoothDevice device = null;
        BluetoothDevice mmDevice = null;
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices) {
                //if (device.getName().equals("WOLFBORG-G6")) {
                    mmDevice = device;
                    break;
                //}
            }
        }

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        BluetoothSocket mmSocket = null;
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mmSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {

            this.looperThread = new LooperThread();
            this.looperThread.setOutputStream(mmSocket.getOutputStream());
            this.looperThread.start();



        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            InputStream mmInputStream = mmSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }




        tv1 = (TextView) findViewById(R.id.textView2);
        tv1.setVisibility(View.GONE);
        tv1.setMovementMethod(new ScrollingMovementMethod());
        tv3 = (TextView) findViewById(R.id.textView3);
        //tb1 = (ToggleButton) findViewById(R.id.toggleButton);
        sensor_data = (TextView) findViewById(R.id.sensor_data);
        //if(sensor_data_orient == 1){
            sensor_data.setVisibility(View.VISIBLE);
        //}


        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> mList= mSensorManager.getSensorList(Sensor.TYPE_ALL);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            // Success! There's a light sensor.
            mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            tv3.setVisibility(View.VISIBLE);
            //tb1.setVisibility(View.VISIBLE);


        }
        else {
            // Failure! No light sensor.
            tv3.setVisibility(View.GONE);
            //tb1.setVisibility(View.GONE);

        }

        /*tb1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(tb1.isChecked() == true){
                    sensor_data.setVisibility(View.VISIBLE);
                    sensor_data_orient = 1;
                }
                else {
                    sensor_data.setVisibility(View.GONE);
                    sensor_data_orient = 0;
                }
            }
        });*/

        for (int i = 1; i < mList.size(); i++) {
            tv1.setVisibility(View.VISIBLE);
            tv1.append("\n" + mList.get(i).getName() + "\n" + mList.get(i).getVendor() + "\n" + mList.get(i).getVersion());
        }


    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        lux = event.values[0];
        sensor_data.setText("Lux: " + lux);
        String outputData = Float.toString(lux);
        this.looperThread.submitReading(outputData);


        // Do something with this sensor value.
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

class LooperThread extends Thread {
    private static final int MSG_TYPE_OUTPUT_DATA = 0;
    private Handler mHandler;
    private volatile OutputStream mOutputStream;

    public void run() {
        Looper.prepare();

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                //try-catch left out for brevity
                String sensor_string = (String) msg.obj;
                byte[] sensor_bytes = sensor_string.getBytes();
                if (msg.what == MSG_TYPE_OUTPUT_DATA && mOutputStream != null) {
                    try {
                        mOutputStream.write(sensor_bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Looper.loop();
    }

    public void submitReading(String outputData) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TYPE_OUTPUT_DATA, outputData));
    }

    public void setOutputStream(OutputStream outputStream) {
        this.mOutputStream = outputStream;
    }
}



/*package com.varunanil.telekrsense;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
*/