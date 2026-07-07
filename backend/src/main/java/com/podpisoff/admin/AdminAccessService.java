package com.podpisoff.admin;

import com.podpisoff.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminAccessService {

    private final AdminProperties adminProperties;

    public AdminAccessService(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    public void requireAdminKey(HttpServletRequest request) {
        String configured = adminProperties.getPanelKey();
        if (!StringUtils.hasText(configured)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Admin panel is not configured");
        }
        String provided = request.getHeader("X-Admin-Key");
        if (!configured.equals(provided)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid admin key");
        }
    }

    public boolean verifyKey(String key) {
        String configured = adminProperties.getPanelKey();
        return StringUtils.hasText(configured) && configured.equals(key);
    }
}
