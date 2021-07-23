package com.example.yink.amadeus;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

/**
 * Created by Yink on 05.03.2017.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
