package com.corn3lius.android.HexWallpaper;

import com.corn3lius.android.HexWallpaper.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity; 
import android.util.Log;

public class hexWallpaperSettings extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getPreferenceManager().setSharedPreferencesName(
				HexWallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.hexsettings);
		getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.i("sharedPref","here with " + key);
	}
}
//
//First, your preference activity should implement
//> ColorPickerDialog.OnColorChangedListener
//> Then add a PreferenceScreen and register setOnPreferenceClickListener to a
//> function where you will do a new ColorPickerDialog().show().
//> Finally, implement your colorChanged function where you will use your
//> preference editor to commit the new color.