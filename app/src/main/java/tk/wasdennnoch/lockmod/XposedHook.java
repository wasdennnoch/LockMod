package tk.wasdennnoch.lockmod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerPolicy;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tk.wasdennnoch.lockmod.tweaks.ColorTweaks;
import tk.wasdennnoch.lockmod.tweaks.DevTweaks;
import tk.wasdennnoch.lockmod.tweaks.DimensionTweaks;
import tk.wasdennnoch.lockmod.tweaks.MiscTweaks;
import tk.wasdennnoch.lockmod.tweaks.PaintTweaks;
import tk.wasdennnoch.lockmod.tweaks.TimingTweaks;

public class XposedHook implements IXposedHookLoadPackage {

    private static boolean debug = false;
    private static boolean reload_settings = true;

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
                Class<?> classPhoneWindowManager = XposedHelpers.findClass(Config.M ? "com.android.server.policy.PhoneWindowManager" : "com.android.internal.policy.impl.PhoneWindowManager", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init", Context.class, IWindowManager.class, WindowManagerPolicy.WindowManagerFuncs.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object pwm = param.thisObject;
                        Context c = (Context) XposedHelpers.getObjectField(pwm, "mContext");
                        c.registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                throw new RuntimeException("Shhhh... Sleep...");
                            }
                        }, new IntentFilter("reboot"));
                    }
                });
            }
        } else if (lpparam.packageName.equals("com.android.systemui")) {

            mPrefs.reload();
            if (mPrefs.getBoolean("active_pattern_tweaks", false)) {
                hookKeyguardPatternConstructor(lpparam.classLoader);
                hookKeyguardPatternOnFinishInflate(lpparam.classLoader);
            }
            //noinspection ConstantConditions,ConstantIfStatement
            if (Config.DEV) {
                DevTweaks.setDevInit(lpparam.classLoader);
            }

        }
    }

    private void hookKeyguardPatternConstructor(ClassLoader classLoader) {
        XposedHelpers.findAndHookConstructor(CLASS_KEYGUARD_PATTERN_VIEW, classLoader, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (reload_settings)
                    mPrefs.reload();

                logD("constructor afterHookedMethod");
                reload_settings = mPrefs.getBoolean("always_reload", true);
                debug = mPrefs.getBoolean("debug_log", false);
                logD("Debugging enabled");

                Context context;
                Object mAppearAnimationUtils;
                Object mDisappearAnimationUtils;
                try {
                    context = (Context) param.args[0];
                    mAppearAnimationUtils = XposedHelpers.getObjectField(param.thisObject, "mAppearAnimationUtils");
                    mDisappearAnimationUtils = XposedHelpers.getObjectField(param.thisObject, "mDisappearAnimationUtils");
                } catch (Throwable t) {
                    logE("Error fetching objects in constructor of KeyguardPatternView", t);
                    return;
                }

                TimingTweaks.setTimingFromConstructor(mPrefs, context, mAppearAnimationUtils, mDisappearAnimationUtils);
            }
        });
    }

    private void hookKeyguardPatternOnFinishInflate(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(CLASS_KEYGUARD_PATTERN_VIEW, classLoader, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                logD("onFinishInflate afterHookedMethod");

                final View mLockPatternView;
                Paint mPaint;
                Paint mPathPaint;
                Object[][] mCellStates;
                final Runnable mCancelPatternRunnable;
                try {
                    mLockPatternView = (View) XposedHelpers.getObjectField(param.thisObject, "mLockPatternView");
                    mPaint = (Paint) XposedHelpers.getObjectField(mLockPatternView, "mPaint");
                    mPathPaint = (Paint) XposedHelpers.getObjectField(mLockPatternView, "mPathPaint");
                    mCellStates = (Object[][]) XposedHelpers.getObjectField(mLockPatternView, "mCellStates");
                    mCancelPatternRunnable = (Runnable) XposedHelpers.getObjectField(param.thisObject, "mCancelPatternRunnable");
                } catch (Throwable t) {
                    logE("Error fetching objects in onFinishInflate in KeyguardPaternView", t);
                    return;
                }

                ColorTweaks.setColors(mPrefs, classLoader, mLockPatternView);
                DimensionTweaks.setDimensions(mPrefs, mLockPatternView, mCellStates, mPathPaint);
                PaintTweaks.setStroke(mPrefs, mPaint, mPathPaint);
                PaintTweaks.setBlurring(mPrefs, mLockPatternView, mPaint, mPathPaint);
                TimingTweaks.setTiming(mPrefs, classLoader, mLockPatternView, mCancelPatternRunnable);
                PaintTweaks.setShader(mPrefs, mLockPatternView, mPaint, mPathPaint);
                MiscTweaks.setMisc(mPrefs, classLoader);
            }
        });
    }

    public static void logE(String msg, Throwable t) {
        log("[FATAL ERROR] " + msg);
        if (t != null)
            XposedBridge.log(t);
    }

    public static void log(String msg) {
        XposedBridge.log("[LockMod] " + msg);
    }

    public static void logD(String msg) {
        if (debug) log("[DEBUG] " + msg);
    }

}
