package com.podpisoff.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    private String panelKey = "podpisoff-admin-dev-key";

    public String getPanelKey() {
        return panelKey;
    }

    public void setPanelKey(String panelKey) {
        this.panelKey = panelKey;
    }
}
