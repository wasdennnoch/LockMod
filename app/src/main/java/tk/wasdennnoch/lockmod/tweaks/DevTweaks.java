package tk.wasdennnoch.lockmod.tweaks;

import android.view.View;
import android.widget.ViewFlipper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;

public class DevTweaks {

    // Some testing during developement. Nothing working to see here.

    public static void setDevInit(ClassLoader classLoader) {


        Class<?> KeyguardSecurityContainer = XposedHelpers.findClass("com.android.keyguard.KeyguardSecurityContainer", classLoader);

        XposedBridge.hookAllConstructors(KeyguardSecurityContainer, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer constructor");
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "showSecurityScreen", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer showSecurityScreen; param 0: "+param.args[0].toString() +" -/- "+ param.thisObject.getClass().getName());
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "getSecurityView", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer getSecurityView: param 0: "+param.args[0].toString()+" -/- "+param.args[0].getClass().getName());
            }
            @Override
             protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final View v = (View) param.getResult();
                if (v != null) {
                    v.post(new Runnable() {
                        @Override
                        public void run() {
                            v.setBackgroundColor(0x22ff0000);
                            v.getLayoutParams().height += 400;
                            v.requestLayout();
                            XposedHook.log("KeyguardSecurityContainer getSecurityView: post: executed");
                        }
                    });
                    XposedHook.log("KeyguardSecurityContainer getSecurityView: result: " + v.getClass().getName());
                } else {
                    XposedHook.log("KeyguardSecurityContainer getSecurityView: result == null");
                }
            }
        });

        XposedHelpers.findAndHookMethod(KeyguardSecurityContainer, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                ViewFlipper mSecurityViewFlipper = (ViewFlipper) XposedHelpers.getObjectField(param.thisObject, "mSecurityViewFlipper");
                XposedHook.log("KeyguardSecurityContainer onFinishInflate, mSecurityViewFlipper class: "+mSecurityViewFlipper.getClass().getName());


                final View v = (View) param.thisObject;
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        XposedHook.log("KeyguardSecurityContainer: post(): getHeight: "+v.getHeight()+", getWidth: "+v.getWidth());
                    }
                });

                XposedHelpers.callMethod(param.thisObject, "showDialog", "Title", "Message: onFinishInflate");


            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "showBouncer", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer showBouncer");
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "hideBouncer", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer hideBouncer");
            }
        });


        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "startAppearAnimation", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer startAppearAnimation");
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "startDisappearAnimation", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer startDisappearAnimation");
            }
        });


    }

}
