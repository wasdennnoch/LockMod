package tk.wasdennnoch.lockmod.tweaks;


import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;
import tk.wasdennnoch.lockmod.tweaks.pattern.ColorTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.DimensionTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.MiscTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.PaintTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.TimingTweaks;


public class PatternTweaks {

    public static void hookKeyguardPatternConstructor(ClassLoader classLoader, final XSharedPreferences mPrefs) {
        XposedHelpers.findAndHookConstructor(XposedHook.CLASS_KEYGUARD_PATTERN_VIEW, classLoader, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (XposedHook.reload_settings)
                    mPrefs.reload(); // TODO move to general lockscreen init

                XposedHook.logD("KeyguardPatternView constructor afterHookedMethod");
                XposedHook.reload_settings = mPrefs.getBoolean("always_reload", true);
                XposedHook.debug = mPrefs.getBoolean("debug_log", false);
                XposedHook.logD("Debugging enabled");

                Context context;
                Object mAppearAnimationUtils;
                Object mDisappearAnimationUtils;
                try {
                    context = (Context) param.args[0];
                    mAppearAnimationUtils = XposedHelpers.getObjectField(param.thisObject, "mAppearAnimationUtils");
                    mDisappearAnimationUtils = XposedHelpers.getObjectField(param.thisObject, "mDisappearAnimationUtils");
                } catch (Throwable t) {
                    XposedHook.logE("Error fetching objects in constructor of KeyguardPatternView", t);
                    return;
                }

                TimingTweaks.setTimingFromConstructor(mPrefs, context, mAppearAnimationUtils, mDisappearAnimationUtils);
            }
        });
    }

    public static void hookKeyguardPatternOnFinishInflate(final ClassLoader classLoader, final XSharedPreferences mPrefs) {
        XposedHelpers.findAndHookMethod(XposedHook.CLASS_KEYGUARD_PATTERN_VIEW, classLoader, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                XposedHook.logD("KeyguardPatternView onFinishInflate afterHookedMethod");

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
                    XposedHook.logE("Error fetching objects in onFinishInflate in KeyguardPaternView", t);
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

}
