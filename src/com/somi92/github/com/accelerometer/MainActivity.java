package com.somi92.github.com.accelerometer;

import java.util.ArrayList;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

	private LinearLayout layout;
	private View mChart;
	
	private SensorManager mSensorManager;
	private Sensor mAccSensor;
	
	private boolean isRunning;
	private float alphaFactor;
	private boolean isHighPassEnabled;
	
	private TextView tvX, tvY, tvZ;
	private Button btnStart, btnStop;
	private EditText etAlpha;
	private CheckBox chbHighpass;
	
	private ArrayList<AcceleratorData> dataSet;
	
	private static final double alphaFactorDefault = 0.1f;
	private double[] gravity = new double[3];
	
	private long opservationCounter;
//	ProgressDialog dialog;
	
//	private AsyncTask<>, ProgressDialog, Void> plotterTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		alphaFactor = prefs.getFloat(getString(R.string.pref_alpha), (float)alphaFactorDefault);
		isHighPassEnabled = prefs.getBoolean(getString(R.string.pref_highpass), true);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		isRunning = false;
		opservationCounter = 0;
		
		chbHighpass = (CheckBox) findViewById(R.id.high_pass);
		chbHighpass.setChecked(isHighPassEnabled);
		
		btnStart = (Button) findViewById(R.id.btnStart);
		btnStop = (Button) findViewById(R.id.btnStop);
		tvX = (TextView) findViewById(R.id.tvX);
		tvY = (TextView) findViewById(R.id.tvY);
		tvZ = (TextView) findViewById(R.id.tvZ);
		
		etAlpha = (EditText) findViewById(R.id.alpha_input);
		if(isHighPassEnabled) {
			etAlpha.setEnabled(true);
		} else {
			etAlpha.setEnabled(false);
		}
		
		tvX.setText("X axis:");
		tvY.setText("Y axis:");
		tvZ.setText("Z axis:");
		
		etAlpha.setText(alphaFactor+"");
		
		layout = (LinearLayout) findViewById(R.id.chart);
		
		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
		
		btnStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				
				try {
					
					gravity[0] = 0;
					gravity[1] = 0;
					gravity[2] = 0;
					
					opservationCounter = 0;
					
					float alpha = Float.parseFloat(etAlpha.getText().toString());
					alphaFactor = alpha;
					
					if(alphaFactor<0 || alphaFactor>1){
						Toast.makeText(MainActivity.this, "Invalid alpha filter coeficient input. Must be in range 0 <= alpha <= 1. Try again.", Toast.LENGTH_SHORT).show();
						return;
					}
					
					SharedPreferences.Editor prefsEditor = prefs.edit();
					prefsEditor.putFloat(getString(R.string.pref_alpha), alphaFactor);
					prefsEditor.putBoolean(getString(R.string.pref_highpass), isHighPassEnabled);
					prefsEditor.commit();
					Toast.makeText(MainActivity.this, "Scan started... Press stop to finish.", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					// TODO: handle exception
					Toast.makeText(MainActivity.this, "Invalid alpha filter coeficient input. Try again.", Toast.LENGTH_SHORT).show();
					return;
				}
				
				btnStart.setEnabled(false);
	            btnStop.setEnabled(true);
	            chbHighpass.setEnabled(false);
	            etAlpha.setEnabled(false);
	            dataSet = new ArrayList<AcceleratorData>();
	            isRunning = true;
	            mSensorManager.registerListener(MainActivity.this, mAccSensor, SensorManager.SENSOR_DELAY_FASTEST);
			}
		});
		
		btnStop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				isRunning = false;
				btnStart.setEnabled(true);
	            btnStop.setEnabled(false);
	            chbHighpass.setEnabled(true);
	            etAlpha.setEnabled(true);
				mSensorManager.unregisterListener(MainActivity.this);
				Toast.makeText(MainActivity.this, "Scan finished. Opservations taken: "+opservationCounter, Toast.LENGTH_SHORT).show();
				layout.removeAllViews();
