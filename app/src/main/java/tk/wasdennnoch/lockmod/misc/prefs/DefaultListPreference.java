package tk.wasdennnoch.lockmod.misc.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;

import tk.wasdennnoch.lockmod.R;

public class DefaultListPreference extends ListPreference {

    String mDefaultValue;

    public DefaultListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // Find default value in entries and indicate that
        int index = findIndexOfValue(mDefaultValue);
        CharSequence[] entries = getEntries();
        if (index >= 0) {
            entries[index] = getContext().getString(R.string.pref_default, entries[index].toString());
            setEntries(entries);
        }
    }

    public DefaultListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DefaultListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.dialogPreferenceStyle);
    }

    public DefaultListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected String onGetDefaultValue(TypedArray a, int index) {
        return (mDefaultValue = (String) super.onGetDefaultValue(a, index));
    }
}
