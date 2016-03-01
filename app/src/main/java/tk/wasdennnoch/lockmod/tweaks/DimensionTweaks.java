package tk.wasdennnoch.lockmod.tweaks;

import android.graphics.Paint;
import android.os.Build;
import android.view.View;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;

public class DimensionTweaks {

    public static void setDimensions(XSharedPreferences prefs, View mLockPatternView, Object[][] mCellStates, Paint mPathPaint) {

        try {
            XposedHelpers.setIntField(mLockPatternView, "mPathWidth", prefs.getInt("line_width", 6));
            mPathPaint.setStrokeWidth(prefs.getInt("line_width", 6));
            int dotSize = prefs.getInt("dot_size", 24);
            XposedHelpers.setIntField(mLockPatternView, "mDotSize", dotSize);
            for (Object[] i : mCellStates) {
                for (Object j : i) {
                    if (Build.VERSION.SDK_INT >= 22)
                        XposedHelpers.setFloatField(j, "radius", dotSize / 2);
                    else
                        XposedHelpers.setFloatField(j, "size", dotSize);
                }
            }
            XposedHelpers.setIntField(mLockPatternView, "mDotSizeActivated", prefs.getInt("dot_size_activated", 56));
            XposedHook.logD("Executed setDimensions");
        } catch (Throwable t) {
            XposedHook.logE("Error executing setDimensions", t);
        }

    }

}
