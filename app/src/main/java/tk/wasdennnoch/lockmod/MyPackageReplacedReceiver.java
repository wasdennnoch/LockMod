package tk.wasdennnoch.lockmod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class MyPackageReplacedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (Config.DEV) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    context.sendBroadcast(new Intent("reboot").setPackage("android"));
                }
            }, 1000);
        }
    }

}
