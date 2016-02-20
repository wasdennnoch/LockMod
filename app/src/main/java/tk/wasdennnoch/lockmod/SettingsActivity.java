package tk.wasdennnoch.lockmod;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;

import com.ceco.lollipop.gravitybox.preference.SeekBarPreference;

import java.util.List;

public class SettingsActivity extends Activity implements LockPatternView.OnPatternListener {

    private boolean mPreviewVisible = false;
    private static LockPatternView mLockPatternView;
    private View mFragment;
    private int mLockPatternHeight;
    private int mFragmentHeight;
    private static SharedPreferences mPrefs;
    private Runnable mCancelPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction().replace(R.id.fragment, new SettingsFragment()).commit();
        mLockPatternView = (LockPatternView) findViewById(R.id.lockPatternView);
        mFragment = findViewById(R.id.fragment);
        mLockPatternView.setOnPatternListener(this);


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ((TextView) findViewById(R.id.not_enabled_warning)).setText(String.format(getString(R.string.wrong_version_warning), Build.VERSION.SDK_INT));
        } else {
            if (isEnabled()) {
                findViewById(R.id.not_enabled_warning).setVisibility(View.GONE);
            }
        }

        mLockPatternView.post(new Runnable() {
            @Override
            public void run() {
                mLockPatternHeight = mLockPatternView.getHeight();
            }
        });
        mFragment.post(new Runnable() {
            @Override
            public void run() {
                mFragmentHeight = mFragment.getMeasuredHeight();
                setLockViewState(mPreviewVisible, false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preview_toggle:
                if (mPreviewVisible) {
                    // Hide the preview
                    setLockViewState(false, true);
                } else {
                    // Show the preview
                    setLockViewState(true, true);
                }
                mPreviewVisible = !mPreviewVisible;
                return true;
            case R.id.action_calc:

                View v = View.inflate(this, R.layout.dialog_calc, null);
                final TextView tv = (TextView) v.findViewById(R.id.px);
                tv.setText(getString(R.string.dialog_calc_out_px_format, 0));
                ((EditText) v.findViewById(R.id.dp)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            tv.setText(getString(R.string.dialog_calc_out_px_format, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Integer.parseInt(s.toString()), getResources().getDisplayMetrics())));
                        } catch (NumberFormatException e) {
                            tv.setText(getString(R.string.dialog_calc_out_px_format, 0));
                        }
                    }
                });

                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_calc_title)
                        .setPositiveButton(R.string.dialog_calc_positive, null)
                        .setView(v)
                        .show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mPreviewVisible) {
            setLockViewState(false, true);
            mPreviewVisible = false;
        } else {
            super.onBackPressed();
            finish();
        }
    }

    private void setLockViewState(boolean show, boolean animate) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            return;

        if (show) {
            if (animate) {
                mLockPatternView.animate()
                        .y(mFragmentHeight - mLockPatternHeight)
                        .setDuration(750)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                mLockPatternView.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mFragment.getLayoutParams().height = mFragmentHeight - mLockPatternHeight;
                                mFragment.requestLayout();
                            }
                        })
                        .start();
            } else {
                mLockPatternView.setY(mFragmentHeight - mLockPatternHeight);
                mLockPatternView.setVisibility(View.VISIBLE);
            }
        } else {
            if (animate) {
                mLockPatternView.animate()
                        .y(mFragmentHeight)
                        .setDuration(750)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                mFragment.getLayoutParams().height = mFragmentHeight;
                                mFragment.requestLayout();
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mLockPatternView.setVisibility(View.GONE);
                            }
                        })
                        .start();
            } else {
                mLockPatternView.setVisibility(View.GONE);
                mLockPatternView.setY(mFragmentHeight);
            }
        }
    }

    @Override
    public void onPatternStart() {
        mLockPatternView.removeCallbacks(mCancelPatternRunnable);
    }

    @Override
    public void onPatternCleared() {
    }

    @Override
    public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
    }

    @Override
    public void onPatternDetected(List<LockPatternView.Cell> pattern) {
        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
        mLockPatternView.postDelayed(mCancelPatternRunnable, mPrefs.getInt("clear_timeout", 2000));
    }

    // Will be hooked to return true
    public boolean isEnabled() {
        return false;
    }


    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //noinspection deprecation
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            mPrefs = getPreferenceManager().getSharedPreferences();
            if (!mPrefs.contains("line_width")) {
                mPrefs.edit().putInt("line_width", getResources().getDimensionPixelSize(R.dimen.line_width_default)).apply();
            }
            if (!mPrefs.contains("dot_size")) {
                mPrefs.edit().putInt("dot_size", getResources().getDimensionPixelSize(R.dimen.dot_size_default)).apply();
            }
            if (!mPrefs.contains("dot_size_activated")) {
                mPrefs.edit().putInt("dot_size_activated", getResources().getDimensionPixelSize(R.dimen.dot_size_activated_default)).apply();
            }
            mLockPatternView.setLockPatternSize((byte) mPrefs.getInt("preview_pattern_size", 3));
            mLockPatternView.initValues(mPrefs);

            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                // Do not refresh preview pattern if those values are changed
                case "appear_animation_duration":
                case "appear_animation_start_translation":
                case "appear_animation_delay_scale":
                case "disappear_animation_duration":
                case "disappear_animation_start_translation":
                case "disappear_animation_delay_scale":
                case "disable_clipping":
                case "preview_pattern_size":
                    break;
                case "hide_launcher_icon":
                    int mode = prefs.getBoolean("hide_launcher_icon", false) ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), "tk.wasdennnoch.lockmod.SettingsAlias"), mode, PackageManager.DONT_KILL_APP);
                    break;
                default:
                    mLockPatternView.initValues(prefs);
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            if (mPrefs.getBoolean("secret", false))
                ((SeekBarPreference) findPreference("preview_pattern_size")).setMaximum(20);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

    }

}
