package tk.wasdennnoch.lockmod.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import java.io.File;

import tk.wasdennnoch.lockmod.R;

public class GeneralSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bare_settings);
        //noinspection ConstantConditions
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.fragment, new Fragment()).commit();
    }


    public static class Fragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //noinspection deprecation
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences_general);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case "hide_launcher_icon":
                    int mode = prefs.getBoolean("hide_launcher_icon", false) ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), "tk.wasdennnoch.lockmod.SettingsAlias"), mode, PackageManager.DONT_KILL_APP);
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            File sharedPrefsDir = new File(getActivity().getFilesDir(), "../shared_prefs");
            File sharedPrefsFile = new File(sharedPrefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
            if (sharedPrefsFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                sharedPrefsFile.setReadable(true, false);
            }
        }

    }

}
