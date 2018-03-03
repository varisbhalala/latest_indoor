
package com.dmsl.anyplace.logger;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.dmsl.airplace.algorithms.RadioMap;
import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.R;
import com.dmsl.anyplace.AnyplaceAboutActivity;
import com.dmsl.anyplace.SelectBuildingActivity;
import com.dmsl.anyplace.cache.AnyplaceCache;
import com.dmsl.anyplace.cache.BackgroundFetchListener;
import com.dmsl.anyplace.googlemap.AnyPlaceMapTileProvider;
import com.dmsl.anyplace.googlemap.MyBuildingsRenderer;
import com.dmsl.anyplace.logger.LoggerPrefs.Action;
import com.dmsl.anyplace.logger.LoggerWiFi.Function;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper;
import com.dmsl.anyplace.nav.AnyUserData;
import com.dmsl.anyplace.nav.BuildingModel;
import com.dmsl.anyplace.nav.FloorModel;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper.SearchTypes;
import com.dmsl.anyplace.sensors.MovementDetector;
import com.dmsl.anyplace.sensors.SensorsMain;
import com.dmsl.anyplace.tasks.DeleteFolderBackgroundTask;
import com.dmsl.anyplace.tasks.DownloadRadioMapTaskBuid;
import com.dmsl.anyplace.tasks.DownloadRadioMapTaskBuid.DownloadRadioMapListener;
import com.dmsl.anyplace.tasks.FetchFloorPlanTask;
import com.dmsl.anyplace.tasks.FetchNearBuildingsTask;
import com.dmsl.anyplace.tasks.UploadRSSLogTask;
import com.dmsl.anyplace.tasks.FetchBuildingsTask.FetchBuildingsTaskListener;
import com.dmsl.anyplace.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener;
import com.dmsl.anyplace.utils.AndroidUtils;
import com.dmsl.anyplace.utils.AnyplaceUtils;
import com.dmsl.anyplace.utils.GeoPoint;
import com.dmsl.anyplace.utils.NetworkUtils;
import com.dmsl.anyplace.wifi.SimpleWifiManager;
import com.dmsl.anyplace.wifi.WifiReceiver;

