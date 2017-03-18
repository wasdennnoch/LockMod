package tk.wasdennnoch.lockmod.utils;

import android.app.ActivityThread;
import android.content.Context;

public class ReflectionUtils {

    public static Context getSystemContext() {
        return ActivityThread.currentActivityThread().getSystemContext();
    }

}
