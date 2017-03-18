package tk.wasdennnoch.lockmod.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.crossbowffs.remotepreferences.RemotePreferences;

import tk.wasdennnoch.lockmod.BuildConfig;

public class ConfigUtils {

    @SuppressWarnings("PointlessBooleanExpression")
    public static final boolean DEV = true && BuildConfig.DEBUG;
    public static final boolean M = Build.VERSION.SDK_INT >= 23;

    private static SharedPreferences sPrefs;

    public static void init(Context context) {
        sPrefs = new RemotePreferences(context, "tk.wasdennnoch.lockmod.PREFERENCES", "tk.wasdennnoch.lockmod_preferences");
    }

    public static boolean getBoolean(String key, boolean def) {
        return sPrefs.getBoolean(key, def);
    }

    public static int getInt(String key, int def) {
        return sPrefs.getInt(key, def);
    }

    public static float getFloat(String key, float def) {
        return sPrefs.getFloat(key, def);
    }

    public static String getString(String key, String def) {
        return sPrefs.getString(key, def);
    }

}
