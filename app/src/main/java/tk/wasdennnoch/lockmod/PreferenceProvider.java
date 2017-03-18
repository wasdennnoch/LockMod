package tk.wasdennnoch.lockmod;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class PreferenceProvider extends RemotePreferenceProvider {

    public PreferenceProvider() {
        super("tk.wasdennnoch.lockmod.PREFERENCES", new String[]{"tk.wasdennnoch.lockmod_preferences"});
    }

}
