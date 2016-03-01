package tk.wasdennnoch.lockmod.tweaks;

public class ExperimentalTweaks {

    //private static final String CLASS_LOCK_PATTERN_UTILS = "com.android.internal.widget.LockPatternUtils";
    //private static final String CLASS_KEYGUARD_VIEW_MEDIATOR = "com.android.systemui.keyguard.KeyguardViewMediator";
    //private XC_MethodHook.Unhook mRandomDotAnimUnhook;
    //private XC_MethodHook.Unhook mKeguardMediatorUnhook1;
    //private XC_MethodHook.Unhook mKeguardMediatorUnhook2;
    //private XC_MethodHook.Unhook mKeguardMediatorUnhook3;
    //private Runnable mRandomDotAnimRunnable;
    //private List<Object> mRandomAnimatedCells = new ArrayList<>();


    /*public static void setRandomDotAnim(XSharedPreferences prefs, ClassLoader classLoader, final View mLockPatternView, final Object thisObject) {

        try {
        if (mPrefs.getBoolean("random_activation_animation", false)) {
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
        }
            XposedHook.logD("Executed setRandomDotAnim");
        } catch (Throwable t) {
            XposedHook.logE("Error executing setRandomDotAnim", t);
        }

    }*/

}
