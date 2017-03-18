package tk.wasdennnoch.lockmod.tweaks.pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;
import tk.wasdennnoch.lockmod.utils.ConfigUtils;

public class MiscTweaks {

    private static boolean mClippingHooked;

    public static void setMisc(ClassLoader classLoader) {

        try {
            final boolean disableClipping = ConfigUtils.getBoolean("disable_clipping", false);
            if (!mClippingHooked) {
                XposedHelpers.findAndHookMethod(XposedHook.CLASS_KEYGUARD_PATTERN_VIEW, classLoader, "enableClipping", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (disableClipping) {
                            param.args[0] = false;
                        }
                        XposedHook.logD("MiscTweaks", "enableClipping afterHookedMethod, clipping forced to " + !disableClipping);
                    }
                });
                mClippingHooked = true;
            }
            XposedHook.logD("MiscTweaks", "Executed setMisc");
        } catch (Throwable t) {
            XposedHook.logE("MiscTweaks", "Error executing setMisc", t);
        }

    }

}
