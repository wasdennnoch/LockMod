package tk.wasdennnoch.lockmod.tweaks;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;
import tk.wasdennnoch.lockmod.tweaks.pattern.ColorTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.DimensionTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.MiscTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.PaintTweaks;
import tk.wasdennnoch.lockmod.tweaks.pattern.TimingTweaks;
import tk.wasdennnoch.lockmod.utils.ConfigUtils;


public class PatternTweaks {

    public static void hookKeyguardPatternConstructor(ClassLoader classLoader) {
        XposedHelpers.findAndHookConstructor(XposedHook.CLASS_KEYGUARD_PATTERN_VIEW, classLoader, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                XposedHook.logD("PatternTweaks", "KeyguardPatternView constructor afterHookedMethod");
                XposedHook.reload_settings = ConfigUtils.getBoolean("always_reload", true);
                XposedHook.debug = ConfigUtils.getBoolean("debug_log", false);

                Context context;
                Object mAppearAnimationUtils;
                Object mDisappearAnimationUtils;
                try {
                    context = (Context) param.args[0];
                    mAppearAnimationUtils = XposedHelpers.getObjectField(param.thisObject, "mAppearAnimationUtils");
                    mDisappearAnimationUtils = XposedHelpers.getObjectField(param.thisObject, "mDisappearAnimationUtils");
                } catch (Throwable t) {
                    XposedHook.logE("PatternTweaks", "Error fetching objects in constructor of KeyguardPatternView", t);
                    return;
                }

                TimingTweaks.setTimingFromConstructor(context, mAppearAnimationUtils, mDisappearAnimationUtils);
            }
        });
    }

    public static void hookKeyguardPatternOnFinishInflate(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(XposedHook.CLASS_KEYGUARD_PATTERN_VIEW, classLoader, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                XposedHook.logD("PatternTweaks", "KeyguardPatternView onFinishInflate afterHookedMethod");

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
                    XposedHook.logE("PatternTweaks", "Error fetching objects in onFinishInflate in KeyguardPaternView", t);
                    return;
                }

                ColorTweaks.setColors(classLoader, mLockPatternView);
                DimensionTweaks.setDimensions(mLockPatternView, mCellStates, mPathPaint);
                PaintTweaks.setStroke(mPaint, mPathPaint);
                PaintTweaks.setBlurring(mLockPatternView, mPaint, mPathPaint);
                TimingTweaks.setTiming(classLoader, mLockPatternView, mCancelPatternRunnable);
                PaintTweaks.setShader(mLockPatternView, mPaint, mPathPaint);
                MiscTweaks.setMisc(classLoader);
            }
        });
    }

}
