package tk.wasdennnoch.lockmod.tweaks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.lockmod.XposedHook;

public class DevTweaks {

    // Some testing during developement. Nothing working to see here.

    public static void setDevInit(ClassLoader classLoader) {


        /*Class<?> KeyguardSecurityContainer = XposedHelpers.findClass("com.android.keyguard.KeyguardSecurityContainer", classLoader);

        XposedBridge.hookAllConstructors(KeyguardSecurityContainer, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer constructor");
            }
        });

        XposedBridge.hookAllMethods(KeyguardSecurityContainer, "showSecurityScreen", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHook.log("KeyguardSecurityContainer showSecurityScreen");
                XposedHook.log(param.args[0].toString() +" -/- "+ param.thisObject.toString());
            }
        });*/


    }

}
