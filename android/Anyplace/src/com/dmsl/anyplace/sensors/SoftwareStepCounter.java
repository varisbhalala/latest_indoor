

package com.dmsl.anyplace.sensors;

public class SoftwareStepCounter {

	private final float mYOffset = 480 * 0.5f;
	private final float mLimit = (float) 1.97;

	private float mLastValues;
	private float mLastDirections;
	private float mLastExtremes[] = new float[2];
	private float mLastDiff;
	private int mLastMatch = -1;

	public int steps = 0;

	public boolean checkIfStep(float[] values) {

		boolean step = false;
		float vSum = values[0] + values[1] + values[2];

		float v = mYOffset - vSum * 4 / 3;

		float direction = (v > mLastValues ? 1 : (v < mLastValues ? -1 : 0));
		if (direction == -mLastDirections) {
			// Direction changed
			int extType = (direction > 0 ? 0 : 1); // minimum or maximum?
			mLastExtremes[extType] = mLastValues;
			float diff = Math.abs(mLastExtremes[extType] - mLastExtremes[1 - extType]);

			if (diff > mLimit) {

				boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff * 2 / 3);
				boolean isPreviousLargeEnough = mLastDiff > (diff / 3);
				boolean isNotContra = (mLastMatch != 1 - extType);

				if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
					steps++;
					step = true;
					mLastMatch = extType;
					// }
				} else {
					// Log.i(TAG, "no step");
					mLastMatch = -1;
				}
			}
			mLastDiff = diff;
		}
		mLastDirections = direction;
		mLastValues = v;

		return step;
	}
}