//				displayChart();
				new PlottingTask().execute();
			}
		});
		
		chbHighpass.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				isHighPassEnabled = chbHighpass.isChecked();
				if(isHighPassEnabled) {
					etAlpha.setEnabled(true);
				} else {
					etAlpha.setEnabled(false);
				}
			}
		});
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
//		mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		 if (isRunning == false) {
	            mSensorManager.unregisterListener(this);
	        }
	}
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (isRunning) {
			
			opservationCounter++;
			
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            
            double[] result;
            
            if(isHighPassEnabled) {
            	result = filterData(x, y, z);
            } else {
            	result = new double[3];
            	result[0] = x;
            	result[1] = y;
            	result[2] = z;
            }
            
            tvX.setText("X axis: \t\t"+result[0]);
    		tvY.setText("Y axis: \t\t"+result[1]);
    		tvZ.setText("Z axis: \t\t"+result[2]);
            
            long timestamp = System.currentTimeMillis();
            
            AcceleratorData data = new AcceleratorData(timestamp, result[0], result[1], result[2]);
            dataSet.add(data);
        }
	}
	
	private void displayChart() {
		if (dataSet != null || dataSet.size() > 0) {

            long t = dataSet.get(0).getTimestamp();
            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
 
            XYSeries xSeries = new XYSeries("X");
            XYSeries ySeries = new XYSeries("Y");
            XYSeries zSeries = new XYSeries("Z");
 
            for (AcceleratorData data : dataSet) {
                xSeries.add(data.getTimestamp() - t, data.getX());
                ySeries.add(data.getTimestamp() - t, data.getY());
                zSeries.add(data.getTimestamp() - t, data.getZ());
            }
 
            dataset.addSeries(xSeries);
            dataset.addSeries(ySeries);
            dataset.addSeries(zSeries);
 
            XYSeriesRenderer xRenderer = new XYSeriesRenderer();
            xRenderer.setColor(Color.RED);
            xRenderer.setFillPoints(true);
            xRenderer.setLineWidth(1);
            xRenderer.setDisplayChartValues(false);
 
            XYSeriesRenderer yRenderer = new XYSeriesRenderer();
            yRenderer.setColor(Color.GREEN);
            yRenderer.setFillPoints(true);
            yRenderer.setLineWidth(1);
            yRenderer.setDisplayChartValues(false);
 
            XYSeriesRenderer zRenderer = new XYSeriesRenderer();
            zRenderer.setColor(Color.BLUE);
            zRenderer.setFillPoints(true);
            zRenderer.setLineWidth(1);
            zRenderer.setDisplayChartValues(false);
 
            XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
            multiRenderer.setXLabels(0);
            multiRenderer.setLabelsColor(Color.RED);
            multiRenderer.setChartTitle("t vs (x,y,z)");
            multiRenderer.setXTitle("Sensor Data: "+opservationCounter+" opservations");
            multiRenderer.setYTitle("Values of Acceleration Axis");
            multiRenderer.setZoomButtonsVisible(true);
//            for (int i = 0; i < dataSet.size(); i++) {
// 
//                multiRenderer.addXTextLabel(i + 1, ""
//                        + (dataSet.get(i).getTimestamp() - t));
//            }
//            for (int i = 0; i < 12; i++) {
//                multiRenderer.addYTextLabel(i + 1, ""+i);
//            }
 
            multiRenderer.addSeriesRenderer(xRenderer);
            multiRenderer.addSeriesRenderer(yRenderer);
            multiRenderer.addSeriesRenderer(zRenderer);
 
            mChart = ChartFactory.getLineChartView(getBaseContext(), dataset,
                    multiRenderer);
 
            layout.addView(mChart);
		}
//		dialog.dismiss();
	}
	
	private double[] filterData(double x, double y, double z) {
		double[] result = new double[3];		
		
		gravity[0] = alphaFactor * gravity[0] + (1 - alphaFactor) * x;
		gravity[1] = alphaFactor * gravity[1] + (1 - alphaFactor) * y;
		gravity[2] = alphaFactor * gravity[2] + (1 - alphaFactor) * z;
		
		result[0] = x - gravity[0];
		result[1] = y - gravity[1];
		result[2] = z - gravity[2];
		
		return result;
	}
	
	public class PlottingTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
//			dialog = ProgressDialog.show(MainActivity.this, "Please wait", "Plotting sensor data...");
			displayChart();
		}
	}
}
