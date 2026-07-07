package com.podpisoff.dev;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.dev")
public class DevPlanProperties {

    private String adminUsernames = "admin123";

    public String getAdminUsernames() {
        return adminUsernames;
    }

    public void setAdminUsernames(String adminUsernames) {
        this.adminUsernames = adminUsernames;
    }

    public List<String> adminUsernameList() {
        return Arrays.stream(adminUsernames.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
    }
}
