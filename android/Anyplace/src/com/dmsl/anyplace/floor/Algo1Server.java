
package com.dmsl.anyplace.floor;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.dmsl.airplace.algorithms.LogRecord;
import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.utils.NetworkUtils;

public class Algo1Server extends FloorSelector {

	public Algo1Server(final Context myContext) {
		super(myContext);
	}

	protected String calculateFloor(Args args) throws Exception {

		JSONObject request = new JSONObject();

		JSONObject f = new JSONObject();
		f.put("MAC", args.firstMac.getBssid());
		f.put("rss", args.firstMac.getRss());
		request.put("first", f);

		if (args.secondMac != null) {
			JSONObject s = new JSONObject();
			s.put("MAC", args.secondMac.getBssid());
			s.put("rss", args.secondMac.getRss());
			request.put("second", s);
		}

		JSONArray jsonArray = new JSONArray();
		for (LogRecord wifi : args.latestScanList) {
			JSONObject js = new JSONObject();
			js.put("MAC", wifi.getBssid());
			js.put("rss", wifi.getRss());
			jsonArray.put(js);
		}

		request.put("wifi", jsonArray);
		request.put("dlat", args.dlat);
		request.put("dlong", args.dlong);

		String response = "";

		if (NetworkUtils.isOnline(context)) {
			response = NetworkUtils.downloadHttpClientJsonPost(
					AnyplaceAPI.predictFloorAlgo1(), request.toString());

			JSONObject json = new JSONObject(response);

			if (json.getString("status").equalsIgnoreCase("error")) {
					throw new Exception("Server Error: "
							+ json.getString("message"));
			}

			return json.getString("floor");
		} else {
			return "";
		}

	}
}
