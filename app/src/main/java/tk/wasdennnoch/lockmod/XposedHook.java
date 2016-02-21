package tk.wasdennnoch.lockmod;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedHook implements IXposedHookLoadPackage {

    private static boolean debug = false;
    private static boolean reload_settings = true;

    private XSharedPreferences mPrefs = new XSharedPreferences(XposedHook.class.getPackage().getName());

    private static final String PACKAGE_OWN = "tk.wasdennnoch.lockmod";
    private static final String CLASS_OWN = "tk.wasdennnoch.lockmod.SettingsActivity";

    private static final String PACKAGE_SYSTEMUI = "com.android.systemui";

    private static final String CLASS_LOCK_PATTERN_VIEW = "com.android.internal.widget.LockPatternView";
    private static final String CLASS_KEYGUARD_PATTERN_VIEW = "com.android.keyguard.KeyguardPatternView";
    private static final String CLASS_KEYGUARD_UNLOCK_PATTERN_LISTENER = CLASS_KEYGUARD_PATTERN_VIEW + "$UnlockPatternListener";

    private XC_MethodHook.Unhook mLastSegmentAlphaUnhook;
    private boolean mTimingHooked;
    private boolean mClippingHooked;

    //private static final String CLASS_LOCK_PATTERN_UTILS = "com.android.internal.widget.LockPatternUtils";
    //private static final String CLASS_KEYGUARD_VIEW_MEDIATOR = "com.android.systemui.keyguard.KeyguardViewMediator";
    //private XC_MethodHook.Unhook mRandomDotAnimUnhook;
    //private XC_MethodHook.Unhook mKeguardMediatorUnhook1;
    //private XC_MethodHook.Unhook mKeguardMediatorUnhook2;
    //private XC_MethodHook.Unhook mKeguardMediatorUnhook3;
    //private Runnable mRandomDotAnimRunnable;
    //private List<Object> mRandomAnimatedCells = new ArrayList<>();

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(PACKAGE_OWN)) {
            // Hook own method to hide warning message
            XposedHelpers.findAndHookMethod(CLASS_OWN, lpparam.classLoader, "isEnabled", XC_MethodReplacement.returnConstant(true));

        } else if (lpparam.packageName.equals(PACKAGE_SYSTEMUI)) {

            hookConstructor(lpparam.classLoader);
            hookOnFinishInflate(lpparam.classLoader);

            log("Successfully hooked Keyguard");

        }
    }

    private void hookConstructor(ClassLoader classLoader) {
        XposedHelpers.findAndHookConstructor(CLASS_KEYGUARD_PATTERN_VIEW, classLoader, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                if (reload_settings)
                    mPrefs.reload();

                reload_settings = mPrefs.getBoolean("always_reload", true);
                debug = mPrefs.getBoolean("debug_log", false);

                Context context = (Context) param.args[0];
                Object mAppearAnimationUtils = XposedHelpers.getObjectField(param.thisObject, "mAppearAnimationUtils");
                Object mDisappearAnimationUtils = XposedHelpers.getObjectField(param.thisObject, "mDisappearAnimationUtils");

                setTimingFromConstructor(context, mAppearAnimationUtils, mDisappearAnimationUtils);
                logD("constructor afterHookedMethod");
            }
        });
    }

    private void hookOnFinishInflate(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(CLASS_KEYGUARD_PATTERN_VIEW, classLoader, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {

                final View mLockPatternView = (View) XposedHelpers.getObjectField(param.thisObject, "mLockPatternView");
                Paint mPaint = (Paint) XposedHelpers.getObjectField(mLockPatternView, "mPaint");
                Paint mPathPaint = (Paint) XposedHelpers.getObjectField(mLockPatternView, "mPathPaint");
                Object[][] mCellStates = (Object[][]) XposedHelpers.getObjectField(mLockPatternView, "mCellStates");
                final Runnable mCancelPatternRunnable = (Runnable) XposedHelpers.getObjectField(param.thisObject, "mCancelPatternRunnable");

                setColors(classLoader, mLockPatternView);
                setDimensions(mLockPatternView, mCellStates, mPathPaint);
                setStroke(mPaint);
                setBlurring(mLockPatternView, mPaint, mPathPaint);
                setTiming(classLoader, mLockPatternView, mCancelPatternRunnable);
                setMisc(classLoader, mLockPatternView, mPaint, mPathPaint);

                setExperimental(classLoader, mLockPatternView, param.thisObject);
                logD("onFinishInflate afterHookedMethod");
            }
        });
    }


    private void setColors(ClassLoader classLoader, View mLockPatternView) {
        XposedHelpers.setIntField(mLockPatternView, "mRegularColor", mPrefs.getInt("regular_color", 0xffffffff));
        XposedHelpers.setIntField(mLockPatternView, "mErrorColor", mPrefs.getInt("error_color", 0xfff4511e));
        XposedHelpers.setIntField(mLockPatternView, "mSuccessColor", mPrefs.getInt("success_color", 0xffffffff));
        if (mPrefs.getBoolean("disable_last_segment_alpha", false)) {
            if (mLastSegmentAlphaUnhook == null)
                mLastSegmentAlphaUnhook = XposedHelpers.findAndHookMethod(CLASS_LOCK_PATTERN_VIEW, classLoader, "calculateLastSegmentAlpha",
                        float.class, float.class, float.class, float.class,
                        XC_MethodReplacement.returnConstant(Color.alpha(mPrefs.getInt("regular_color", 0xffffffff)) / 255));
        } else {
            if (mLastSegmentAlphaUnhook != null) {
                mLastSegmentAlphaUnhook.unhook();
                mLastSegmentAlphaUnhook = null;
            }
        }
    }

    private void setDimensions(View mLockPatternView, Object[][] mCellStates, Paint mPathPaint) {
        XposedHelpers.setIntField(mLockPatternView, "mPathWidth", mPrefs.getInt("line_width", 6));
        mPathPaint.setStrokeWidth(mPrefs.getInt("line_width", 6));
        int dotSize = mPrefs.getInt("dot_size", 24);
        XposedHelpers.setIntField(mLockPatternView, "mDotSize", dotSize);
        for (Object[] i : mCellStates) {
            for (Object j : i) {
                XposedHelpers.setFloatField(j, "size", dotSize);
            }
        }
        XposedHelpers.setIntField(mLockPatternView, "mDotSizeActivated", mPrefs.getInt("dot_size_activated", 56));
    }

    private void setStroke(Paint mPaint) {
        if (mPrefs.getBoolean("stroke_dots", false)) {
            int strokeWidth = mPrefs.getInt("stroke_dots_width", 6);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(strokeWidth);
            if (mPrefs.getBoolean("dash_dots", false)) {
                PathEffect dash = new DashPathEffect(
                        new float[]{strokeWidth * mPrefs.getFloat("dash_dots_on_multiplier", 1),
                                strokeWidth * mPrefs.getFloat("dash_dots_off_multiplier", 1)}
                        , 0);
                PathEffect effect = new ComposePathEffect(dash, new CornerPathEffect(strokeWidth));
                mPaint.setPathEffect(effect);
            }
        }
    }

    private void setBlurring(View mLockPatternView, Paint mPaint, Paint mPathPaint) {
        if (mPrefs.getBoolean("blur_dot", false)) {
            // Hardware acceleration not supported by blurring. Enables clipping, but we have to live with it.
            mLockPatternView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            BlurMaskFilter blurMaskFilter = new BlurMaskFilter(mPrefs.getInt("blur_dot_radius", 5),
                    BlurMaskFilter.Blur.valueOf(mPrefs.getString("blur_dot_mode", "NORMAL")));
            mPaint.setMaskFilter(blurMaskFilter);
        }
        if (mPrefs.getBoolean("blur_line", false)) {
            // Hardware acceleration not supported by blurring. Enables clipping, but we have to live with it.
            mLockPatternView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            BlurMaskFilter blurMaskFilter = new BlurMaskFilter(mPrefs.getInt("blur_line_radius", 5),
                    BlurMaskFilter.Blur.valueOf(mPrefs.getString("blur_line_mode", "NORMAL")));
            mPathPaint.setMaskFilter(blurMaskFilter);
        }
    }

    private void setTiming(ClassLoader classLoader, final View mLockPatternView, final Runnable mCancelPatternRunnable) {
        if (!mTimingHooked) {
            XposedHelpers.findAndHookMethod(CLASS_KEYGUARD_UNLOCK_PATTERN_LISTENER, classLoader, "onPatternDetected", List.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mLockPatternView.removeCallbacks(mCancelPatternRunnable);
                    mLockPatternView.postDelayed(mCancelPatternRunnable, mPrefs.getInt("clear_timeout", 2000));
                    logD("onPatternDetected afterHookedMethod");
                }
            });
            mTimingHooked = true;
        }
    }

    private void setTimingFromConstructor(Context context, Object mAppearAnimationUtils, Object mDisappearAnimationUtils) {

        XposedHelpers.setLongField(mAppearAnimationUtils, "mDuration", mPrefs.getInt("appear_animation_duration", 220));
        XposedHelpers.setLongField(mDisappearAnimationUtils, "mDuration", mPrefs.getInt("disappear_animation_duration", 125));

        XposedHelpers.setFloatField(mAppearAnimationUtils, "mStartTranslation",
                (32 * context.getResources().getDisplayMetrics().density) * mPrefs.getFloat("appear_animation_start_translation", 1.5f));
        XposedHelpers.setFloatField(mDisappearAnimationUtils, "mStartTranslation",
                (32 * context.getResources().getDisplayMetrics().density) * mPrefs.getFloat("disappear_animation_start_translation", 1.2f));

        XposedHelpers.setFloatField(mAppearAnimationUtils, "mDelayScale", mPrefs.getFloat("appear_animation_delay_scale", 2.0f));
        XposedHelpers.setFloatField(mDisappearAnimationUtils, "mDelayScale", mPrefs.getFloat("disappear_animation_delay_scale", 0.8f));

        //TODO in settings
        //XposedHelpers.setObjectField(mAppearAnimationUtils, "mInterpolator", AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in));
        //XposedHelpers.setObjectField(mDisappearAnimationUtils, "mInterpolator", AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in));

    }

    private void setMisc(ClassLoader classLoader, final View mLockPatternView, final Paint mPaint, final Paint mPathPaint) {
        //TODO in settings (changable matrix rotation / gradient style, color picker)
        if (mPrefs.getBoolean("rainbow_shader", false)) {
            // Fun with shaders. Don't ask me why I did this, but it's a feature now

            mLockPatternView.post(new Runnable() {
                @Override
                public void run() {

                    /*int[] colors = new int[]{0xffff0000, 0xffff7f00, 0xffffff00, 0xff00ff00, 0xff00ffff, 0xff0000ff, 0xff7f00ff, 0xffff00ff};

                    Shader linearShaderDot = new LinearGradient(0, 0, 0, mLockPatternView.getHeight(), colors, null, Shader.TileMode.MIRROR);
                    Matrix matrixDot = new Matrix();
                    matrixDot.setRotate(45);
                    linearShaderDot.setLocalMatrix(matrixDot);
                    mPaint.setShader(linearShaderDot);

                    Shader linearShaderPath = new LinearGradient(0, 0, 0, mLockPatternView.getHeight(), colors, null, Shader.TileMode.MIRROR);
                    Matrix matrixLine = new Matrix();
                    matrixLine.setRotate(135);
                    linearShaderPath.setLocalMatrix(matrixLine);
                    mPathPaint.setShader(linearShaderPath);*/

                    int[] colors = new int[]{0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
                    Shader shader = new SweepGradient(mLockPatternView.getWidth() / 2, mLockPatternView.getHeight() / 2, colors, null);
                    mPaint.setShader(shader);
                    mPathPaint.setShader(shader);

                }
            });
        }
        final boolean disableClipping = mPrefs.getBoolean("disable_clipping", false);
        if (!mClippingHooked) {
            XposedHelpers.findAndHookMethod(CLASS_KEYGUARD_PATTERN_VIEW, classLoader, "enableClipping", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (disableClipping) {
                        param.args[0] = false;
                    }
                    logD("enableClipping afterHookedMethod");
                }
            });
            mClippingHooked = true;
        }
    }


    // Experimental stuff that is not yet configurable, unstable or jff. Empty if there's currently nothing to mess with
    @SuppressWarnings("UnusedParameters")
    private void setExperimental(ClassLoader classLoader, final View mLockPatternView, final Object thisObject) {




        /*if (mPrefs.getBoolean("random_activation_animation", false)) {
            final Object[][] sCells =
                    (Object[][]) XposedHelpers.getStaticObjectField(
                            XposedHelpers.findClass("com.android.internal.widget.LockPatternView.Cell", classLoader), "sCells");
            final int activateCount = mPrefs.getInt("random_activation_count", 1);
            final int activationInterval = mPrefs.getInt("random_activation_interval", 1000);
            mRandomDotAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    for (Object[] i : sCells) {
                        Collections.addAll(mRandomAnimatedCells, i);
                    }
                    Collections.shuffle(mRandomAnimatedCells);
                    mRandomAnimatedCells = mRandomAnimatedCells.subList(0, activateCount);
                    for (Object cell : mRandomAnimatedCells) {
                        XposedHelpers.callMethod(mLockPatternView, "startCellActivatedAnimation", cell);
                        XposedHelpers.callMethod(mLockPatternView, "cancelLineAnimations");
                    }
                    mRandomAnimatedCells.clear();
                    mLockPatternView.postDelayed(mRandomDotAnimRunnable, activationInterval);
                }
            };

            // Start it, but HECK PLEASE NOT INSTANTLY, IT WILL KILL EVERYTHING BECAUE IT'S GETTING CALLED EVERY TIME, AND IS REPEATING EVERY FEW MS, AND WITH LIKE 100 LOOPS - JUST NO
            //mLockPatternView.post(mRandomDotAnimRunnable);

            // Stop when unlocked
            if (mRandomDotAnimUnhook == null) {
                mRandomDotAnimUnhook = XposedHelpers.findAndHookMethod(CLASS_LOCK_PATTERN_UTILS, classLoader, "checkPattern", List.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if ((boolean) param.getResult()) {
                            mLockPatternView.removeCallbacks(mRandomDotAnimRunnable);
                            logD("checkPattern afterHookedMethod");
                        }
                    }
                });
                mKeguardMediatorUnhook1 = XposedHelpers.findAndHookMethod(CLASS_KEYGUARD_VIEW_MEDIATOR, classLoader, "handleShow", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mLockPatternView.post(mRandomDotAnimRunnable);
                        logD("handleShow afterHookedMethod");
                    }
                });
                mKeguardMediatorUnhook2 = XposedHelpers.findAndHookMethod(CLASS_KEYGUARD_VIEW_MEDIATOR, classLoader, "handleHide", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mLockPatternView.removeCallbacks(mRandomDotAnimRunnable);
                        logD("handleHide afterHookedMethod");
                    }
                });
                mKeguardMediatorUnhook3 = XposedHelpers.findAndHookMethod(CLASS_KEYGUARD_VIEW_MEDIATOR, classLoader, "handleNotifyScreenOff", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mLockPatternView.removeCallbacks(mRandomDotAnimRunnable);
                        logD("handleNotifyScreenOff afterHookedMethod");
                    }
                });
            }
        } else {
            if (mRandomDotAnimUnhook != null) {
                mRandomDotAnimUnhook.unhook();
                mKeguardMediatorUnhook1.unhook();
                mKeguardMediatorUnhook2.unhook();
                mKeguardMediatorUnhook3.unhook();
                mRandomDotAnimUnhook = null;
                mKeguardMediatorUnhook1 = null;
                mKeguardMediatorUnhook2 = null;
                mKeguardMediatorUnhook3 = null;
            }
        }*/


    }

    private void log(String msg) {
        XposedBridge.log("[LockMod] " + msg);
    }

    private void logD(String msg) {
        if (debug) log("[DEBUG] " + msg);
    }


}
