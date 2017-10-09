package tk.wasdennnoch.lockmod.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.ceco.lollipop.gravitybox.preference.SeekBarPreference;

import java.io.File;
import java.util.List;

import tk.wasdennnoch.lockmod.LockPatternPreviewView;
import tk.wasdennnoch.lockmod.R;

public class PatternViewSettingsActivity extends Activity implements LockPatternPreviewView.OnPatternListener {

    private static boolean mPreviewVisible = false;
    private static LockPatternPreviewView mLockPatternView;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern_view_settings);

        //noinspection ConstantConditions
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mFragment = findViewById(R.id.fragment);
        mLockPatternView = (LockPatternPreviewView) findViewById(R.id.lockPatternView);
        mLockPatternView.setOnPatternListener(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        getFragmentManager().beginTransaction().replace(R.id.fragment, new Fragment()).commit();


        mLockPatternView.post(new Runnable() {
            @Override
            public void run() {
                mLockPatternHeight = mLockPatternView.getHeight();
            }
        });
        mFragment.post(new Runnable() {
            @Override
            public void run() {
                mFragmentHeight = mFragment.getHeight();
                mPreviewVisible = false;
                if (!isLandscape()) {
                    mLockPatternView.setVisibility(View.GONE);
                    mLockPatternView.setY(mFragmentHeight);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        final Switch s = (Switch) menu.findItem(R.id.action_toggle).getActionView();
        s.setChecked(mPrefs.getBoolean("active_pattern_tweaks", false));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPrefs.edit().putBoolean("active_pattern_tweaks", s.isChecked()).apply();
            }
        });
        if (isLandscape())
            menu.findItem(R.id.action_preview_toggle).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preview_toggle:
                if (mPreviewVisible) {
                    setLockViewState(false); // Hide the preview
                } else {
                    setLockViewState(true); // Show the preview
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
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mPreviewVisible) {
            setLockViewState(false);
            mPreviewVisible = false;
        } else {
            super.onBackPressed();
            finish();
        }
    }

    private void setLockViewState(boolean show) {
        if (isLandscape())
            return;
        ViewPropertyAnimator anim = mLockPatternView.animate()
                .setDuration(750)
                .setInterpolator(new AccelerateDecelerateInterpolator());
        if (show) {
            anim.y(mFragmentHeight - mLockPatternHeight)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mLockPatternView.setVisibility(View.VISIBLE);
                            mLockPatternView.initValues(mPrefs);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mFragment.getLayoutParams().height = mFragmentHeight - mLockPatternHeight;
                            mFragment.requestLayout();
                        }
                    });
        } else {
            anim.y(mFragmentHeight)
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
                    });
        }
        anim.start();
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    public void onPatternStart() {
        mLockPatternView.removeCallbacks(mCancelPatternRunnable);
    }

    @Override
    public void onPatternCleared() {
    }

    @Override
    public void onPatternCellAdded(List<LockPatternPreviewView.Cell> pattern) {
    }

    @Override
    public void onPatternDetected(List<LockPatternPreviewView.Cell> pattern) {
        mLockPatternView.setDisplayMode(LockPatternPreviewView.DisplayMode.Wrong);
        mLockPatternView.postDelayed(mCancelPatternRunnable, mPrefs.getInt("clear_timeout", 2000));
    }


    public static class Fragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

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
            setClipping();
            mLockPatternView.setLockPatternSize((byte) mPrefs.getInt("preview_pattern_size", 3));
            mLockPatternView.initValues(mPrefs);

            addPreferencesFromResource(R.xml.preferences_pattern_view);
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
                case "preview_pattern_size":
                    break;
                case "hide_launcher_icon":
                    int mode = prefs.getBoolean("hide_launcher_icon", false) ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), "tk.wasdennnoch.lockmod.SettingsAlias"), mode, PackageManager.DONT_KILL_APP);
                    break;
                case "disable_clipping":
                    setClipping();
                    break;
                default:
                    if (mPreviewVisible)
                        mLockPatternView.initValues(prefs);
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            if (mPrefs.getBoolean("secret", false))
                ((SeekBarPreference) findPreference("preview_pattern_size")).setMaximum(20);
        }

        @Override
        public void onPause() {
            super.onPause();
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
            File sharedPrefsDir = new File(getActivity().getFilesDir(), "../shared_prefs");
            File sharedPrefsFile = new File(sharedPrefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
            if (sharedPrefsFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                sharedPrefsFile.setReadable(true, false);
            }
        }

        private void setClipping() {
            mLockPatternView.setClipToOutline(!mPrefs.getBoolean("disable_clipping", false)); // Enable the deactivation
        }

    }

}
