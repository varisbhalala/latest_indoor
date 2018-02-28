

package com.dmsl.anyplace.floor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class GroupWifiFromRadiomap {

	protected String floor;
	protected String NaN;
	
	public GroupWifiFromRadiomap() {

	}

	protected abstract void process(String maxMac, String[] macs, String line);

	protected abstract String getFloor();

	public void readFloorAlgoFromFile(InputStream in) throws IOException {
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new InputStreamReader(in));
			String line;

			line = bf.readLine();
			NaN = line.split(" ")[2];

			line = bf.readLine();
			String[] macs = line.split(", ");

			while ((line = bf.readLine()) != null) {

				String[] segs = line.split(", ");

				String maxMac = "";
				int maxRss = Integer.MIN_VALUE;

				for (int i = 3; i < segs.length; i++) {

					if (!segs[i].equals(NaN)) {

						int rss = Integer.parseInt(segs[i].split("\\.")[0]);

						if (rss > maxRss) {
							maxRss = rss;
							maxMac = macs[i];
						}
					}
				}
				
				process(maxMac, macs, line);
			}
		} finally {
			try {
				if (bf != null)
					bf.close();
			} catch (IOException e) {
			}
		}
	}

	protected void run(InputStream in,String floor) throws IOException {
		this.floor = floor;
		readFloorAlgoFromFile(in);
	}
}