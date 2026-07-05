package com.podpisoff.reminder;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    public List<ReminderResponse> list() {
        return reminderService.list();
    }

    @PostMapping
    public ReminderResponse create(@RequestBody @Valid ReminderRequest request) {
        return reminderService.create(request);
    }

    @PutMapping("/{id}")
    public ReminderResponse update(@PathVariable Long id, @RequestBody @Valid ReminderRequest request) {
        return reminderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        reminderService.delete(id);
    }
}
