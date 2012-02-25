package it.testnode;



import java.io.IOException;
import java.nio.channels.NotYetConnectedException;

import org.json.JSONException;
import org.json.JSONObject;



import com.clwillingham.socket.io.IOSocket;
import com.clwillingham.socket.io.MessageCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

public class DemoActivity extends Activity {
	
	private String url;
	
	IOSocket ioSocket;
	MessageCallback myMessageCallback;
	
	private boolean isConnected;
	
	
	private FrameLayout backgroundView;
	
	private static final String TAG = "DemoActivity";
	
	private static final int MENU_CONNECT = 1;
	
	private SensorSendingRunnable sendingThreadRunnable;
	
	private Sensor accelerometer;
	private SensorManager sensorManager;
	private AccelerometerListener accelerometerlistener;
	
	private String currentColor;
	private static final String YELLOW = "yellow";
	private static final String GREEN = "green";
	private static final String RED = "red";
	private static final String BLUE = "blue";
	
	private NfcAdapter mAdapter;
	private PendingIntent pendingIntent;
	private IntentFilter [] intentFiltersArray;
	private String [][] techListsArray;
	
	private AlertDialog dialog;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo);
		backgroundView = (FrameLayout)this.findViewById(R.id.backgroundView);
		isConnected = false;
        currentColor = YELLOW;
        
        myMessageCallback = new MyMessageCallback();
        
        sensorManager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerlistener = new AccelerometerListener();
        
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
    
       dialog = createAlertDialogWithUrl();
	}
	
	private AlertDialog createAlertDialogWithUrl(){
		LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_url, null);
        return new AlertDialog.Builder(DemoActivity.this)
            .setTitle("Connect URL")
            .setView(textEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	EditText editText = (EditText)textEntryView.findViewById(R.id.urlEditText);
                	url = editText.getText().toString();
                	connect();
                    /* User clicked OK so do some stuff */
                }
            })
            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	dialog.dismiss();
                    /* User clicked cancel so do some stuff */
                }
            })
            .create();
    }
	
	
	public boolean onOptionsItemSelected (MenuItem item){
		if(item.getItemId() == MENU_CONNECT){
			if(isConnected){
				disconnectFromServer();
				
			}else{
				dialog.show();
			}
			
		}
		return true;
	}
	
	private MenuItem menuItem;
	public boolean onCreateOptionsMenu (Menu menu){
    	menuItem = menu.add(0, MENU_CONNECT, 0, "Connect");
    	return true;
    }
	
	 protected void onStart(){
	    	super.onStart();
	    	sensorManager.registerListener(accelerometerlistener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	    	}
	    
	    protected void onStop(){
	    	super.onStop();
	    	sensorManager.unregisterListener(accelerometerlistener);
	    	}
	    
	    protected void onResume(){
	    	super.onResume();
	    	mAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
		    
	    }
	    
	    protected void onPause(){
	    	super.onPause();
	    	mAdapter.disableForegroundDispatch(this);
		    
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
	
	private void changeColor(String textColor){
    	currentColor = textColor;
    	if(currentColor.equals(YELLOW)){
    		backgroundView.setBackgroundColor(getResources().getColor(R.color.yellow));
    	}
    	else if(currentColor.equals(RED)){
    		backgroundView.setBackgroundColor(getResources().getColor(R.color.red));
    	}
    	else if(currentColor.equals(GREEN)){
    		backgroundView.setBackgroundColor(getResources().getColor(R.color.green));
    	}
    	else if(currentColor.equals(BLUE)){
    		backgroundView.setBackgroundColor(getResources().getColor(R.color.blue));
    	}
    }
    

	
	private void connect(){
		ioSocket = new IOSocket("http://"+url, myMessageCallback);
        
		if(!isConnected){
			
			Thread t = new Thread(){
				public void run(){
					
					try {
						ioSocket.connect();
						
						
						
						Thread t2 = new Thread(){
							public void run(){
								//show log info
								menuItem.setTitle("Disconnect");
								
								//TODO show real UI
							}
						};
						DemoActivity.this.runOnUiThread(t2);
						
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
	}
	
	private void disconnectFromServer(){
		Thread t = new Thread(){
			public void run(){
				isConnected = false;
				ioSocket = null;
				//ioSocket.disconnect();
				
				
				Thread t2 = new Thread(){
					public void run(){
						
						//hide debug info
						menuItem.setTitle("Connect");
						
						
						
						//TODO hide real UI
					}
				};
				DemoActivity.this.runOnUiThread(t2);
				
			}
		};
		t.start();
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
			
			if(isConnected && ioSocket != null){
				sendingThreadRunnable.set(x,y,z);
				
			}
			
		}
    	
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
