package tk.wasdennnoch.lockmod.tweaks.pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerPolicy;
import android.widget.ViewFlipper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;
import tk.wasdennnoch.lockmod.utils.ConfigUtils;

public class DevTweaks {

    // Some testing during developement. Nothing working to see here.

    public static void devInitSysUI(ClassLoader classLoader) {


        Class<?> KeyguardSecurityContainer = XposedHelpers.findClass("com.android.keyguard.KeyguardSecurityContainer", classLoader);

        XposedBridge.hookAllConstructors(KeyguardSecurityContainer, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logI("DevTweaks", "KeyguardSecurityContainer constructor");
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "showSecurityScreen", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logI("DevTweaks", "KeyguardSecurityContainer showSecurityScreen; param 0: "+param.args[0].toString() +" -/- "+ param.thisObject.getClass().getName());
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "getSecurityView", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logI("DevTweaks", "KeyguardSecurityContainer getSecurityView: param 0: "+param.args[0].toString()+" -/- "+param.args[0].getClass().getName());
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
                            XposedHook.logI("DevTweaks", "KeyguardSecurityContainer getSecurityView: post: executed");
                        }
                    });
                    XposedHook.logI("DevTweaks", "KeyguardSecurityContainer getSecurityView: result: " + v.getClass().getName());
                } else {
                    XposedHook.logI("DevTweaks", "KeyguardSecurityContainer getSecurityView: result == null");
                }
            }
        });

        XposedHelpers.findAndHookMethod(KeyguardSecurityContainer, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                ViewFlipper mSecurityViewFlipper = (ViewFlipper) XposedHelpers.getObjectField(param.thisObject, "mSecurityViewFlipper");
                XposedHook.logI("DevTweaks", "KeyguardSecurityContainer onFinishInflate, mSecurityViewFlipper class: "+mSecurityViewFlipper.getClass().getName());


                final View v = (View) param.thisObject;
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        XposedHook.logI("DevTweaks", "KeyguardSecurityContainer: post(): getHeight: "+v.getHeight()+", getWidth: "+v.getWidth());
                    }
                });

                XposedHelpers.callMethod(param.thisObject, "showDialog", "Title", "Message: onFinishInflate");


            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "showBouncer", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logI("DevTweaks", "KeyguardSecurityContainer showBouncer");
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "hideBouncer", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logI("DevTweaks", "KeyguardSecurityContainer hideBouncer");
            }
        });


        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "startAppearAnimation", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logI("DevTweaks", "KeyguardSecurityContainer startAppearAnimation");
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "startDisappearAnimation", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.logI("DevTweaks", "KeyguardSecurityContainer startDisappearAnimation");
            }
        });


    }


    public static void devInitAndroid(ClassLoader classLoader) {
        Class<?> classPhoneWindowManager = XposedHelpers.findClass(ConfigUtils.M ? "com.android.server.policy.PhoneWindowManager" : "com.android.internal.policy.impl.PhoneWindowManager", classLoader);
        XposedHelpers.findAndHookMethod(classPhoneWindowManager, "init", Context.class, IWindowManager.class, WindowManagerPolicy.WindowManagerFuncs.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object pwm = param.thisObject;
                Context c = (Context) XposedHelpers.getObjectField(pwm, "mContext");
                c.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        throw new RuntimeException("Shhhh... Sleep...");
                    }
                }, new IntentFilter("reboot"));
            }
        });
    }


}
