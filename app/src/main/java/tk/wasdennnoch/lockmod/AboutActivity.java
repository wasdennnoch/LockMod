package tk.wasdennnoch.lockmod;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity extends Activity {

    private int mSecretClickCount;
    private Toast mSecretToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.version)).setText(String.format(getString(R.string.about_version), pInfo.versionName, pInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            findViewById(R.id.version).setVisibility(View.GONE);
        }

        findViewById(R.id.github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://github.com/wasdennnoch/LockMod")));
            }
        });
        findViewById(R.id.xda).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/xposed/modules/tweak-lollipop-lockscreen-t3319133/")));
            }
        });
        findViewById(R.id.icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AboutActivity.this);
                if (!prefs.getBoolean("secret", false)) {
                    mSecretClickCount++;
                    if (mSecretClickCount == 10) {
                        if (mSecretToast != null)
                            mSecretToast.cancel();
                        mSecretToast = Toast.makeText(AboutActivity.this, "You have unlocked a secret! You can now set the lock pattern size up to 20 :P", Toast.LENGTH_LONG);
                        mSecretToast.show();
                        prefs.edit().putBoolean("secret", true).apply();
                    } else if (mSecretClickCount >= 5) {
                        if (mSecretToast != null)
                            mSecretToast.cancel();
                        if (10 - mSecretClickCount == 1)
                            mSecretToast = Toast.makeText(AboutActivity.this, String.format("You are %d click away from a secret :D", 10 - mSecretClickCount), Toast.LENGTH_SHORT);
                        else
                            mSecretToast = Toast.makeText(AboutActivity.this, String.format("You are %d clicks away from a secret :D", 10 - mSecretClickCount), Toast.LENGTH_SHORT);
                        mSecretToast.show();
                    }
                    return;
                }
                if (mSecretToast != null)
                    mSecretToast.cancel();
                mSecretToast = Toast.makeText(AboutActivity.this, "You have already unlocked the secret :P", Toast.LENGTH_SHORT);
                mSecretToast.show();
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
