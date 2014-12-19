package com.example.shakedetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Shake extends Activity implements SensorEventListener {
	
	private int N = 165;
    private Sensor mAccelerometer;
	private SensorManager mSensorManager;
	
	private int head = 0;
	
	private float outNum = 0;
	private float Sum = 0;
	private float bufferMean = 0;
	
	private float[] buffer = new float[N];
	private boolean bufferFull = false;
	
	private boolean shaking = false;
	private float alpha = (float) 80;
	private float beta = (float) 70;
	
	private boolean cooldown = false;
	private long cooldownStart = 0;
	private long cooldownEnd = 0;
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_shake);
	// leave screen on for this demo
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
	// setup listener at fastest delay speed
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(Shake.this, mAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);
        
	}

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.shake, menu);
		return true;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
	// Check for the completion of the cooldown
		if (cooldown) {
			if (System.currentTimeMillis() > cooldownEnd){
				cooldown = false;
			}
		} 
		
	// Get the textviews for displaying intermediate data and changing background color
		TextView tvM= (TextView)findViewById(R.id.mean);
		TextView tvS= (TextView)findViewById(R.id.sum);
		TextView tvV= (TextView)findViewById(R.id.value);
		LinearLayout li=(LinearLayout)findViewById(R.id.layout);

		
		//float x = event.values[0];
		//float y = event.values[1];
		//float z = event.values[2];
		
	// Record the data about to be overwritten and record the current magnitude
	// Recording magnitude so as to not worry about gravity's affect on a given orientation
		outNum = buffer[head];
		float x_axisSquare = event.values[0] * event.values[0];
		float y_axisSquare = event.values[1] * event.values[1];
		float z_axisSquare = event.values[2] * event.values[2];
		
		float magnitude = (float) Math.sqrt(x_axisSquare + y_axisSquare + z_axisSquare);
		
		buffer[head] = filter(magnitude);
		tvV.setText(Float.toString(buffer[head]));
		
	// Update the buffer sum
		if (bufferFull) {
			Sum += buffer[head] - outNum;
		} else {
			Sum += buffer[head];
		}	
		tvS.setText(Float.toString(Sum));
		
	// Increment the head
		head = (head + 1) % N;
	
	// Calculate the mean of the buffer
		bufferMean = Sum/N;
		tvM.setText(Float.toString(bufferMean));
		
	// Check if we have filled the buffer 
	// Only empty for the first 164 values
		if (head == 0) {
			bufferFull = true;
		}
		
	// Check against thresholds
		if (bufferFull) {
			// Alpha is rising threshold and Beta is falling threshold
			if (bufferMean > alpha) {
				shaking = true;
			} else if (bufferMean < beta) {
				shaking = false;
			}
		}
		
	// Check for shake and whether we are on cooldown or not
		//if (shaking && !cooldown) {
		if (shaking) {
			//messageBox("Shake Alert","You have shaken the device");
			li.setBackgroundColor(Color.parseColor("#ffff00"));
			
		// Record when shake began and begin cooldown
			cooldownStart = System.currentTimeMillis();
			cooldownEnd = cooldownStart + 1650;
			cooldown = true;
		} else {
			li.setBackgroundColor(Color.parseColor("#ffffff"));
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// can be safely ignored for this demo
	}

// alert box
	private void messageBox(String method, String message) {
		Log.d("EXCEPTION: " + method,  message);
	
		AlertDialog.Builder messageBox = new AlertDialog.Builder(this);
		messageBox.setTitle(method);
		messageBox.setMessage(message);
		messageBox.setCancelable(false);
		messageBox.setNeutralButton("OK", null);
		messageBox.show();
	}

