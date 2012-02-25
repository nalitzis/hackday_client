package it.testnode;



import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;


public class MappingActivity extends Activity{
	
	private Sensor accelerometer;
	private SensorManager sensorManager;
	private AccelerometerListener accelerometerlistener;
	
	private float lastX, lastY, lastZ;
	private boolean inited;
	
	private CanvasView canvas;
	private TextView deltaX, newX;
	
	public void onCreate(Bundle savedinstanceState){
		super.onCreate(savedinstanceState);
		this.setContentView(R.layout.mapping);
		inited = false;
		canvas = (CanvasView)this.findViewById(R.id.canvasView);
		deltaX = (TextView)this.findViewById(R.id.deltaX);
		newX = (TextView)this.findViewById(R.id.newX);
		
		
		sensorManager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerlistener = new AccelerometerListener();
        
	}
	
	protected void onResume(){
    	super.onResume();
    	sensorManager.registerListener(accelerometerlistener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    	 
    }
    
    protected void onPause(){
    	super.onPause();
    	sensorManager.unregisterListener(accelerometerlistener);
    	
    }
	
	private void draw(float x, float y, float z){
		/*boolean draw = true;
		float deltaX ;
		if(!inited){
			deltaX = 0;
			inited = true;
		}
		else{
		deltaX	= lastX - x;
		if(deltaX < 0){
			draw = false;
		}
		else{
			
		}
		}
		
		
		int newX = (200+ (int)(10*deltaX));
		this.deltaX.setText("deltaX "+deltaX);
		this.newX.setText("newX "+ newX);
		
		
		Point p = new Point(newX, 200);*/
		
		Point p = new Point(200+ (int)x,200);
		canvas.addPoint(p);
		lastX = x;
		
	}
	
	
	
	private class AccelerometerListener implements SensorEventListener{

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		
		
		private double gravityConstantX = SensorManager.GRAVITY_EARTH;
        private double gravityConstantY = SensorManager.GRAVITY_EARTH;
        private double gravityConstantZ = SensorManager.GRAVITY_EARTH;
 

		@Override
		public void onSensorChanged(SensorEvent event) {
			
			final float x = event.values[0];
			final float y = event.values[1];
			final float z = event.values[2];
			
	        double accX = -x/gravityConstantX;
	        double accY = -y/gravityConstantY;
	        double accZ = -z/gravityConstantZ;
	 
	        double totAcc = Math.sqrt((accX*accX)+(accY*accY)+(accZ*accZ));
	        double tiltXInGs = Math.asin(accX/totAcc);
	        double tiltYInGs = Math.asin(accY/totAcc);
	        double tiltZInGs = Math.asin(accZ/totAcc);
	 
	        //convert to rads, or something close, end up with the opposite angle
	        double tiltX = Math.sin(tiltXInGs);
	        double tiltY = Math.sin(tiltYInGs);
	        double tiltZ = Math.sin(tiltZInGs);
	 
	        //use these angles to calculate gravity  * -1
	        Double gx = (gravityConstantX  * Math.sin(tiltX));
	        Double gy = (gravityConstantY * Math.sin(tiltY));
	        Double gz = (gravityConstantZ * Math.sin(tiltZ));
	 
	        //add the resulting negative gravity and convert the result  (acceleration of the phone relative to the ground)
	        // to centimeters a second
	        Integer resultx = Utils.metersToCentimeters(x + gx);
	        Integer resulty = Utils.metersToCentimeters(y + gy);
	        Integer resultz = Utils.metersToCentimeters(z + gz);
	 
	        //assuming the phone is held on it's back it seems to need this correction
	        resultz -= 100;
			
			
			
			
			
			
			//TODO
			draw(resultx, resulty,resultz);
			
		}
		
		
    	
    }

}
