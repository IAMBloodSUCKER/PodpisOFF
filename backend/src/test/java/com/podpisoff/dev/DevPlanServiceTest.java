package com.podpisoff.dev;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.podpisoff.user.Plan;
import com.podpisoff.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DevPlanServiceTest {

    private DevPlanProperties properties;
    private DevPlanService service;

    @BeforeEach
    void setUp() {
        properties = new DevPlanProperties();
        properties.setAdminUsernames("admin123,tester");
        service = new DevPlanService(null, null, null, properties, null);
    }

    @Test
    void recognizesAdminUsernames() {
        User admin = new User();
        admin.setUsername("admin123");
        assertTrue(service.isDevAdmin(admin));

        User mixedCase = new User();
        mixedCase.setUsername("Admin123");
        assertTrue(service.isDevAdmin(mixedCase));

        User regular = new User();
        regular.setUsername("user1");
        assertFalse(service.isDevAdmin(regular));
    }
}
