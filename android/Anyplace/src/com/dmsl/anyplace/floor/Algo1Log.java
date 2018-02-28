

package com.dmsl.anyplace.floor;

import java.util.ArrayList;
import java.util.HashMap;

import com.dmsl.airplace.algorithms.LogRecord;
import com.dmsl.anyplace.utils.GeoPoint;

import android.content.Context;

public class Algo1Log extends FloorSelector {

	public Algo1Log(final Context myContext) {
		super(myContext);
	}

	protected String calculateFloor(Args args) throws Exception {

		GroupWifiFromLog algo1 = new Algo1Help( args);
		algo1.run(context.getAssets().open("rssHTC"));

		return algo1.getFloor();
	}

	private static class Algo1Help extends GroupWifiFromLog {

		final double a = 10;
		final double b = 10;
		final int l1 = 10;

		HashMap<String, Wifi> input = new HashMap<String, Wifi>();
		ArrayList<Score> mostSimilar = new ArrayList<Score>(10);

		private GeoPoint bbox[] = null;
		private Args args;

		public Algo1Help( Args args) {
			super();

			this.args = args;
			if (!(args.dlat == 0 || args.dlong == 0)) {
				bbox = GeoPoint.getGeoBoundingBox(args.dlat, args.dlong, 100);
			}

			for (LogRecord listenObject : args.latestScanList) {
				input.put(listenObject.getBssid(), new Wifi(listenObject.getBssid(), listenObject.getRss()));
			}
		}

		private double compare(ArrayList<String> bucket) {

			// # Timestamp, X, Y, HEADING, MAC Address of AP, RSS, FLOOR

			long score = 0;
			int nNCM = 0;
			int nCM = 0;

			for (String wifiDatabase : bucket) {
				String[] segs = wifiDatabase.split(" ");

				String mac = segs[4];

				if (input.containsKey(mac)) {
					Integer diff = (Integer.parseInt(segs[5]) - input.get(mac).rss);
					score += diff * diff;

					nCM++;
				} else {
					nNCM++;
				}
			}

			return Math.sqrt(score) - a * nCM + b * nNCM;
		}

		private void checkScore(double similarity, String floor) {

			if (mostSimilar.size() == 0) {
				mostSimilar.add(new Score(similarity, floor));
				return;
			}

			for (int i = 0; i < mostSimilar.size(); i++) {
				if (mostSimilar.get(i).similarity > similarity) {
					mostSimilar.add(i, new Score(similarity, floor));
					if (mostSimilar.size() > l1) {
						mostSimilar.remove(mostSimilar.size() - 1);
					}
					return;
				}
			}

			if (mostSimilar.size() < l1) {
				mostSimilar.add(new Score(similarity, floor));
			}

		}

		public String getFloor() {
			// Floor -Score
			HashMap<String, Integer> sum_floor_score = new HashMap<String, Integer>();

			for (Score s : mostSimilar) {
				Integer score = 1;
				if (sum_floor_score.containsKey(s.floor)) {
					score = sum_floor_score.get(s.floor) + 1;
				}

				sum_floor_score.put(s.floor, score);
			}

			String max_floor = "";
			int max_score = 0;

			for (String floor : sum_floor_score.keySet()) {
				int score = sum_floor_score.get(floor);
				if (max_score < score) {
					max_score = score;
					max_floor = floor;
				}
			}

			return max_floor;

		}

		protected void process(String maxMac, ArrayList<String> values) {

			// # Timestamp, X, Y, HEADING, MAC Address of AP, RSS, FLOOR

			if (maxMac.equals(args.firstMac.getBssid()) || (args.secondMac != null && maxMac.equals(args.secondMac.getBssid()))) {

				String[] segs = values.get(0).split(" ");
				double x = Double.parseDouble(segs[1]);
				double y = Double.parseDouble(segs[2]);
				if (bbox == null || (x > bbox[0].dlat && x < bbox[1].dlat && y > bbox[0].dlon && y < bbox[1].dlon)) {

					double similarity = compare(values);
					checkScore(similarity, segs[6]);
				}
			}

		}

		private class Score {

			double similarity;
			String floor;

			Score(double similarity, String floor) {
				this.similarity = similarity;
				this.floor = floor;
			}
		}

		private class Wifi {
			String mac;
			Integer rss;

			Wifi(String mac, Integer rss) {
				this.mac = mac;
				this.rss = rss;
			}
		}
	}
}
