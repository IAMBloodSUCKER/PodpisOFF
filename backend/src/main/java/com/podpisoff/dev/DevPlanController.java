package com.podpisoff.dev;

import com.podpisoff.auth.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
public class DevPlanController {

    private final DevPlanService devPlanService;

    public DevPlanController(DevPlanService devPlanService) {
        this.devPlanService = devPlanService;
    }

    @GetMapping("/tools")
    public DevToolsResponse tools() {
        return devPlanService.toolsStatus();
    }

    @PostMapping("/plan")
    public AuthResponse switchPlan(@RequestBody @Valid DevPlanSwitchRequest request) {
        return devPlanService.switchPlan(request);
    }
}
