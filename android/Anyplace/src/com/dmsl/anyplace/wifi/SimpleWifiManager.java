
package com.dmsl.anyplace.wifi;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.dmsl.anyplace.MyApplication;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class SimpleWifiManager {

	private final static Long DEFAULT_INTERVAL = 2000L;
	private static SimpleWifiManager mInstance = null;

	/**
	 * Creates a new instance
	 * 
	 */
	public static SimpleWifiManager getInstance() {
		
		if (mInstance == null) {
			synchronized (MyApplication.getAppContext()) {
				if (mInstance == null) {
					mInstance = new SimpleWifiManager(MyApplication.getAppContext());
				}
			}
		}
		return mInstance;
	}

	/** WiFi manager used to scan and get scan results */
	private final WifiManager mainWifi;

	/**
	 * Intent with the SCAN_RESULTS_AVAILABLE_ACTION action will be broadcast to
	 * asynchronously announce that the scan is complete and results are
	 * available.
	 */
	private final IntentFilter wifiFilter;

	/** Timer to perform new scheduled scans */
	private final Timer timer;

	/** Task to perform a scan */
	private TimerTask WifiTask;

	/** Application context */
	private final Context mContext;

	/** If Scanning, true or false */
	private Boolean isScanning;

	/**
	 * Creates a new instance
	 * 
	 * @param context
	 *            Application context
	 */
	private SimpleWifiManager(Context context) {
		mContext = context;
		isScanning = Boolean.valueOf(false);
		mainWifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		wifiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		timer = new Timer();
	}


	public Boolean getIsScanning() {
		return isScanning;
	}

	public List<ScanResult> getScanResults() {
		return mainWifi.getScanResults();
	}


	public void startScan(Long interval) {
		synchronized (isScanning) {
			isScanning = true;
		}

		//enableWifi();

		if (WifiTask != null) {
			WifiTask.cancel();
			WifiTask = null;
		}

		if (timer != null) {
			timer.purge();
		}

		WifiTask = new TimerTask() {
			@Override
			public void run() {
				mainWifi.startScan();
			}
		};

		timer.schedule(WifiTask, 0, interval);

	}

	public void startScan(String samples_interval) {
		long interval = DEFAULT_INTERVAL;
		try {
			interval = Long.parseLong(samples_interval);
		} catch (NumberFormatException ex) {

		}

		startScan(interval);
	}

	public void startScan() {
		startScan(DEFAULT_INTERVAL);
	}


	public void stopScan() {

		synchronized (isScanning) {
			isScanning = false;
		}

		if (WifiTask != null) {
			WifiTask.cancel();
			WifiTask = null;
		}

		if (timer != null) {
			timer.purge();
		}
	}

	// Call startScan
	public void registerScan(WifiReceiver receiverWifi) {

		mContext.registerReceiver(receiverWifi, wifiFilter);
	}

	public void unregisterScan(WifiReceiver receiverWifi) {

		mContext.unregisterReceiver(receiverWifi);
	}

	private void enableWifi() {
		if (!mainWifi.isWifiEnabled())
			if (mainWifi.getWifiState() != WifiManager.WIFI_STATE_ENABLING)
				mainWifi.setWifiEnabled(true);
	}
void disableWifi() {
		if (mainWifi.isWifiEnabled())
			if (mainWifi.getWifiState() != WifiManager.WIFI_STATE_DISABLING)
				mainWifi.setWifiEnabled(false);
	}

}
