

package com.dmsl.anyplace.cache;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import android.os.AsyncTask;

import com.dmsl.anyplace.MyApplication;
import com.dmsl.anyplace.cache.BackgroundFetchListener.ErrorType;
import com.dmsl.anyplace.cache.BackgroundFetchListener.Status;
import com.dmsl.anyplace.nav.BuildingModel;
import com.dmsl.anyplace.nav.FloorModel;
import com.dmsl.anyplace.tasks.DownloadRadioMapTaskBuid;
import com.dmsl.anyplace.tasks.FetchFloorPlanTask;
import com.dmsl.anyplace.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener;

@SuppressWarnings("serial")
class BackgroundFetch implements Serializable, Runnable {

	private BackgroundFetchListener l;
	public BuildingModel build = null;

	public Status status = Status.RUNNING;
	private ErrorType error = ErrorType.EXCEPTION;

	private int progress_total = 0;
	private int progress_current = 0;

	private AsyncTask<Void, Void, String> currentTask;

	BackgroundFetch(BackgroundFetchListener l, BuildingModel build) {
		this.l = l;
		this.build = build;
	}

	@Override
	public void run() {
		fetchFloors();		
	}
	
	// Fetch Building Floors Details
	private void fetchFloors() {
		if (build.isFloorsLoaded() == false) {
			build.loadFloors(new FetchFloorsByBuidTaskListener() {

				@Override
				public void onSuccess(String result, List<FloorModel> floors) {
					progress_total = build.getFloors().size() * 2;
					fetchAllFloorPlans(0);
				}

				@Override
				public void onErrorOrCancel(String result) {
					status = Status.STOPPED;
					l.onErrorOrCancel(result, error);

				}
			}, MyApplication.getAppContext(), false, false);

		} else {
			progress_total = build.getFloors().size() * 2;
			fetchAllFloorPlans(0);
		}
	}

	// Fetch Floor Maps
	private void fetchAllFloorPlans(final int index) {

		if (build.isFloorsLoaded()) {
			if (index < build.getFloors().size()) {
				FloorModel f = build.getFloors().get(index);

				currentTask = new FetchFloorPlanTask(MyApplication.getAppContext(), build.buid, f.floor_number);
				((FetchFloorPlanTask) currentTask).setCallbackInterface(new FetchFloorPlanTask.FetchFloorPlanTaskListener() {

					@Override
					public void onSuccess(String result, File floor_plan_file) {
						l.onProgressUpdate(++progress_current, progress_total);
						fetchAllFloorPlans(index + 1);
					}

					@Override
					public void onErrorOrCancel(String result) {
						status = Status.STOPPED;
						l.onErrorOrCancel(result, error);
					}

					@Override
					public void onPrepareLongExecute() {
						// TODO Auto-generated method stub

					}
				});
				currentTask.execute();

			} else {
				fetchAllRadioMaps(0);
			}
		} else {
			status = Status.STOPPED;
			l.onErrorOrCancel("Fetch Floor Plans Error", error);
		}

	}

	// fetch All Radio Maps except from current floor(floor_number)
	private void fetchAllRadioMaps(final int index) {

		if (build.getFloors() != null) {
			if (index < build.getFloors().size()) {
				FloorModel f = build.getFloors().get(index);

				AsyncTask<Void, Void, String> task = new DownloadRadioMapTaskBuid(new DownloadRadioMapTaskBuid.DownloadRadioMapListener() {

					@Override
					public void onSuccess(String result) {
						l.onProgressUpdate(progress_current++, progress_total);
						fetchAllRadioMaps(index + 1);
					}

					@Override
					public void onErrorOrCancel(String result) {
						status = Status.STOPPED;
						l.onErrorOrCancel(result, BackgroundFetchListener.ErrorType.EXCEPTION);
					}

					@Override
					public void onPrepareLongExecute() {
						// TODO Auto-generated method stub

					}

				}, MyApplication.getAppContext(), build.getLatitudeString(), build.getLongitudeString(), build.buid, f.floor_number, false);

				int currentapiVersion = android.os.Build.VERSION.SDK_INT;
				if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
					// Execute task parallel with others
					currentTask = task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					currentTask = task.execute();
				}

			} else {
				status = Status.SUCCESS;
				l.onSuccess("Finished loading building");
			}
		} else {
			l.onErrorOrCancel("Fetch Floor Plans Error", error);
		}
	}

	public void cancel() {
		error = BackgroundFetchListener.ErrorType.CANCELLED;
		if (currentTask != null) {
			currentTask.cancel(true);
		}
	}

	
}
