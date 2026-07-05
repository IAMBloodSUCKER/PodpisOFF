package com.podpisoff.reminder;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final AuthFacade authFacade;

    public ReminderService(ReminderRepository reminderRepository, AuthFacade authFacade) {
        this.reminderRepository = reminderRepository;
        this.authFacade = authFacade;
    }

    public List<ReminderResponse> list() {
        User user = authFacade.getCurrentUser();
        return reminderRepository.findAllByUserIdOrderByRemindAtAsc(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ReminderResponse create(ReminderRequest request) {
        User user = authFacade.getCurrentUser();
        Reminder reminder = new Reminder();
        reminder.setUser(user);
        apply(reminder, request);
        return toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse update(Long id, ReminderRequest request) {
        User user = authFacade.getCurrentUser();
        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reminder not found"));
        apply(reminder, request);
        return toResponse(reminder);
    }

    @Transactional
    public void delete(Long id) {
        User user = authFacade.getCurrentUser();
        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reminder not found"));
        reminderRepository.delete(reminder);
    }

    private void apply(Reminder reminder, ReminderRequest request) {
        reminder.setTitle(request.title().trim());
        reminder.setNote(request.note() == null ? null : request.note().trim());
        reminder.setRemindAt(request.remindAt());
        reminder.setDone(request.done());
    }

    private ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(
            reminder.getId(),
            reminder.getTitle(),
            reminder.getNote(),
            reminder.getRemindAt(),
            reminder.isDone(),
            reminder.getCreatedAt(),
            reminder.getUpdatedAt()
        );
    }
}
