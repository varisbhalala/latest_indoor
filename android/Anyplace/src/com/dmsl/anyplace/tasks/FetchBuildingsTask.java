
package com.dmsl.anyplace.tasks;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.nav.BuildingModel;
import com.dmsl.anyplace.utils.NetworkUtils;

public class FetchBuildingsTask extends AsyncTask<Void, Void, String> {

	public interface FetchBuildingsTaskListener {
		void onErrorOrCancel(String result);

		void onSuccess(String result, List<BuildingModel> buildings);
	}

	private FetchBuildingsTaskListener mListener;
	private Context ctx;

	private List<BuildingModel> buildings = new ArrayList<BuildingModel>();
	private boolean success = false;
	private ProgressDialog dialog;
	private Boolean showDialog = true;

	public FetchBuildingsTask(FetchBuildingsTaskListener fetchBuildingsTaskListener, Context ctx) {
		this.mListener = fetchBuildingsTaskListener;
		this.ctx = ctx;
	}

	public FetchBuildingsTask(FetchBuildingsTaskListener fetchBuildingsTaskListener, Context ctx, Boolean showDialog) {
		this.mListener = fetchBuildingsTaskListener;
		this.ctx = ctx;
		this.showDialog = showDialog;
	}

	@Override
	protected void onPreExecute() {
		if (showDialog) {
			dialog = new ProgressDialog(ctx);
			dialog.setIndeterminate(true);
			dialog.setTitle("Fetching Buildings");
			dialog.setMessage("Please be patient...");
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					FetchBuildingsTask.this.cancel(true);
				}
			});
			dialog.show();
		}

	}

	@Override
	protected String doInBackground(Void... params) {

		if (!NetworkUtils.isOnline(ctx)) {
			return "No connection available!";
		}

		try {

			JSONObject j = new JSONObject();
			try {
				j.put("username", "username");
				j.put("password", "pass");

			} catch (JSONException e) {
				return "Error requesting the buildings around you!";
			}

			String response = null;

			//Uses GZIP encoding
			response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.getFetchBuildingsUrl(), j.toString());

			JSONObject json = new JSONObject(response);

			// Missing in Zip Format
			if (json.has("status") && json.getString("status").equalsIgnoreCase("error")) {
				return "Error Message: " + json.getString("message");
			}

			// process the buildings received
			BuildingModel b;
			JSONArray buids_json = new JSONArray(json.getString("buildings"));
			for (int i = 0, sz = buids_json.length(); i < sz; i++) {
				JSONObject cp = (JSONObject) buids_json.get(i);
				b = new BuildingModel();
				b.setPosition(cp.getString("coordinates_lat"), cp.getString("coordinates_lon"));
				b.buid = cp.getString("buid");

				b.name = cp.getString("name");
				// b.url = cp.getString("url");

				buildings.add(b); // the anyplace Cache list
			}

			Collections.sort(buildings);

			success = true;
			return "Successfully fetched buildings";

		} catch (ConnectTimeoutException e) {
			return "Cannot connect to Anyplace service!";
		} catch (SocketTimeoutException e) {
			return "Communication with the server is taking too long!";
		} catch (UnknownHostException e) {
			return "No connection available!";
		} catch (Exception e) {
			return "Error fetching buildings. [ " + e.getMessage() + " ]";
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (showDialog)
			dialog.dismiss();

		if (success) {
			mListener.onSuccess(result, buildings);
		} else {
			// there was an error during the process
			mListener.onErrorOrCancel(result);
		}

	}

	@Override
	protected void onCancelled(String result) {
		if (showDialog)
			dialog.dismiss();
		mListener.onErrorOrCancel("Buildings Fetch cancelled...");
	}

	@Override
	protected void onCancelled() { // just for < API 11
		if (showDialog)
			dialog.dismiss();
		mListener.onErrorOrCancel("Buildings Fetch cancelled...");
	}

}
