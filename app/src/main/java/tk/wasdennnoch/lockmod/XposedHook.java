package tk.wasdennnoch.lockmod;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.lockmod.tweaks.PatternTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.DevTweaks;
import tk.wasdennnoch.lockmod.utils.ConfigUtils;
import tk.wasdennnoch.lockmod.utils.ReflectionUtils;

public class XposedHook implements IXposedHookLoadPackage {

    public static boolean debug = false;

    public static final String CLASS_LOCK_PATTERN_VIEW = "com.android.internal.widget.LockPatternView";
    public static final String CLASS_KEYGUARD_PATTERN_VIEW = "com.android.keyguard.KeyguardPatternView";
    public static final String CLASS_KEYGUARD_UNLOCK_PATTERN_LISTENER = CLASS_KEYGUARD_PATTERN_VIEW + "$UnlockPatternListener";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("tk.wasdennnoch.lockmod")) {
            XposedHelpers.findAndHookMethod("tk.wasdennnoch.lockmod.activities.SettingsActivity", lpparam.classLoader, "isEnabled", XC_MethodReplacement.returnConstant(true));
        } else if (lpparam.packageName.equals("android")) {
            if (ConfigUtils.DEV) {
                DevTweaks.devInitAndroid(lpparam.classLoader);
            }
        } else if (lpparam.packageName.equals("com.android.systemui")) {
            ConfigUtils.init(ReflectionUtils.getSystemContext());
            if (!ConfigUtils.getBoolean("can_read_prefs", false))
                logW("XposedHook", "Preferences inaccessible via provider");
            if (ConfigUtils.getBoolean("active_pattern_tweaks", false)) {
                PatternTweaks.hookKeyguardPatternConstructor(lpparam.classLoader);
                PatternTweaks.hookKeyguardPatternOnFinishInflate(lpparam.classLoader);
            }
            //noinspection ConstantConditions,ConstantIfStatement
            if (ConfigUtils.DEV) {
                DevTweaks.devInitSysUI(lpparam.classLoader);
            }
        }
    }

    public static void logE(String tag, String msg, Throwable t) {
        XposedBridge.log("[LockMod] [ERROR]" + tag + ": " + msg);
        if (t != null)
            XposedBridge.log(t);
    }

    public static void logI(String tag, String msg) {
        XposedBridge.log("[LockMod] [INFO]" + tag + ": " + msg);
    }

    public static void logW(String tag, String msg) {
        XposedBridge.log("[LockMod] [WARNING]" + tag + ": " + msg);
    }

    public static void logD(String tag, String msg) {
        if (debug) XposedBridge.log("[LockMod] [DEBUG]" + tag + ": " + msg);
    }

}
