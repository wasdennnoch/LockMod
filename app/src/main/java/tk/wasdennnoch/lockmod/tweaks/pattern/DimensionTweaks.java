package tk.wasdennnoch.lockmod.tweaks.pattern;

import android.graphics.Paint;
import android.os.Build;
import android.view.View;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;
import tk.wasdennnoch.lockmod.utils.ConfigUtils;

public class DimensionTweaks {

    public static void setDimensions(View mLockPatternView, Object[][] mCellStates, Paint mPathPaint) {

        try {
            XposedHelpers.setIntField(mLockPatternView, "mPathWidth", ConfigUtils.getInt("line_width", 6));
            mPathPaint.setStrokeWidth(ConfigUtils.getInt("line_width", 6));
            int dotSize = ConfigUtils.getInt("dot_size", 24);
            XposedHelpers.setIntField(mLockPatternView, "mDotSize", dotSize);
            for (Object[] i : mCellStates) {
                for (Object j : i) {
                    if (Build.VERSION.SDK_INT >= 22)
                        XposedHelpers.setFloatField(j, "radius", dotSize / 2);
                    else
                        XposedHelpers.setFloatField(j, "size", dotSize);
                }
            }
            XposedHelpers.setIntField(mLockPatternView, "mDotSizeActivated", ConfigUtils.getInt("dot_size_activated", 56));
            XposedHook.logD("DimensionTweaks", "Executed setDimensions");
        } catch (Throwable t) {
            XposedHook.logE("DimensionTweaks", "Error executing setDimensions", t);
        }

    }

}
