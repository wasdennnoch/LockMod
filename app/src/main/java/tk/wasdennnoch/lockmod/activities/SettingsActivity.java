package tk.wasdennnoch.lockmod.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import tk.wasdennnoch.lockmod.R;

public class SettingsActivity extends Activity implements AdapterView.OnItemClickListener {

    private List<AdapterItem> mItems = Arrays.asList(
            new AdapterItem(R.string.activity_pattern_settings, R.drawable.ic_settings, PatternViewSettingsActivity.class),
            new AdapterItem(R.string.activity_general, R.drawable.ic_settings, GeneralSettingsActivity.class),
            //new AdapterItem(R.string.activity_experimental, R.drawable.ic_settings, ExperimentalSettingsActivity.class),
            new AdapterItem(R.string.activity_about, R.drawable.ic_info, AboutActivity.class)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ((TextView) findViewById(R.id.not_enabled_warning)).setText(String.format(getString(R.string.wrong_version_warning), Build.VERSION.SDK_INT));
        } else {
            if (isEnabled()) {
                findViewById(R.id.not_enabled_warning).setVisibility(View.GONE);
            }
        }

        ListView listView = (ListView) findViewById(R.id.list);
        listView.setOnItemClickListener(this);
        listView.setAdapter(new Adapter(this, R.layout.settings_list_item, mItems));

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(this, mItems.get(position).target));
    }

    // Will be hooked to return true
    public boolean isEnabled() {
        return false;
    }

    private class Adapter extends ArrayAdapter {

        Adapter(Context context, int resource, List<AdapterItem> objects) {
            //noinspection unchecked
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AdapterItem item = mItems.get(position);
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.settings_list_item, parent, false);
            ((TextView) convertView.findViewById(R.id.name)).setText(getString(item.nameId));
            ((ImageView) convertView.findViewById(R.id.icon)).setImageResource(item.iconId);
            return convertView;
        }

    }

    private class AdapterItem {
        int nameId;
        int iconId;
        Class<?> target;

        AdapterItem(int nameId, int iconId, Class<?> target) {
            this.nameId = nameId;
            this.iconId = iconId;
            this.target = target;
        }
    }

}
