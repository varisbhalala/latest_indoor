
package com.dmsl.anyplace.floor;

import org.json.JSONObject;

import android.content.Context;

import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.utils.NetworkUtils;

public class Algo2Server extends FloorSelector {

	Algo2Server(final Context myContext) {
		super(myContext);
	}

	protected String calculateFloor(Args args) throws Exception {

		JSONObject request = new JSONObject();

		JSONObject f = new JSONObject();
		f.put("MAC", args.firstMac.getBssid());
		f.put("rss", args.firstMac.getRss());

		request.put("first", f);
		request.put("dlat", args.dlat);
		request.put("dlong", args.dlong);

		String response = "";

		if (NetworkUtils.isOnline(context)) {
			response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.predictFloorAlgo2(), request.toString());

			JSONObject json = new JSONObject(response);

			if (json.getString("status").equalsIgnoreCase("error")) {
				throw new Exception("Server Error: "
						+ json.getString("message"));
			}

			return json.getString("floor");
		}else{
			return "";
		}
	}
}