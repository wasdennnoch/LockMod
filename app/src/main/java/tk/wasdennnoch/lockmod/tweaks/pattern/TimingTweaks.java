package tk.wasdennnoch.lockmod.tweaks.pattern;

import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;
import tk.wasdennnoch.lockmod.utils.ConfigUtils;

public class TimingTweaks {

    private static boolean mTimingHooked;

    public static void setTiming(ClassLoader classLoader, final View mLockPatternView, final Runnable mCancelPatternRunnable) {

        try {
            if (!mTimingHooked) {
                XposedHelpers.findAndHookMethod(XposedHook.CLASS_KEYGUARD_UNLOCK_PATTERN_LISTENER, classLoader, "onPatternDetected", List.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mLockPatternView.removeCallbacks(mCancelPatternRunnable);
                        mLockPatternView.postDelayed(mCancelPatternRunnable, ConfigUtils.getInt("clear_timeout", 2000));
                        XposedHook.logD("TimingTweaks", "onPatternDetected afterHookedMethod");
                    }
                });
                mTimingHooked = true;
            }
            XposedHook.logD("TimingTweaks", "Executed setTiming");
        } catch (Throwable t) {
            XposedHook.logE("TimingTweaks", "Error executing setTiming", t);
        }

    }

    public static void setTimingFromConstructor(Context context, Object mAppearAnimationUtils, Object mDisappearAnimationUtils) {

        try {
            XposedHelpers.setLongField(mAppearAnimationUtils, "mDuration", ConfigUtils.getInt("appear_animation_duration", 220));
            XposedHelpers.setLongField(mDisappearAnimationUtils, "mDuration", ConfigUtils.getInt("disappear_animation_duration", 125));

            XposedHelpers.setFloatField(mAppearAnimationUtils, "mStartTranslation",
                    (32 * context.getResources().getDisplayMetrics().density) * ConfigUtils.getFloat("appear_animation_start_translation", 1.5f));
            XposedHelpers.setFloatField(mDisappearAnimationUtils, "mStartTranslation",
                    (32 * context.getResources().getDisplayMetrics().density) * ConfigUtils.getFloat("disappear_animation_start_translation", 1.2f));

            XposedHelpers.setFloatField(mAppearAnimationUtils, "mDelayScale", ConfigUtils.getFloat("appear_animation_delay_scale", 2.0f));
            XposedHelpers.setFloatField(mDisappearAnimationUtils, "mDelayScale", ConfigUtils.getFloat("disappear_animation_delay_scale", 0.8f));

            int ipID;
            String saved;
            for (int i = 0; i < 2; i++) {
                boolean appear = i == 0;
                if (appear)
                    saved = ConfigUtils.getString("appear_animation_interpolator", "");
                else
                    saved = ConfigUtils.getString("disappear_animation_interpolator", "");
                switch (saved) {
                    case "accelerate_cubic":
                        ipID = android.R.interpolator.accelerate_cubic;
                        break;
                    case "accelerate_decelerate":
                        ipID = android.R.interpolator.accelerate_decelerate;
                        break;
                    case "accelerate_quad":
                        ipID = android.R.interpolator.accelerate_quad;
                        break;
                    case "accelerate_quint":
                        ipID = android.R.interpolator.accelerate_quint;
                        break;
                    case "anticipate":
                        ipID = android.R.interpolator.anticipate;
                        break;
                    case "anticipate_overshoot":
                        ipID = android.R.interpolator.anticipate_overshoot;
                        break;
                    case "bounce":
                        ipID = android.R.interpolator.bounce;
                        break;
                    case "cycle":
                        ipID = android.R.interpolator.cycle;
                        break;
                    case "decelerate_cubic":
                        ipID = android.R.interpolator.decelerate_cubic;
                        break;
                    case "decelerate_quad":
                        ipID = android.R.interpolator.decelerate_quad;
                        break;
                    case "decelerate_quint":
                        ipID = android.R.interpolator.decelerate_quint;
                        break;
                    case "fast_out_linear_in":
                        ipID = android.R.interpolator.fast_out_linear_in;
                        break;
                    case "fast_out_slow_in":
                        ipID = android.R.interpolator.fast_out_slow_in;
                        break;
                    case "linear":
                        ipID = android.R.interpolator.linear;
                        break;
                    case "linear_out_slow_in":
                        ipID = android.R.interpolator.linear_out_slow_in;
                        break;
                    case "overshoot":
                        ipID = android.R.interpolator.overshoot;
                        break;
                    default:
                        if (appear)
                            ipID = android.R.interpolator.linear_out_slow_in;
                        else
                            ipID = android.R.interpolator.fast_out_linear_in;
                        break;
                }
                Interpolator interpolator = AnimationUtils.loadInterpolator(context, ipID);
                if (appear)
                    XposedHelpers.setObjectField(mAppearAnimationUtils, "mInterpolator", interpolator);
                else
                    XposedHelpers.setObjectField(mDisappearAnimationUtils, "mInterpolator", interpolator);
            }
            XposedHook.logD("TimingTweaks", "Executed setTimingFromConstructor");
        } catch (Throwable t) {
            XposedHook.logE("TimingTweaks", "Error executing setTimingFromConstructor", t);
        }

    }

}
