package it.testnode;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;

import org.json.JSONException;
import org.json.JSONObject;

import com.clwillingham.socket.io.IOSocket;
import com.clwillingham.socket.io.MessageCallback;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class TestNodeAndroidActivity extends Activity {
	IOSocket ioSocket;
	MessageCallback myMessageCallback;
	
	private boolean isConnected;
	
	private static final String TAG ="TestNodeAndroidActivity";
	
	private Button connectbtn, sendbtn;
	private EditText urlEditText;
	
	private TextView textViewX, textViewY, textViewZ, textViewColor;
	private TextView xTextViewValue, yTextViewValue, zTextViewValue;
	
	private View colorView;
	
	private Sensor gyroscope;
	private Sensor accelerometer;
	private SensorManager sensorManager;
	private AccelerometerListener accelerometerlistener;
	//private GyroscopeListener gyroscopeListener;
	
	private SensorSendingRunnable sendingThreadRunnable;
	
	private String currentColor;
	private static final String YELLOW = "yellow";
	private static final String GREEN = "green";
	private static final String RED = "red";
	private static final String BLUE = "blue";
	
	private NfcAdapter mAdapter;
	private PendingIntent pendingIntent;
	private IntentFilter [] intentFiltersArray;
	private String [][] techListsArray;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isConnected = false;
        currentColor = YELLOW;
        
        setContentView(R.layout.main);
        connectbtn = (Button)this.findViewById(R.id.button1);
        sendbtn = (Button)this.findViewById(R.id.sendbtn);
        urlEditText = (EditText)this.findViewById(R.id.urlEditText);
        textViewX = (TextView)this.findViewById(R.id.textViewX);
        textViewY = (TextView)this.findViewById(R.id.textViewY);
        textViewZ = (TextView)this.findViewById(R.id.textViewZ);
        
        xTextViewValue = (TextView)this.findViewById(R.id.valueX);
        yTextViewValue = (TextView)this.findViewById(R.id.valueY);
        zTextViewValue = (TextView)this.findViewById(R.id.valueZ);
        
        textViewColor = (TextView)this.findViewById(R.id.textViewColor);
        colorView = (View)this.findViewById(R.id.colorView);
        
        myMessageCallback = new MyMessageCallback();
        
        sensorManager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerlistener = new AccelerometerListener();
        
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //gyroscopeListener = new GyroscopeListener();
        
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(
        	    this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("text/plain");    
        }
        catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
       intentFiltersArray = new IntentFilter[] {ndef, };
       techListsArray = new String[][] { new String[] { NfcA.class.getName() } };
    
    }
    
    private static final int MENU_CANVAS = 1;
    
    public boolean onCreateOptionsMenu (Menu menu){
    	MenuItem menuItem = menu.add(0, MENU_CANVAS, 0, "canvas");
    	return true;
    }
    
    public boolean onOptionsItemSelected (MenuItem item){
    	if(item.getItemId() == MENU_CANVAS){
    		Intent intent = new Intent(this, MappingActivity.class);
    		startActivity(intent);
    	}
    	return true;
    }
    
    
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }
    
    void resolveIntent(Intent intent) {
        // Parse the intent
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            // When a tag is discovered we send it to the service to be save. We
            // include a PendingIntent for the service to call back onto. This
            // will cause this activity to be restarted with onNewIntent(). At
            // that time we read it from the database and view it.
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    String text = TextRecord.parse(msgs[i].getRecords()[0]);
                    Log.d(TAG, "tag found " + text);
                    changeColor(text);
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[] {};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {record});
                msgs = new NdefMessage[] {msg};
            }
            
        } else {
            Log.e(TAG, "Unknown intent " + intent+" action "+action);
            finish();
            return;
        }
    }

   
    
    protected void onResume(){
    	super.onResume();
    	sensorManager.registerListener(accelerometerlistener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    	//sensorManager.registerListener(gyroscopeListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    	 mAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }
    
    protected void onPause(){
    	super.onPause();
    	sensorManager.unregisterListener(accelerometerlistener);
    	//sensorManager.unregisterListener(gyroscopeListener);
    	mAdapter.disableForegroundDispatch(this);
    }
    
    private void changeColor(String textColor){
    	currentColor = textColor;
    	if(currentColor.equals(YELLOW)){
    		colorView.setBackgroundColor(getResources().getColor(R.color.yellow));
    	}
    	else if(currentColor.equals(RED)){
    		colorView.setBackgroundColor(getResources().getColor(R.color.red));
    	}
    	else if(currentColor.equals(GREEN)){
    		colorView.setBackgroundColor(getResources().getColor(R.color.green));
    	}
    	else if(currentColor.equals(BLUE)){
    		colorView.setBackgroundColor(getResources().getColor(R.color.blue));
    	}
    }
    
    private class SensorSendingRunnable implements Runnable{
    	private boolean hasChanged = false;
    	private static final long TIMESLEEP = 20;
    	
    	private float x,y,z;
    	
    	public void set(float x, float y, float z){
    		this.x = x;
    		this.y = y;
    		this.z = z;
    		hasChanged = true;
    	}
    	
		@Override
		public void run() {
			while(isConnected){
				if(hasChanged){
					hasChanged = false;
					try {
						if(ioSocket != null)
							ioSocket.emit("sensorData",  new JSONObject().put("x", ""+x)
									.put("y", ""+y).put("z", ""+z).put("color", currentColor));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch(NotYetConnectedException notYet){
						notYet.printStackTrace();
						Log.w(TAG, "not sent since we're not connected yet");
					}
					try {
						Thread.sleep(TIMESLEEP);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
    	
    }
    
    
    private class GyroscopeListener implements SensorEventListener{

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			
			
			
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			final float x = event.values[0];
			final float y = event.values[1];
			final float z = event.values[2];
			xTextViewValue.setText(""+x);
			yTextViewValue.setText(""+y);
			zTextViewValue.setText(""+z);
			if(isConnected && ioSocket != null){
				sendingThreadRunnable.set(x,y,z);
				
			}
		}
    	
    }
    
    private class AccelerometerListener implements SensorEventListener{

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			final float x = event.values[0];
			final float y = event.values[1];
			final float z = event.values[2];
			xTextViewValue.setText(""+x);
			yTextViewValue.setText(""+y);
			zTextViewValue.setText(""+z);
			if(isConnected && ioSocket != null){
				sendingThreadRunnable.set(x,y,z);
				
			}
			
		}
    	
    }
    
    public void onConnectTapped(View view){
    	ioSocket = new IOSocket("http://"+this.urlEditText.getText().toString(), myMessageCallback);
        
			if(!isConnected){
				
				Thread t = new Thread(){
					public void run(){
						
						try {
							ioSocket.connect();
							
							
							
							Thread t2 = new Thread(){
								public void run(){
									//show log info
									
									connectbtn.setText(getString(R.string.disconnect));
									sendbtn.setVisibility(View.VISIBLE);
									urlEditText.setEnabled(false);
									textViewX.setVisibility(View.VISIBLE);
									textViewY.setVisibility(View.VISIBLE);
									textViewZ.setVisibility(View.VISIBLE);
									xTextViewValue.setVisibility(View.VISIBLE);
									yTextViewValue.setVisibility(View.VISIBLE);
									zTextViewValue.setVisibility(View.VISIBLE);
									colorView.setVisibility(View.VISIBLE);
									textViewColor.setVisibility(View.VISIBLE);
									
									
									//TODO show real UI
								}
							};
							TestNodeAndroidActivity.this.runOnUiThread(t2);
							
							isConnected = true;
							//after we are connected, we create a thread that sends data
							//the thread will finish its job when we disconnect from the server
							sendingThreadRunnable = new SensorSendingRunnable();
							Thread sendingThread = new Thread(sendingThreadRunnable);
							sendingThread.start();
							
							
							
						} catch (IOException e) {
							Log.w(TAG, "cannot connect");
							e.printStackTrace();
						}
						
					}
				};
				t.start();
				
				
			}
			else{
				Thread t = new Thread(){
					public void run(){
						ioSocket.disconnect();
						isConnected = false;
						Thread t2 = new Thread(){
							public void run(){
								
								//hide debug info
								
								connectbtn.setText(getString(R.string.connect));
								sendbtn.setVisibility(View.INVISIBLE);
								urlEditText.setEnabled(true);
								textViewX.setVisibility(View.INVISIBLE);
								textViewY.setVisibility(View.INVISIBLE);
								textViewZ.setVisibility(View.INVISIBLE);
								xTextViewValue.setVisibility(View.INVISIBLE);
								yTextViewValue.setVisibility(View.INVISIBLE);
								zTextViewValue.setVisibility(View.INVISIBLE);
								textViewColor.setVisibility(View.INVISIBLE);
								colorView.setVisibility(View.INVISIBLE);
								
								
								//TODO hide real UI
							}
						};
						TestNodeAndroidActivity.this.runOnUiThread(t2);
						
					}
				};
				t.start();
			}
			
    }
    
    public void sendButtonTapped(View view){
    	Thread t = new Thread(){
    		public void run(){
    			try {
    				ioSocket.send("Hello world");
    				ioSocket.emit("sensorData",  new JSONObject().put("name", "Spot").put("action", "run"));
    			} catch (IOException e) {
    				Log.w(TAG, "cannot send message");
    				e.printStackTrace();
    			} catch (JSONException ex){
    				Log.w(TAG, "json exception");
    				ex.printStackTrace();
    			}
    		}
    	};
    	t.start();
    	// simple message
    	

    	// event with a json message
    	//socket.emit("see", new JSONObject().put("name", "Spot").put("action", "run"));
    }
    
    private class MyMessageCallback implements MessageCallback{

		@Override
		public void on(String event, JSONObject... data) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMessage(String message) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMessage(JSONObject json) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onConnect() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDisconnect() {
			// TODO Auto-generated method stub
			
		}
    	
    }
}