// coefficients for the sinc
	private final double[] h = 
		{
		    -5.761389214612223E-4,
		    -1.6587952906317897E-4,
		    -1.166338194787461E-4,
		    -1.5728304710562774E-5,
		    1.2438572320419756E-4,
		    2.78772526009801E-4,
		    4.1424439786270776E-4,
		    4.948960843086645E-4,
		    4.908784754069886E-4,
		    3.858436843334765E-4,
		    1.83706407986706E-4,
		    -8.922020002879591E-5,
		    -3.8571834280173503E-4,
		    -6.454117236669414E-4,
		    -8.054340440848992E-4,
		    -8.148440436172193E-4,
		    -6.471786379396872E-4,
		    -3.1099911503756206E-4,
		    1.4744004384837855E-4,
		    6.481481609098162E-4,
		    0.0010906153927679064,
		    0.0013719938711604697,
		    0.001409183745356752,
		    0.0011592489945266152,
		    6.350115396237752E-4,
		    -9.048241230349751E-5,
		    -8.916978660757757E-4,
		    -0.001611803897593809,
		    -0.002090050961084154,
		    -0.0021938083741891976,
		    -0.001856759634184891,
		    -0.001092505735631106,
		    -7.728150377945501E-6,
		    0.0012116882515201847,
		    0.0023299143273794354,
		    0.0031036387887359755,
		    0.0033329775129779483,
		    0.002906950749616032,
		    0.001837613883527154,
		    2.7067474335188606E-4,
		    -0.001528777923117972,
		    -0.003218536781814011,
		    -0.004440302514603953,
		    -0.004892317675032046,
		    -0.004396453781132305,
		    -0.002949162188565306,
		    -7.402720088634636E-4,
		    0.0018639125482573984,
		    0.004377095204377606,
		    0.006278038464021256,
		    0.007112799852509905,
		    0.006593406414678947,
		    0.004674482029668269,
		    0.0015883599521771838,
		    -0.002171019333236032,
		    -0.0059193602046422695,
		    -0.008896285269174924,
		    -0.010409389517006009,
		    -0.009979812467344864,
		    -0.007460036229883443,
		    -0.0031025671353401983,
		    0.0024435176922035545,
		    0.008212334180380858,
		    0.013069936373176173,
		    0.01591354577430119,
		    0.015885793814485776,
		    0.012570470668997689,
		    0.006128563906569146,
		    -0.0026517619388603964,
		    -0.01241167979379727,
		    -0.021380752896243194,
		    -0.027623451693498603,
		    -0.029335120920548415,
		    -0.025145546031904806,
		    -0.01437684907360077,
		    0.002785532539283991,
		    0.025245424019811676,
		    0.05108241369697795,
		    0.07776847659959385,
		    0.10248457128171432,
		    0.12249318908385368,
		    0.13551128662322845,
		    0.1400270535522379,
		    0.13551128662322845,
		    0.12249318908385368,
		    0.10248457128171432,
		    0.07776847659959385,
		    0.05108241369697795,
		    0.025245424019811676,
		    0.002785532539283991,
		    -0.01437684907360077,
		    -0.025145546031904806,
		    -0.029335120920548415,
		    -0.027623451693498603,
		    -0.021380752896243194,
		    -0.01241167979379727,
		    -0.0026517619388603964,
		    0.006128563906569146,
		    0.012570470668997689,
		    0.015885793814485776,
		    0.01591354577430119,
		    0.013069936373176173,
		    0.008212334180380858,
		    0.0024435176922035545,
		    -0.0031025671353401983,
		    -0.007460036229883443,
		    -0.009979812467344864,
		    -0.010409389517006009,
		    -0.008896285269174924,
		    -0.0059193602046422695,
		    -0.002171019333236032,
		    0.0015883599521771838,
		    0.004674482029668269,
		    0.006593406414678947,
		    0.007112799852509905,
		    0.006278038464021256,
		    0.004377095204377606,
		    0.0018639125482573984,
		    -7.402720088634636E-4,
		    -0.002949162188565306,
		    -0.004396453781132305,
		    -0.004892317675032046,
		    -0.004440302514603953,
		    -0.003218536781814011,
		    -0.001528777923117972,
		    2.7067474335188606E-4,
		    0.001837613883527154,
		    0.002906950749616032,
		    0.0033329775129779483,
		    0.0031036387887359755,
		    0.0023299143273794354,
		    0.0012116882515201847,
		    -7.728150377945501E-6,
		    -0.001092505735631106,
		    -0.001856759634184891,
		    -0.0021938083741891976,
		    -0.002090050961084154,
		    -0.001611803897593809,
		    -8.916978660757757E-4,
		    -9.048241230349751E-5,
		    6.350115396237752E-4,
		    0.0011592489945266152,
		    0.001409183745356752,
		    0.0013719938711604697,
		    0.0010906153927679064,
		    6.481481609098162E-4,
		    1.4744004384837855E-4,
		    -3.1099911503756206E-4,
		    -6.471786379396872E-4,
		    -8.148440436172193E-4,
		    -8.054340440848992E-4,
		    -6.454117236669414E-4,
		    -3.8571834280173503E-4,
		    -8.922020002879591E-5,
		    1.83706407986706E-4,
		    3.858436843334765E-4,
		    4.908784754069886E-4,
		    4.948960843086645E-4,
		    4.1424439786270776E-4,
		    2.78772526009801E-4,
		    1.2438572320419756E-4,
		    -1.5728304710562774E-5,
		    -1.166338194787461E-4,
		    -1.6587952906317897E-4,
		    -5.761389214612223E-4,
		};

// declarations for the filter	
	private int n = 0;
	private double[] x = new double[N];

/*	Filter specifications:
		Filter type          : Lowpass
		Sample Rate          : 500 Hz
		Passband gain        : 1 dB
		Stopband gain        : -60 dB
		Low band edge        : 30 Hz
		High band edge       : 40 Hz

		Filter design results:
		Filter order         : 164
		Passband weight      : 1
*/
	public float filter(float bufferIn)
	{
	    float bufferOut = (float) 0.0;

	    // Store the current input, overwriting the oldest input
	    x[n] = bufferIn;

	    // Multiply the filter coefficients by the previous inputs and sum
	    for (int i=0; i<N; i++)
	    {
	        bufferOut += h[i] * x[((N - i) + n) % N];
	    }

	    // Increment the input buffer index to the next location
	    n = (n + 1) % N;

	    return bufferOut;
	}
}
