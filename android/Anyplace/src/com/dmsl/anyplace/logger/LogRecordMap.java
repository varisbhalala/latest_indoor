
package com.dmsl.anyplace.logger;

import java.io.Serializable;

public class LogRecordMap implements Serializable {

	public long ts;
	public double lng;
	public double lat;
	public float heading;
	public boolean walking;
	public String bssid;
	public int rss;

	public LogRecordMap(long ts, double lat, double lng, float heading, boolean walking, String bssid, int rss) {
		super();
		this.ts = ts;
		this.lng = lng;
		this.lat = lat;
		this.heading = heading;
		this.walking = walking;
		this.bssid = bssid;
		this.rss = rss;
	}

	public String toString() {

		String str;
		str = String.valueOf(ts) + " " + String.valueOf(lat) + " " + String.valueOf(lng) + " " + String.valueOf(heading) + " " + String.valueOf(bssid) + " " + String.valueOf(rss);
		return str;
	}

}
