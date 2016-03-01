package tk.wasdennnoch.lockmod.tweaks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;

public class MiscTweaks {

    private static boolean mClippingHooked;

    public static void setMisc(XSharedPreferences prefs, ClassLoader classLoader) {

        try {
            final boolean disableClipping = prefs.getBoolean("disable_clipping", false);
            if (!mClippingHooked) {
                XposedHelpers.findAndHookMethod(XposedHook.CLASS_KEYGUARD_PATTERN_VIEW, classLoader, "enableClipping", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (disableClipping) {
                            param.args[0] = false;
                        }
                        XposedHook.logD("enableClipping afterHookedMethod, clipping forced to " + !disableClipping);
                    }
                });
                mClippingHooked = true;
            }
            XposedHook.logD("Executed setMisc");
        } catch (Throwable t) {
            XposedHook.logE("Error executing setMisc", t);
        }

    }

}
