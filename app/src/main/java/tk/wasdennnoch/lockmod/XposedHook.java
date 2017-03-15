package tk.wasdennnoch.lockmod;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.lockmod.tweaks.PatternTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.DevTweaks;

public class XposedHook implements IXposedHookLoadPackage {

    public static boolean debug = false;
    public static boolean reload_settings = true;

    private XSharedPreferences mPrefs = new XSharedPreferences("tk.wasdennnoch.lockmod");

    public static final String CLASS_LOCK_PATTERN_VIEW = "com.android.internal.widget.LockPatternView";
    public static final String CLASS_KEYGUARD_PATTERN_VIEW = "com.android.keyguard.KeyguardPatternView";
    public static final String CLASS_KEYGUARD_UNLOCK_PATTERN_LISTENER = CLASS_KEYGUARD_PATTERN_VIEW + "$UnlockPatternListener";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("tk.wasdennnoch.lockmod")) {
            XposedHelpers.findAndHookMethod("tk.wasdennnoch.lockmod.activities.SettingsActivity", lpparam.classLoader, "isEnabled", XC_MethodReplacement.returnConstant(true));
        } else if (lpparam.packageName.equals("android")) {
            if (Config.DEV) {
                DevTweaks.devInitAndroid(lpparam.classLoader);
            }
        } else if (lpparam.packageName.equals("com.android.systemui")) {
            mPrefs.reload();
            if (mPrefs.getBoolean("active_pattern_tweaks", false)) {
                PatternTweaks.hookKeyguardPatternConstructor(lpparam.classLoader, mPrefs);
                PatternTweaks.hookKeyguardPatternOnFinishInflate(lpparam.classLoader, mPrefs);
            }
            //noinspection ConstantConditions,ConstantIfStatement
            if (Config.DEV) {
                DevTweaks.devInitSysUI(lpparam.classLoader);
            }
        }
    }

    public static void logE(String msg, Throwable t) {
        XposedBridge.log("[LockMod] [ERROR]" + msg);
        if (t != null)
            XposedBridge.log(t);
    }

    public static void logI(String msg) {
        XposedBridge.log("[LockMod] [INFO]" + msg);
    }

    public static void logW(String msg) {
        XposedBridge.log("[LockMod] [WARNING]" + msg);
    }

    public static void logD(String msg) {
        if (debug) XposedBridge.log("[LockMod] [DEBUG]" + msg);
    }

}
