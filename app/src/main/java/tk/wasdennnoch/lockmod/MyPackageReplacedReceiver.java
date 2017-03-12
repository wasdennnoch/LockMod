package tk.wasdennnoch.lockmod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyPackageReplacedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Config.DEV) {
            context.sendBroadcast(new Intent("reboot").setPackage("android"));
        }
    }

}
