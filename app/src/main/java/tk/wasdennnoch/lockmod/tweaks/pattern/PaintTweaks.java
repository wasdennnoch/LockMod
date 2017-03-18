package tk.wasdennnoch.lockmod.tweaks.pattern;

import android.graphics.BlurMaskFilter;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.View;
import android.view.ViewTreeObserver;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;
import tk.wasdennnoch.lockmod.utils.ConfigUtils;

public class PaintTweaks {

    private static boolean mDisableHWAccelerationHooked;
    private static boolean mDisableHWAcceleration;

    public static void setStroke(Paint mPaint, Paint mPathPaint) {

        try {
            if (ConfigUtils.getBoolean("stroke_dots", false)) {
                int strokeWidth = ConfigUtils.getInt("stroke_dots_width", 6);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(strokeWidth);
                if (ConfigUtils.getBoolean("dash_dots", false)) {
                    PathEffect dash = new DashPathEffect(
                            new float[]{strokeWidth * ConfigUtils.getFloat("dash_dots_on_multiplier", 1),
                                    strokeWidth * ConfigUtils.getFloat("dash_dots_off_multiplier", 1)}
                            , 0);
                    PathEffect effect = new ComposePathEffect(dash, new CornerPathEffect(strokeWidth));
                    mPaint.setPathEffect(effect);
                }
            }
            int lineWidth = ConfigUtils.getInt("line_width", 6);
            if (ConfigUtils.getBoolean("dash_line", false)) {
                PathEffect dash = new DashPathEffect(
                        new float[]{lineWidth * ConfigUtils.getFloat("dash_line_on_multiplier", 1),
                                lineWidth * ConfigUtils.getFloat("dash_line_off_multiplier", 1)}
                        , 0);
                PathEffect effect = new ComposePathEffect(dash, new CornerPathEffect(lineWidth));
                mPathPaint.setPathEffect(effect);
            }
            XposedHook.logD("PaintTweaks", "Executed setStroke");
        } catch (Throwable t) {
            XposedHook.logE("PaintTweaks", "Error executing setStroke", t);
        }

    }

    public static void setBlurring(View mLockPatternView, Paint mPaint, Paint mPathPaint) {

        try {
            if (ConfigUtils.getBoolean("blur_dot", false)) {
                mDisableHWAcceleration = true;
                BlurMaskFilter blurMaskFilter = new BlurMaskFilter(ConfigUtils.getInt("blur_dot_radius", 5),
                        BlurMaskFilter.Blur.valueOf(ConfigUtils.getString("blur_dot_mode", "NORMAL")));
                mPaint.setMaskFilter(blurMaskFilter);
            } else {
                mDisableHWAcceleration = false;
            }
            if (ConfigUtils.getBoolean("blur_line", false)) {
                mDisableHWAcceleration = true;
                BlurMaskFilter blurMaskFilter = new BlurMaskFilter(ConfigUtils.getInt("blur_line_radius", 5),
                        BlurMaskFilter.Blur.valueOf(ConfigUtils.getString("blur_line_mode", "NORMAL")));
                mPathPaint.setMaskFilter(blurMaskFilter);
            } else {
                mDisableHWAcceleration = false;
            }
            disableHWAcceleration(mLockPatternView);
            XposedHook.logD("PaintTweaks", "Executed setBlurring");
        } catch (Throwable t) {
            XposedHook.logE("PaintTweaks", "Error executing setBlurring", t);
        }

    }

    public static void setShader(final View mLockPatternView, final Paint mPaint, final Paint mPathPaint) {

        try {
            if (ConfigUtils.getBoolean("rainbow_shader", false)) {

                final int[] colors = new int[]{0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};

                    /*Shader linearShaderDot = new LinearGradient(0, 0, 0, mLockPatternView.getHeight(), colors, null, Shader.TileMode.MIRROR);
                    Matrix matrixDot = new Matrix();
                    matrixDot.setRotate(45);
                    linearShaderDot.setLocalMatrix(matrixDot);
                    mPaint.setShader(linearShaderDot);

                    Shader linearShaderPath = new LinearGradient(0, 0, 0, mLockPatternView.getHeight(), colors, null, Shader.TileMode.MIRROR);
                    Matrix matrixLine = new Matrix();
                    matrixLine.setRotate(135);
                    linearShaderPath.setLocalMatrix(matrixLine);
                    mPathPaint.setShader(linearShaderPath);*/

                mLockPatternView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Fun with shaders. Don't ask me why I did this, but it's a feature now

                        //TODO add rotation with matrix, maybe a color picker
                        //TODO Seperate shaders for dots and line

                        // Shaders have to be initiated here because we need to wait until the view is laid out to fetch the size

                        int w = mLockPatternView.getWidth();
                        int h = mLockPatternView.getHeight();
                        if (w <= 0 || h <= 0) return;
                        final Shader shader = generteShader("rainbow_shader_type", w, h, colors, 190);

                        mPaint.setShader(shader);
                        mPathPaint.setShader(shader);
                    }
                });


            }
            XposedHook.logD("PaintTweaks", "Executed setShader");
        } catch (Throwable t) {
            XposedHook.logE("PaintTweaks", "Error executing setShader", t);
        }

    }

    private static Shader generteShader(String prefName, int width, int height, int[] colors, int rotation) {
        Shader.TileMode tileMode = Shader.TileMode.REPEAT;
        final Shader shader;
        switch (ConfigUtils.getString(prefName, "")) {
            default /* linear */:
                shader = new LinearGradient(0, 0, width, height, colors, null, tileMode);
                break;
            case "radial":
                shader = new RadialGradient(width / 2, width / 2, width / 2, colors, null, tileMode);
                break;
            case "sweep":
                shader = new SweepGradient(width / 2, height / 2, colors, null);
                break;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(rotation);
        shader.setLocalMatrix(matrix);
        return shader;
    }

    private static void disableHWAcceleration(View mLockPatternView) {
        // This disables HW acceleration for all views. Sadly this is necessary
        // to not lead the SystemUI into a crash loop because of a ClassCastException
        // because the LockPatternView is stupid.
        // TODO find a way to not completely disable HW acceleration for everything
        if (!mDisableHWAccelerationHooked) {
            XposedHelpers.findAndHookMethod(mLockPatternView.getClass().getSuperclass(), "isHardwareAccelerated", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mDisableHWAcceleration)
                        param.setResult(false);
                }
            });
            mDisableHWAccelerationHooked = true;
        }
        if (mDisableHWAcceleration)
            mLockPatternView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

}
