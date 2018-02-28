

package com.dmsl.anyplace.logger;

import com.dmsl.anyplace.AndroidFileBrowser;
import com.dmsl.anyplace.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore.MediaColumns;

public class LoggerPrefs extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final int SELECT_IMAGE = 7;
	private static final int SELECT_PATH = 8;

	public enum Action {
		REFRESH_BUILDING
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(AnyplaceLoggerActivity.SHARED_PREFS_LOGGER);

		addPreferencesFromResource(R.xml.preferences_logger);

		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		// getPreferenceManager().findPreference("image_custom").setOnPreferenceClickListener(new
		// OnPreferenceClickListener() {
		//
		// @Override
		// public boolean onPreferenceClick(Preference preference) {
		// Intent i = new Intent(Intent.ACTION_PICK,
		// android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		// i.setType("image/*");
		// startActivityForResult(i, SELECT_IMAGE);
		// return true;
		// }
		// });
/*

		getPreferenceManager().findPreference("folder_browser").setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				Intent i = new Intent(getBaseContext(), AndroidFileBrowser.class);

				Bundle extras = new Bundle();
				extras.putBoolean("selectFolder", true);
				SharedPreferences preferences = getSharedPreferences("LoggerPreferences", MODE_PRIVATE);
				extras.putString("defaultPath", preferences.getString("folder_browser", ""));
				i.putExtras(extras);

				startActivityForResult(i, SELECT_PATH);
				return true;
			}
		});
		
*/

		getPreferenceManager().findPreference("refresh_building").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("action", Action.REFRESH_BUILDING);
				setResult(RESULT_OK, returnIntent);
				finish();
				return true;
			}
		});


	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		SharedPreferences customSharedPreference;

		customSharedPreference = getSharedPreferences("LoggerPreferences", MODE_PRIVATE);

		switch (requestCode) {

		case SELECT_IMAGE:
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedImage = data.getData();
				String RealPath;
				SharedPreferences.Editor editor = customSharedPreference.edit();
				RealPath = getRealPathFromURI(selectedImage);
				editor.putString("image_custom", RealPath);
				editor.commit();
			}
			break;
		case SELECT_PATH:
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedFolder = data.getData();
				String path = selectedFolder.toString();
				SharedPreferences.Editor editor = customSharedPreference.edit();
				editor.putString("folder_browser", path);
				editor.commit();
			}
			break;
		}
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaColumns.DATA };
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onDestroy() {
		// Unregister the listener whenever a key changes
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

	}

}