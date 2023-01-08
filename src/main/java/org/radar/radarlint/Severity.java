package org.radar.radarlint;

/**
 *
 * @author Victor
 */
public enum Severity {
    BLOCKER("Blocker", "/org/radar/radarlint/images/blocker.png"),
    CRITICAL("Critical", "/org/radar/radarlint/images/critical.png"),
    MAJOR("Major", "/org/radar/radarlint/images/major.png"),
    MINOR("Minor", "/org/radar/radarlint/images/minor.png"),
    INFO("Info", "/org/radar/radarlint/images/info.png");
    
    private final String userDescription;
    private final String resourcePath;
    private boolean visible = true;

    private Severity(String userDescription, String resourcePath) {
        this.userDescription = userDescription;
        this.resourcePath = resourcePath;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