import android.app.AlertDialog;
import android.app.ProgressDialog;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class AnyplaceLoggerActivity extends SherlockFragmentActivity implements OnSharedPreferenceChangeListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, OnMapClickListener {
	private static final String TAG = "AnyplaceLoggerActivity";
	public static final String SHARED_PREFS_LOGGER = "LoggerPreferences";

	// Define a request code to send to Google Play services This code is
	// returned in Activity.onActivityResult
	private final static int LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9001;
	private final static int PREFERENCES_ACTIVITY_RESULT = 1114;
	private static final int SELECT_PLACE_ACTIVITY_RESULT = 1112;

	private static final float mInitialZoomLevel = 18.0f;

	// Location API
	private LocationClient mLocationClient;
	// Define an object that holds accuracy and frequency parameters
	private LocationRequest mLocationRequest;

	private GoogleMap mMap;
	private Marker marker;
	private LatLng curLocation = null;

	// <Load Building and Marker>
	private ClusterManager<BuildingModel> mClusterManager;
	private DownloadRadioMapTaskBuid downloadRadioMapTaskBuid;
	private SearchTypes searchType = null;
	private Marker gpsMarker = null;
	private float bearing;
	private ImageButton btnTrackme;
	ProgressBar progressBar;
	private SimpleWifiManager wifi;
	private WifiReceiver receiverWifi;
	private TextView textFloor;
	private TextView scanResults;
	private ProgressDialog mSamplingProgressDialog;
	private String folder_path;
	private String filename_rss;
	private Button btnRecord;
	private TextView mTrackingInfoView = null;
	private SharedPreferences preferences;
	private SensorsMain positioning;
	private MovementDetector movementDetector;
	private float raw_heading = 0.0f;
	private boolean walking = false;

	private boolean upInProgress = false;
	private Object upInProgressLock = new Object();

	private boolean userIsNearby = false;
	private BuildingModel mCurrentBuilding = null;
	private FloorModel mCurrentFloor = null;
	private HeatmapTileProvider mProvider;
	// Logger Service
	private int mCurrentSamplesTaken = 0;
	private boolean mIsSamplingActive = false;
	private LoggerWiFi logger;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_logger);

		textFloor = (TextView) findViewById(R.id.textFloor);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		btnRecord = (Button) findViewById(R.id.recordBtn);
		btnRecord.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				btnRecordingInfo();
			}
		});

		// setup the trackme button overlaid in the map
		btnTrackme = (ImageButton) findViewById(R.id.btnTrackme);
		btnTrackme.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (gpsMarker != null) {
					AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
					mAnyplaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {

						@Override
						public void onSuccess(String result, List<BuildingModel> buildings) {
							FetchNearBuildingsTask nearest = new FetchNearBuildingsTask();
							nearest.run(buildings.iterator(), gpsMarker.getPosition().latitude, gpsMarker.getPosition().longitude, 100);

							if (nearest.buildings.size() > 0) {
								bypassSelectBuildingActivity(nearest.buildings.get(0));
							} else {
								// mMap.getCameraPosition().zoom
								mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker.getPosition(), mInitialZoomLevel));
							}
						}

						@Override
						public void onErrorOrCancel(String result) {

						}

					}, AnyplaceLoggerActivity.this, false);
				}

			}
		});

		ImageButton btnFloorUp = (ImageButton) findViewById(R.id.btnFloorUp);
		btnFloorUp.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (mCurrentBuilding == null) {
					Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
					return;
				}

				if (mIsSamplingActive) {
					Toast.makeText(getBaseContext(), "Invalid during logging.", Toast.LENGTH_LONG).show();
					return;
				}

				// Move one floor up
				int index = mCurrentBuilding.getSelectedFloorIndex();

				if (mCurrentBuilding.checkIndex(index + 1)) {
					bypassSelectBuildingActivity(mCurrentBuilding, mCurrentBuilding.getFloors().get(index + 1));
				}

			}
		});

		ImageButton btnFloorDown = (ImageButton) findViewById(R.id.btnFloorDown);
		btnFloorDown.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mCurrentBuilding == null) {
					Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
					return;
				}

				if (mIsSamplingActive) {
					Toast.makeText(getBaseContext(), "Invalid during logging.", Toast.LENGTH_LONG).show();
					return;
				}

				// Move one floor down
				int index = mCurrentBuilding.getSelectedFloorIndex();

				if (mCurrentBuilding.checkIndex(index - 1)) {
					bypassSelectBuildingActivity(mCurrentBuilding, mCurrentBuilding.getFloors().get(index - 1));
				}
			}

		});

		scanResults = (TextView) findViewById(R.id.detectedAPs);
		mTrackingInfoView = (TextView) findViewById(R.id.trackingInfoData);


		mLocationRequest = LocationRequest.create();

		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		mLocationRequest.setInterval(2000);

		mLocationRequest.setFastestInterval(1000);
		mLocationClient = new LocationClient(this, this, this);

		// get settings
		PreferenceManager.setDefaultValues(this, SHARED_PREFS_LOGGER, MODE_PRIVATE, R.xml.preferences_logger, true);
		preferences = getSharedPreferences(SHARED_PREFS_LOGGER, MODE_PRIVATE);
		preferences.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(preferences, "walk_bar");

		String folder_browser = preferences.getString("folder_browser", null);
		if (folder_browser == null) {
			File f = new File(Environment.getExternalStorageDirectory() + File.separator + getResources().getString(R.string.app_name));
			f.mkdirs();
			if (f.mkdirs() || f.isDirectory()) {
				String path = f.getAbsolutePath();
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString("folder_browser", path);
				editor.commit();
			}
		} else {
			File f = new File(folder_browser);
			f.mkdirs();
		}

		// WiFi manager to manage scans
		wifi = SimpleWifiManager.getInstance();
		// Create new receiver to get broadcasts
		receiverWifi = new SimpleWifiReceiver();
		wifi.registerScan(receiverWifi);
		wifi.startScan(preferences.getString("samples_interval", "1000"));

		positioning = new SensorsMain(this);
		movementDetector = new MovementDetector();
		positioning.addListener(movementDetector);
		positioning.addListener(new OrientationListener());
		movementDetector.addStepListener(new WalkingListener());

		AnyPlaceLoggerReceiver mSamplingAnyplaceLoggerReceiver = new AnyPlaceLoggerReceiver();
		logger = new LoggerWiFi(mSamplingAnyplaceLoggerReceiver);

		setUpMapIfNeeded();
	}




	private void setUpMapIfNeeded() {

		if (mMap != null) {
			return;
		}
		// Try to obtain the map from the SupportMapFragment.
		mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		mClusterManager = new ClusterManager<BuildingModel>(this, mMap);
		// Check if we were successful in obtaining the map.
		if (mMap != null) {
			initMap();
			// initCamera();
			initListeners();
		}

	}

	private void initMap() {
		// Sets the map type to be NORMAL - ROAD mode
		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		// mMap.setMyLocationEnabled(true); //displays a button to navigate to
		// the current user's position
	}

	private void initCamera() {
		// Only for the first time
		if (gpsMarker != null) {
			return;
		}

		Location gps = mLocationClient.getLastLocation();
		if (gps != null) {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(gps.getLatitude(), gps.getLongitude()), mInitialZoomLevel), new CancelableCallback() {

				@Override
				public void onFinish() {
					handleBuildingsOnMap();
				}

				@Override
				public void onCancel() {
					handleBuildingsOnMap();
				}
			});
		} else {
			AsyncTask<Void, Integer, Void> task = new AsyncTask<Void, Integer, Void>() {

				GeoPoint location;

				@Override
				protected Void doInBackground(Void... params) {
					try {
						location = AndroidUtils.getIPLocation();
					} catch (Exception e) {

					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {

					if (location != null && gpsMarker == null) {
						updateLocation(location);
						mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.dlat, location.dlon), mInitialZoomLevel), new CancelableCallback() {

							@Override
							public void onFinish() {
								handleBuildingsOnMap();
							}

							@Override
							public void onCancel() {
								handleBuildingsOnMap();
							}
						});
					} else {
						handleBuildingsOnMap();
					}

				}

			};

			int currentapiVersion = android.os.Build.VERSION.SDK_INT;
			if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				task.execute();
			}
		}
	}

	private void initListeners() {

		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
			@Override
			public void onCameraChange(CameraPosition position) {
				// change search box message and clear pois
				if (searchType != AnyPlaceSeachingHelper.getSearchType(position.zoom)) {
					searchType = AnyPlaceSeachingHelper.getSearchType(position.zoom);
					if (searchType == SearchTypes.INDOOR_MODE) {
						btnTrackme.setVisibility(View.INVISIBLE);
						btnRecord.setVisibility(View.VISIBLE);
						mLocationClient.removeLocationUpdates(AnyplaceLoggerActivity.this);
						if (gpsMarker != null) {
							// draw the location of the new position
							gpsMarker.remove();
						}

					} else if (searchType == SearchTypes.OUTDOOR_MODE) {
						btnTrackme.setVisibility(View.VISIBLE);
						btnRecord.setVisibility(View.INVISIBLE);
						mLocationClient.requestLocationUpdates(mLocationRequest, AnyplaceLoggerActivity.this);

					}
				}

				bearing = position.bearing;
				mClusterManager.onCameraChange(position);
			}
		});

		mMap.setOnMapClickListener(this);

		mMap.setOnMarkerDragListener(new OnMarkerDragListener() {

			@Override
			public void onMarkerDragStart(Marker arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onMarkerDragEnd(Marker arg0) {
				// TODO Auto-generated method stub
				LatLng dragPosition = arg0.getPosition();

				if (mIsSamplingActive) {
					saveRecordingToLine(dragPosition);
				}

				curLocation = dragPosition;

			}

			@Override
			public void onMarkerDrag(Marker arg0) {
				// TODO Auto-generated method stub

			}
		});

		mMap.setOnMarkerClickListener(mClusterManager);

		mClusterManager.setOnClusterItemClickListener(new OnClusterItemClickListener<BuildingModel>() {

			@Override
			public boolean onClusterItemClick(final BuildingModel b) {
				if (b != null) {
					bypassSelectBuildingActivity(b);
				}
				// Prevent Popup dialog
				return true;
			}
		});
	}

	private boolean checkReady() {
		if (mMap == null) {
			Toast.makeText(this, "Map not ready!", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	private boolean checkPlayServices() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			// Continue
			return true;
		} else {
			// Google Play services was not available for some reason

			// GooglePlayServicesUtil.getErrorDialog(resultCode, this,
			// 0).show();
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i("AnyplaceNavigator", "This device is not supported.");
				finish();
			}
			return false;
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// TODO - CHECK HOW THIS WORKS
		Log.d("Google Play Services", "Connection failed");

		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this, LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
		}

	}

	@Override
	public void onConnected(Bundle arg0) {
		// No map is loaded
		if (checkPlayServices()) {
			initCamera();
			SearchTypes type = AnyPlaceSeachingHelper.getSearchType(mMap.getCameraPosition().zoom);
			if (type == SearchTypes.INDOOR_MODE) {
				mLocationClient.removeLocationUpdates(AnyplaceLoggerActivity.this);
			} else if (type == SearchTypes.OUTDOOR_MODE) {
				mLocationClient.requestLocationUpdates(mLocationRequest, AnyplaceLoggerActivity.this);
			}
		}
	}

	@Override
	public void onLocationChanged(final Location location) {

		if (location != null) {
			GeoPoint gps;
			if (AnyplaceAPI.DEBUG_WIFI) {
				gps = AnyUserData.fakeGPS();
			} else {
				gps = new GeoPoint(location.getLatitude(), location.getLongitude());
			}

			updateLocation(gps);
		}
	}

	private void updateLocation(GeoPoint gps) {
		if (gpsMarker != null) {
			// draw the location of the new position
			gpsMarker.remove();
		}
		MarkerOptions marker = new MarkerOptions();
		marker.position(new LatLng(gps.dlat, gps.dlon));
		marker.title("User").snippet("Estimated Position");
		marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon21));
		marker.rotation(raw_heading - bearing);
		gpsMarker = this.mMap.addMarker(marker);

	}

	private void handleBuildingsOnMap() {

		AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
		mAnyplaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {

			@Override
			public void onSuccess(String result, List<BuildingModel> buildings) {
				List<BuildingModel> collection = new ArrayList<BuildingModel>(buildings);
				mClusterManager.clearItems();
				if (mCurrentBuilding != null)
					collection.remove(mCurrentBuilding);
				mClusterManager.addItems(collection);
				mClusterManager.cluster();
				// HACK. This dumps all the cached icons & recreates everything.
				mClusterManager.setRenderer(new MyBuildingsRenderer(AnyplaceLoggerActivity.this, mMap, mClusterManager));
			}

			@Override
			public void onErrorOrCancel(String result) {

			}

		}, this, false);
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	}


	private void clearMap() {
		if (!checkReady()) {
			return;
		}
		mMap.clear();
	}

	@Override
	public void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();

		if (!mIsSamplingActive) {
			positioning.pause();
		}
	}

	@Override
	public void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
		setUpMapIfNeeded();

		if (!mIsSamplingActive) {
			positioning.resume();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mLocationClient.connect();

		// Flurry Analytics
		if (AnyplaceAPI.FLURRY_ENABLE) {
			FlurryAgent.onStartSession(this, AnyplaceAPI.FLURRY_APIKEY);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		// Disconnecting the client invalidates it.
		mLocationClient.disconnect();

		// Flurry Analytics
		if (AnyplaceAPI.FLURRY_ENABLE) {
			FlurryAgent.onEndSession(this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		wifi.unregisterScan(receiverWifi);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SELECT_PLACE_ACTIVITY_RESULT:
			if (resultCode == RESULT_OK) {
				if (data == null) {
					return;
				}

				String fpf = data.getStringExtra("floor_plan_path");
				if (fpf == null) {
					Toast.makeText(getBaseContext(), "You haven't selected both building and floor...!", Toast.LENGTH_SHORT).show();
					return;
				}

				try {
					BuildingModel b = AnyplaceCache.getInstance(this).getSpinnerBuildings().get(data.getIntExtra("bmodel", 0));
					FloorModel f = b.getFloors().get(data.getIntExtra("fmodel", 0));

					bypassSelectBuildingActivity(b, f);
				} catch (Exception ex) {
					Toast.makeText(getBaseContext(), "You haven't selected both building and floor...!", Toast.LENGTH_SHORT).show();
				}
			} else if (resultCode == RESULT_CANCELED) {
				// CANCELLED
				if (data == null) {
					return;
				}
				String msg = (String) data.getSerializableExtra("message");
				if (msg != null) {
					Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
				}
			}
			break;
		case PREFERENCES_ACTIVITY_RESULT:
			if (resultCode == RESULT_OK) {
				LoggerPrefs.Action result = (Action) data.getSerializableExtra("action");

				switch (result) {
				case REFRESH_BUILDING:

					if (mCurrentBuilding == null) {
						Toast.makeText(getBaseContext(), "Load a map before performing this action!", Toast.LENGTH_SHORT).show();
						break;
					}

					if (progressBar.getVisibility() == View.VISIBLE) {
						Toast.makeText(getBaseContext(), "Building Loading in progress. Please Wait!", Toast.LENGTH_SHORT).show();
						break;
					}

					try {

						// clear_floorplans
						File floorsRoot = new File(AnyplaceUtils.getFloorPlansRootFolder(this), mCurrentBuilding.buid);
						// clear radiomaps
						File radiomapsRoot = AnyplaceUtils.getRadioMapsRootFolder(this);
						final String[] radiomaps = radiomapsRoot.list(new FilenameFilter() {

							@Override
							public boolean accept(File dir, String filename) {
								if (filename.startsWith(mCurrentBuilding.buid))
									return true;
								else
									return false;
							}
						});
						for (int i = 0; i < radiomaps.length; i++) {
							radiomaps[i] = radiomapsRoot.getAbsolutePath() + File.separator + radiomaps[i];
						}

						DeleteFolderBackgroundTask task = new DeleteFolderBackgroundTask(new DeleteFolderBackgroundTask.DeleteFolderBackgroundTaskListener() {

							@Override
							public void onSuccess() {
								bypassSelectBuildingActivity(mCurrentBuilding, mCurrentBuilding.getSelectedFloor());
							}
						}, this, true);
						task.setFiles(floorsRoot);
						task.setFiles(radiomaps);
						task.execute();
					} catch (Exception e) {
						Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				}
				break;
			}
			break;
		}
	}

	private void bypassSelectBuildingActivity(final BuildingModel b) {

		if (b != null) {

			if (mIsSamplingActive) {
				Toast.makeText(getBaseContext(), "Invalid during logging.", Toast.LENGTH_LONG).show();
				return;
			}

			// Load Building
			b.loadFloors(new FetchFloorsByBuidTaskListener() {

				@Override
				public void onSuccess(String result, List<FloorModel> floors) {

					AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
					ArrayList<BuildingModel> list = new ArrayList<BuildingModel>(1);
					list.add(b);
					mAnyplaceCache.setSelectedBuildingIndex(0);
					mAnyplaceCache.setSpinnerBuildings(list);

					FloorModel floor;
					if ((floor = b.getFloorFromNumber("0")) == null) {
						floor = b.getSelectedFloor();
					}

					bypassSelectBuildingActivity(b, floor);
				}

				@Override
				public void onErrorOrCancel(String result) {
					Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();

				}
			}, AnyplaceLoggerActivity.this, false, true);
		}
	}

	private void bypassSelectBuildingActivity(final BuildingModel b, final FloorModel f) {

		final FetchFloorPlanTask fetchFloorPlanTask = new FetchFloorPlanTask(this, b.buid, f.floor_number);

		fetchFloorPlanTask.setCallbackInterface(new FetchFloorPlanTask.FetchFloorPlanTaskListener() {

			private ProgressDialog dialog;

			@Override
			public void onSuccess(String result, File floor_plan_file) {
				if (dialog != null)
					dialog.dismiss();
				selectPlaceActivityResult(b, f);
			}

			@Override
			public void onErrorOrCancel(String result) {
				if (dialog != null)
					dialog.dismiss();
				Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onPrepareLongExecute() {
				dialog = new ProgressDialog(AnyplaceLoggerActivity.this);
				dialog.setIndeterminate(true);
				dialog.setTitle("Downloading floor plan");
				dialog.setMessage("Please be patient...");
				dialog.setCancelable(true);
				dialog.setCanceledOnTouchOutside(false);
				dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						fetchFloorPlanTask.cancel(true);
					}
				});
				dialog.show();
			}
		});
		fetchFloorPlanTask.execute();
	}

	private void loadMapBasicLayer(BuildingModel b, FloorModel f) {
		// remove the previous GroundOverlay or TileOverlay
		mMap.clear();
		TileOverlay mTileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new AnyPlaceMapTileProvider(getBaseContext(), b.buid, f.floor_number)).zIndex(0));
	}

	private void selectPlaceActivityResult(final BuildingModel b, FloorModel f) {

		// set the newly selected floor
		b.setSelectedFloor(f.floor_number);
		mCurrentBuilding = b;
		mCurrentFloor = f;
		curLocation = null;
		userIsNearby = false;
		textFloor.setText(f.floor_name);

		loadMapBasicLayer(b, f);
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(b.getPosition(), 19.0f), new CancelableCallback() {

			@Override
			public void onFinish() {
				handleBuildingsOnMap();
			}

			@Override
			public void onCancel() {
			}
		});

		class Callback implements DownloadRadioMapListener, PreviousRunningTask {
			boolean progressBarEnabled = false;
			boolean disableSuccess = false;

			@Override
			public void onSuccess(String result) {
				if (disableSuccess) {
					onErrorOrCancel("");
					return;
				}

				File root;
				try {
					root = AnyplaceUtils.getRadioMapFoler(AnyplaceLoggerActivity.this, mCurrentBuilding.buid, mCurrentFloor.floor_number);
					File f = new File(root, AnyplaceUtils.getRadioMapFileName(mCurrentFloor.floor_number));

					new HeatmapTask().execute(f);
				} catch (Exception e) {
				}

				if (AnyplaceAPI.PLAY_STORE) {

					AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
					mAnyplaceCache.fetchAllFloorsRadiomapsRun(new BackgroundFetchListener() {

						@Override
						public void onSuccess(String result) {
							hideProgressBar();
							if (AnyplaceAPI.DEBUG_MESSAGES) {
								btnTrackme.setBackgroundColor(Color.YELLOW);
							}
						}

						@Override
						public void onProgressUpdate(int progress_current, int progress_total) {
							progressBar.setProgress((int) ((float) progress_current / progress_total * progressBar.getMax()));
						}

						@Override
						public void onErrorOrCancel(String result, ErrorType error) {
							if (error == ErrorType.EXCEPTION)
								hideProgressBar();
						}

						@Override
						public void onPrepareLongExecute() {
							showProgressBar();
						}

					}, mCurrentBuilding);
				}
			}

			@Override
			public void onErrorOrCancel(String result) {
				if (progressBarEnabled) {
					hideProgressBar();
				}
			}

			@Override
			public void onPrepareLongExecute() {
				progressBarEnabled = true;
				showProgressBar();
				// Set a smaller percentage than fetchAllFloorsRadiomapsOfBUID
				progressBar.setProgress((int) (1.0f / (b.getFloors().size() * 2) * progressBar.getMax()));
			}

			@Override
			public void disableSuccess() {
				disableSuccess = true;
			}
		}

		if (downloadRadioMapTaskBuid != null) {
			((PreviousRunningTask) downloadRadioMapTaskBuid.getCallbackInterface()).disableSuccess();
		}

		downloadRadioMapTaskBuid = new DownloadRadioMapTaskBuid(new Callback(), this, b.getLatitudeString(), b.getLongitudeString(), b.buid, f.floor_number, false);

		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			downloadRadioMapTaskBuid.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			downloadRadioMapTaskBuid.execute();
		}
		showHelp("Help", "<b>1.</b> Select your floor (using arrows on the right).<br><b>2.</b> Click on the map (to identify your location).");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// prevent orientation change when auto-rotate is enabled on Android OS
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	// MENUS
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main_menu_logger, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.main_menu_upload_rsslog: {
			uploadRSSLog();
			return true;
		}
		case R.id.main_menu_loadmap: {
			// start the activity where the user can select the building
			if (mIsSamplingActive) {
				Toast.makeText(this, "Invalid during logging.", Toast.LENGTH_LONG).show();
				return true;
			}

			Location currentLocation = mLocationClient.getLastLocation();

			Intent placeIntent = new Intent(this, SelectBuildingActivity.class);
			Bundle b = new Bundle();
			if (currentLocation != null) {
				b.putString("coordinates_lat", String.valueOf(currentLocation.getLatitude()));
				b.putString("coordinates_lon", String.valueOf(currentLocation.getLongitude()));
			}

			if (mCurrentBuilding == null) {
				b.putSerializable("mode", SelectBuildingActivity.Mode.NEAREST);
			}

			placeIntent.putExtras(b);
			startActivityForResult(placeIntent, SELECT_PLACE_ACTIVITY_RESULT);
			return true;
		}
		case R.id.main_menu_clear_logging: {
			if (mCurrentBuilding == null)
				Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
			else {
				loadMapBasicLayer(mCurrentBuilding, mCurrentFloor);
				handleBuildingsOnMap();

				if (curLocation != null)
					updateMarker(curLocation);
			}
			return true;
		}

		// Launch preferences
		case R.id.main_menu_preferences: {
			Intent i = new Intent(this, LoggerPrefs.class);
			startActivityForResult(i, PREFERENCES_ACTIVITY_RESULT);
			return true;
		}
		/*case R.id.main_menu_about: {
			startActivity(new Intent(AnyplaceLoggerActivity.this, AnyplaceAboutActivity.class));
			return true;
		}

		case R.id.main_menu_exit: {
			this.finish();
			System.gc();
		}*/
		}
		return false;
	}

	private void updateMarker(LatLng latlng) {
		if (this.marker != null) {
			this.marker.remove();
		}
		this.marker = this.mMap.addMarker(new MarkerOptions().position(latlng).draggable(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
		curLocation = latlng;
	}

	// update the info view
	private void updateInfoView() {

		StringBuilder sb = new StringBuilder();
		sb.append("Lat[ ");
		if (curLocation != null)
			sb.append(curLocation.latitude);
		sb.append(" ]");
		sb.append("\nLon[ ");
		if (curLocation != null)
			sb.append(curLocation.longitude);
		sb.append(" ]");
		sb.append("\nHeading[ ");
		sb.append(String.format("%.2f", raw_heading));
		sb.append(" ]");
		sb.append("  Status[ ");
		sb.append(String.format("%8s", walking ? "Walking" : "Standing"));
		sb.append(" ]");
		sb.append("  Samples[ ");
		sb.append(mCurrentSamplesTaken);
		sb.append(" ]");
		mTrackingInfoView.setText(sb.toString());

	}

	/*
	 * Gets called whenever there is a change in sensors in positioning
	 * 
	 * @see com.lambrospetrou.anyplace.tracker.Positioning.PositioningListener#
	 * onNewPosition()
	 */

	private class OrientationListener implements SensorsMain.IOrientationListener {
		@Override
		public void onNewOrientation(float[] values) {
			raw_heading = values[0];
			updateInfoView();
		}
	}

	private class WalkingListener implements MovementDetector.MovementListener {

		@Override
		public void onWalking() {
			walking = true;
			updateInfoView();
		}

		@Override
		public void onStanding() {
			walking = false;
			updateInfoView();
		}

	}

	//
	// The receiver of the result after processing a WiFi ScanResult previously
	// by WiFiReceiver
	//
	public class AnyPlaceLoggerReceiver implements LoggerWiFi.Callback {

		public double dist(double lat1, double lon1, double lat2, double lon2) {
			double dLat;
			double dLon;

			int R = 6371; // Km
			dLat = (lat2 - lat1) * Math.PI / 180;
			dLon = (lon2 - lon1) * Math.PI / 180;
			lat1 = lat1 * Math.PI / 180;
			lat2 = lat2 * Math.PI / 180;

			double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
			double d = R * c;

			return d;

		}

		public double dist(LatLng latlng1, LatLng latlng2) {
			double lat1 = latlng1.latitude;
			double lon1 = latlng1.longitude;
			double lat2 = latlng2.latitude;
			double lon2 = latlng2.longitude;

			return dist(lat1, lon1, lat2, lon2);
		}

		private void draw(LatLng latlng, int sum) {
			CircleOptions options = new CircleOptions();
			options.center(latlng);
			options.radius(0.5 + sum * 0.05);
			options.fillColor(android.graphics.Color.BLUE);
			options.strokeWidth(3);
			// Display above floor image
			options.zIndex(2);
			mMap.addCircle(options);
		}

		@Override
		public void onFinish(LoggerWiFi logger, Function function) {
			if (function == Function.ADD) {
				runOnUiThread(new Runnable() {
					public void run() {
						updateInfoView();
					}
				});
			} else if (function == Function.SAVE) {

				final boolean exceptionOccured = logger.exceptionOccured;
				final String msg = logger.msg;
				final ArrayList<ArrayList<LogRecordMap>> mSamples = logger.mSamples;

				runOnUiThread(new Runnable() {
					@Override
					public void run() {

						if (exceptionOccured) {
							Toast.makeText(AnyplaceLoggerActivity.this, msg, Toast.LENGTH_LONG).show();
							return;
						} else {
							if (!(mSamples == null || mSamples.size() == 0)) {
								ArrayList<LogRecordMap> prevSample = mSamples.get(0);
								int sum = 0;

								for (int i = 1; i < mSamples.size(); i++) {
									ArrayList<LogRecordMap> records = mSamples.get(i);
									// double d = dist(prevSample.get(0).lat,
									// prevSample.get(0).lng,
									// records.get(0).lat, records.get(0).lng);

									if (records.get(0).walking) {
										LatLng latlng = new LatLng(prevSample.get(0).lat, prevSample.get(0).lng);
										draw(latlng, sum);
										prevSample = records;
									} else {
										if (sum < 10)
											sum += 1;
									}
								}

								LatLng latlng = new LatLng(prevSample.get(0).lat, prevSample.get(0).lng);
								draw(latlng, sum);
							}

							Toast.makeText(AnyplaceLoggerActivity.this, mSamples.size() + " Samples Recorded Successfully!", Toast.LENGTH_LONG).show();
						}

						mCurrentSamplesTaken -= mSamples.size();
						if (mSamplingProgressDialog != null) {
							mSamplingProgressDialog.dismiss();
							mSamplingProgressDialog = null;
							enableRecordButton();
							showHelp("Help", "When you are done logging, click \"Menu\" -> \"Upload\"");
						}

					}
				});

			}

		}
	}

	//
	// The WifiReceiver is responsible to Receive Access Points results
	//
	private class SimpleWifiReceiver extends WifiReceiver {

		@Override
		public void onReceive(Context c, Intent intent) {

			// Log.d("SimpleWiFi Receiver", "wifi received");

			try {
				if (intent == null || c == null || intent.getAction() == null)
					return;

				List<ScanResult> wifiList = wifi.getScanResults();
				scanResults.setText("AP : " + wifiList.size());

				// If we are not in an active sampling session we have to skip
				// this intent
				if (!mIsSamplingActive)
					return;

				if (wifiList.size() > 0) {
					mCurrentSamplesTaken++;

					logger.add(wifiList, curLocation.latitude + "," + curLocation.longitude, raw_heading, walking);
				}
			} catch (RuntimeException e) {
				Toast.makeText(c, "RuntimeException [" + e.getMessage() + "]", Toast.LENGTH_SHORT).show();
				return;
			}
		}
	}

	//
	// Method used to print pop up message to user
	//
	protected void toastPrint(String textMSG, int duration) {
		Toast.makeText(this, textMSG, duration).show();
	}

	//
	// Record button pressed. Get samples number from preferences
	//
	private void btnRecordingInfo() {

		if (mIsSamplingActive) {
			mIsSamplingActive = false;
			mSamplingProgressDialog = new ProgressDialog(AnyplaceLoggerActivity.this);
			mSamplingProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mSamplingProgressDialog.setMessage("Saving...");
			mSamplingProgressDialog.setCancelable(false);
			mSamplingProgressDialog.show();

			saveRecordingToLine(curLocation);

		} else {
			startRecordingInfo();
		}
	}

	private void startRecordingInfo() {


		if (mCurrentFloor == null || !mCurrentFloor.isFloorValid()) {
			Toast.makeText(getBaseContext(), "Load map before recording...", Toast.LENGTH_SHORT).show();
			return;
		}


		if (curLocation == null) {
			Toast.makeText(getBaseContext(), "Click a position before recording...", Toast.LENGTH_SHORT).show();
			return;
		}

		boolean hasGPS = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
		if(hasGPS) {
			if(!userIsNearby) {
				LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

				if (statusOfGPS == false) {
					Toast.makeText(this, "Please enable GPS", Toast.LENGTH_LONG).show();
					return;
				}

				final GeoPoint gps;
				if (AnyplaceAPI.DEBUG_WIFI) {
					gps = AnyUserData.fakeGPS();
				} else {
					Location location = mLocationClient.getLastLocation();
					if (location == null) {
						Toast.makeText(this, "Waiting for a valid GPS signal", Toast.LENGTH_LONG).show();
						return;
					}
					gps = new GeoPoint(location.getLatitude(), location.getLongitude());
				}

				if (GeoPoint.getDistanceBetweenPoints(mCurrentBuilding.longitude, mCurrentBuilding.latitude, gps.dlon, gps.dlat, "") > 200) {
					Toast.makeText(getBaseContext(), "You are only allowed to use the logger for a building you are currently at or physically nearby.", Toast.LENGTH_SHORT).show();
					return;
				}
				userIsNearby = true;
			}
		}

		folder_path = (String) preferences.getString("folder_browser", "n/a");
		if (folder_path.equals("n/a") || folder_path.equals("")) {
			toastPrint("Folder path not specified\nGo to Menu::Preferences::Storing Settings::Folder", Toast.LENGTH_LONG);
			return;

		} else if ((!(new File(folder_path).canWrite()))) {
			toastPrint("Folder path is not writable\nGo to Menu::Preferences::Storing Settings::Folder", Toast.LENGTH_LONG);
			return;
		}

		filename_rss = (String) preferences.getString("filename_log", "n/a");
		if (filename_rss.equals("n/a") || filename_rss.equals("")) {
			toastPrint("Filename of RSS log not specified\nGo to Menu::Preferences::Storing Settings::Filename", Toast.LENGTH_LONG);
			return;
		}

		disableRecordButton();
		// start the TASK
		mIsSamplingActive = true;
	}

	private void saveRecordingToLine(LatLng latlng) {

		logger.save(latlng.latitude + "," + latlng.longitude, folder_path, filename_rss, mCurrentFloor.floor_number, mCurrentBuilding.buid);

	}


	@Override
	public void onMapClick(LatLng latlng) {

		if (mIsSamplingActive) {
			saveRecordingToLine(latlng);
		}

		updateMarker(latlng);
		updateInfoView();

		if (!mIsSamplingActive) {
			showHelp("Help", "<b>1.</b> Please click \"START\"<br><b>2.</b> Then walk around the building in staight lines.<br><b>3.</b> Re-identify your location on the map every time you turn.");
		}

	}


	private void uploadRSSLog() {
		synchronized (upInProgressLock) {
			if (!upInProgress) {

				if (!NetworkUtils.isOnline(AnyplaceLoggerActivity.this)) {
					Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
					return;
				}

				String file_path = preferences.getString("folder_browser", "") + File.separator + preferences.getString("filename_log", "");
				startUploadTask(file_path);

			} else {
				Toast.makeText(getApplicationContext(), "Already uploading rss log...", Toast.LENGTH_SHORT).show();
			}
		}

	}

	private void startUploadTask(final String file_path) {
		upInProgress = true;

		new UploadRSSLogTask(new UploadRSSLogTask.UploadRSSLogTaskListener() {
			@Override
			public void onSuccess(String result) {
				upInProgress = false;
				File file = new File(file_path);
				file.delete();

				AlertDialog.Builder builder = new AlertDialog.Builder(AnyplaceLoggerActivity.this);
				if (mCurrentBuilding == null)
					builder.setMessage("Thank you for improving the location quality");
				else
					builder.setMessage("Thank you for improving the location quality for building " + mCurrentBuilding.name);

				builder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// do things
					}
				});
				AlertDialog alert = builder.create();
				alert.show();

			}

			@Override
			public void onErrorOrCancel(String result) {
				upInProgress = false;
				Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			}
		}, this, file_path, preferences.getString("username", ""), preferences.getString("password", "")).execute();
	}

	private void showProgressBar() {
		progressBar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		progressBar.setVisibility(View.GONE);
	}


	private void enableRecordButton() {
		btnRecord.setText("Start WiFi Recording");
		btnRecord.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_invisible, 0, 0, 0);
	}

	private void disableRecordButton() {
		btnRecord.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_online, 0, 0, 0);
		btnRecord.setText("Stop WiFi Recording");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub

		if (key.equals("walk_bar")) {
			int sensitivity = sharedPreferences.getInt("walk_bar", 26);
			int max = Integer.parseInt(getResources().getString(R.string.walk_bar_max));
			MovementDetector.setSensitivity(max - sensitivity);
		} else if (key.equals("samples_interval")) {
			wifi.startScan(sharedPreferences.getString("samples_interval", "1000"));
		}

	}

	private void showHelp(String title, String message) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		LayoutInflater adbInflater = LayoutInflater.from(this);
		View eulaLayout = adbInflater.inflate(R.layout.info_window_help, null);
		final CheckBox dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.skip);
		adb.setView(eulaLayout);
		adb.setTitle(Html.fromHtml(title));
		adb.setMessage(Html.fromHtml(message));
		adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putBoolean("skipHelpMessage", dontShowAgain.isChecked());
				editor.commit();
				return;
			}
		});

		Boolean skipMessage = preferences.getBoolean("skipHelpMessage", false);
		if (!skipMessage)
			adb.show();
	}

	private class HeatmapTask extends AsyncTask<File, Integer, Collection<WeightedLatLng>> {

		public HeatmapTask() {

		}

		@Override
		protected Collection<WeightedLatLng> doInBackground(File... params) {
			return RadioMap.readRadioMapLocations(params[0]);
		}

		@Override
		protected void onPostExecute(Collection<WeightedLatLng> result) {
			// Check if need to instantiate (avoid setData etc
			// twice)
			if (mProvider == null) {
				mProvider = new HeatmapTileProvider.Builder().weightedData(result).build();
			} else {
				mProvider.setWeightedData(result);
			}

			TileOverlay mHeapOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider).zIndex(1));
		}

	}

	interface PreviousRunningTask {
		void disableSuccess();
	}
}
