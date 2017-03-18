package tk.wasdennnoch.lockmod.tweaks.pattern;

import android.graphics.Color;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;
import tk.wasdennnoch.lockmod.utils.ConfigUtils;

public class ColorTweaks {

    private static XC_MethodHook.Unhook mLastSegmentAlphaUnhook;

    public static void setColors(ClassLoader classLoader, View mLockPatternView) {

        try {
            XposedHelpers.setIntField(mLockPatternView, "mRegularColor", ConfigUtils.getInt("regular_color", 0xffffffff));
            XposedHelpers.setIntField(mLockPatternView, "mErrorColor", ConfigUtils.getInt("error_color", 0xfff4511e));
            XposedHelpers.setIntField(mLockPatternView, "mSuccessColor", ConfigUtils.getInt("success_color", 0xffffffff));

            if (ConfigUtils.getBoolean("disable_last_segment_alpha", false)) {
                if (mLastSegmentAlphaUnhook == null)
                    mLastSegmentAlphaUnhook = XposedHelpers.findAndHookMethod(XposedHook.CLASS_LOCK_PATTERN_VIEW, classLoader, "calculateLastSegmentAlpha",
                            float.class, float.class, float.class, float.class,
                            XC_MethodReplacement.returnConstant(Color.alpha(ConfigUtils.getInt("regular_color", 0xffffffff)) / 255));
            } else {
                if (mLastSegmentAlphaUnhook != null) {
                    mLastSegmentAlphaUnhook.unhook();
                    mLastSegmentAlphaUnhook = null;
                }
            }
            XposedHook.logD("ColorTweaks", "Executed setColors");
        } catch (Throwable t) {
            XposedHook.logE("ColorTweaks", "Error executing setColors", t);
        }

    }

}
