package org.radar.radarlint.settings;

import java.util.prefs.Preferences;

/**
 */
public class SonarLintProjectKeyPreference extends AbstractPreferenceAccessor<String> {
   
    public SonarLintProjectKeyPreference(Preferences preferences) {
        super(preferences, "sonarLint.projectkey");
    }

    @Override
    public void setValue(String value) {
        getPreferences().put(getPreferenceKey(), value);
    }

    @Override
    public String getValue() {
        return getPreferences().get(getPreferenceKey(), "");
    }
    
}
