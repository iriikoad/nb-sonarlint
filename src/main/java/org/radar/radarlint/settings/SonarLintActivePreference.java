package org.radar.radarlint.settings;

import java.util.prefs.Preferences;

/**
 *
 * @author Víctor
 */
public class SonarLintActivePreference extends AbstractPreferenceAccessor<Boolean> {
    
    public SonarLintActivePreference(Preferences preferences) {
        super(preferences, "sonarLint.active");
    }

    @Override
    public void setValue(Boolean value) {
        getPreferences().putBoolean(getPreferenceKey(), value);
    }

    @Override
    public Boolean getValue() {
        return getPreferences().getBoolean(getPreferenceKey(), true);
    }
    
}
