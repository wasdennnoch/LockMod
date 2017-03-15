package tk.wasdennnoch.lockmod;

import android.os.Build;

public class Config {

    @SuppressWarnings("PointlessBooleanExpression")
    public static final boolean DEV = true && BuildConfig.DEBUG;
    public static final boolean M = Build.VERSION.SDK_INT >= 23;

}
