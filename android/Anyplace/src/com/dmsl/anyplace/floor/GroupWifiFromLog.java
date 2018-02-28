
package com.dmsl.anyplace.floor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public abstract class GroupWifiFromLog {

	public GroupWifiFromLog() {

	}

	protected abstract void process(String maxMac, ArrayList<String> values);

	protected abstract String getFloor();

	private void calculateMaxWifi(ArrayList<String> values) {
		if (values.size() > 0) {
			String maxMac = "";
			int maxRss = Integer.MIN_VALUE;

			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				String[] segs = value.split(" ");
				try {
					String mac = segs[4];
					int rss = Integer.parseInt(segs[5]);

					if (rss > maxRss) {
						maxRss = rss;
						maxMac = mac;
					}
				} catch (NumberFormatException ex) {
					if (values.remove(value))
						i--;

				}
			}

			process(maxMac, values);

		}
	}

	private void readFloorAlgoFromFile(InputStream in) throws IOException {
		String line;
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new InputStreamReader(in));

			ArrayList<String> values = new ArrayList<String>(10);

			while ((line = bf.readLine()) != null) {
				if (line.startsWith("# Timestamp")) {
					calculateMaxWifi(values);
					values.clear();
				} else {
					values.add(line);
				}
			}

			calculateMaxWifi(values);

		} finally {
			try {
				if (bf != null)
					bf.close();
			} catch (IOException e) {

			}
		}
	}

	public void run(InputStream in) throws IOException {
		readFloorAlgoFromFile(in);
	}
}
