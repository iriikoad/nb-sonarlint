package org.radar.radarlint.settings;

import org.openide.util.NbPreferences;
import org.radar.radarlint.SonarLintSaveTask;

/**
 *
 */
public class SonarLintGlobalActivePreference extends AbstractPreferenceAccessor<Boolean> {

    private static final boolean DEFAULT_ACTIVE = true;

    public SonarLintGlobalActivePreference() {
        super(NbPreferences.forModule(SonarLintSaveTask.class), "sonarlint.activated");
    }
    
    @Override
    public void setValue(Boolean value) {
        getPreferences().putBoolean(getPreferenceKey(), value);
    }

    @Override
    public Boolean getValue() {
        return getPreferences().getBoolean(getPreferenceKey(), DEFAULT_ACTIVE);
    }
    
}
