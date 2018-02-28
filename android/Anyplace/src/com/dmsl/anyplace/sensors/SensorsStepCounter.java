

package com.dmsl.anyplace.sensors;

import java.util.ArrayList;
import java.util.List;

import com.dmsl.anyplace.sensors.SensorsMain.IAccelerometerListener;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorsStepCounter implements SensorEventListener, IAccelerometerListener {

	private static final String TAG = "Step Counter";
	private static final float SOFTWARE_LENGTH_KM = 0.00030f;
	private static final float HARDWARE_LENGTH_KM = 0.00055f;
	
	private final boolean forceSoftwareSensor = false;
	
	// Orientation
	public interface IStepListener {
		public void onNewStep(float value);
	}

	private List<IStepListener> stepListeners = new ArrayList<IStepListener>(1);

	public void addListener(IStepListener list) {
		if (!stepListeners.contains(list))
			stepListeners.add(list);
	}

	public void removeListener(IStepListener list) {
		stepListeners.remove(list);
	}

	private void triggerStepListeners(float value) {
		for (IStepListener l : stepListeners) {
			l.onNewStep(value);
		}
	}

	private SoftwareStepCounter softwareSensor = null;
	private SensorsMain sensorsMain;
	private SensorManager mSensorManager = null;
	private boolean isPositioningOn = false;
	private UserData mUserData;

	/**
	 * Constructor
	 * 
	 * @param engine
	 *            the engine object this positioning object belongs to
	 * @param context
	 *            the activity it belongs too.
	 */
	public SensorsStepCounter(Context context, SensorsMain sensorsMain) {
		mUserData = new UserData();

		// get the sensor manager
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

		PackageManager myPackageManager = context.getPackageManager();

		if (forceSoftwareSensor || !myPackageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)) {
			softwareSensor = new SoftwareStepCounter();
			this.sensorsMain = sensorsMain;
		}

	}

	public float getStepLength()
	{
		if (softwareSensor == null) {
			return HARDWARE_LENGTH_KM;
		} else {
			return SOFTWARE_LENGTH_KM;
		}
	}
	
	/**
	 * Pause the position processing
	 */
	public void pause() {
		if (isPositioningOn) {
			Log.i(TAG, "pause");
			isPositioningOn = false;
			if (softwareSensor == null) {
				mSensorManager.unregisterListener(this);
			} else {
				sensorsMain.removeListener((IAccelerometerListener) this);
			}
		}
	}

	/**
	 * Resume the position processing
	 */
	public void resume() {
		if (!isPositioningOn) {
			Log.i(TAG, "resume");
			isPositioningOn = true;

			if (softwareSensor == null) {
				registerSensors();
			} else {
				sensorsMain.addListener((IAccelerometerListener) this);
			}
		}
	}

	// register sensors
	private void registerSensors() {

		Sensor countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
		if (countSensor != null) {
			mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
		}
	}

	/**
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Log.i(TAG, "onSensorChanged: " + event.sensor.getType());

		switch (event.sensor.getType()) {

		case Sensor.TYPE_STEP_COUNTER:
			mUserData.step = event.values[0];
			triggerStepListeners(mUserData.step);
			break;

		}

	}

	@Override
	public void onNewAccelerometer(float[] values) {
		if (softwareSensor.checkIfStep(values)) {
			mUserData.step = softwareSensor.steps;
			triggerStepListeners(mUserData.step);
		}
	}

	/**
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor,
	 *      int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private static class UserData {

		public float step;

		public UserData() {
			step = 0;
		}

		/**
		 * Clone this data set
		 * 
		 * @return a new object with the same user data
		 */
		public UserData clone() {
			UserData data = new UserData();
			data.step = step;
			return data;
		}
	}

}
