package com.podpisoff.reminder;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.Plan;
import com.podpisoff.user.PlanAccessService;
import com.podpisoff.user.PlanLimits;
import com.podpisoff.user.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {

    private static final int FREE_PLAN_LIMIT = PlanLimits.FREE_REMINDER_LIMIT;

    private final ReminderRepository reminderRepository;
    private final AuthFacade authFacade;
    private final PlanAccessService planAccessService;

    public ReminderService(ReminderRepository reminderRepository,
                           AuthFacade authFacade,
                           PlanAccessService planAccessService) {
        this.reminderRepository = reminderRepository;
        this.authFacade = authFacade;
        this.planAccessService = planAccessService;
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
        long currentCount = reminderRepository.countByUserId(user.getId());
        if (planAccessService.effectivePlan(user) == Plan.FREE && currentCount >= FREE_PLAN_LIMIT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FREE plan allows up to 5 reminders");
        }
        planAccessService.validateReminderRepeat(user, request.repeat(), null);
        Reminder reminder = new Reminder();
        reminder.setUser(user);
        apply(reminder, request, false);
        return toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse update(Long id, ReminderRequest request) {
        User user = authFacade.getCurrentUser();
        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reminder not found"));
        planAccessService.validateReminderRepeat(user, request.repeat(), reminder.getRepeatType());
        apply(reminder, request, reminder.isDone());
        return toResponse(reminder);
    }

    @Transactional
    public void delete(Long id) {
        User user = authFacade.getCurrentUser();
        Reminder reminder = reminderRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reminder not found"));
        reminderRepository.delete(reminder);
    }

    private void apply(Reminder reminder, ReminderRequest request, boolean wasDone) {
        ReminderRepeat repeat = request.repeat() == null ? ReminderRepeat.ONCE : request.repeat();
        reminder.setTitle(request.title().trim());
        reminder.setNote(request.note() == null ? null : request.note().trim());
        reminder.setRepeatType(repeat);

        LocalDateTime remindAt = request.remindAt();
        if (repeat != ReminderRepeat.ONCE) {
            remindAt = ReminderSchedule.nextOccurrence(repeat, remindAt, LocalDateTime.now());
        }

        if (repeat != ReminderRepeat.ONCE && request.done() && !wasDone) {
            reminder.setRemindAt(ReminderSchedule.advanceAfterAck(repeat, remindAt, LocalDateTime.now()));
            reminder.setDone(false);
            return;
        }

        reminder.setRemindAt(remindAt);
        reminder.setDone(repeat == ReminderRepeat.ONCE && request.done());
    }

    private ReminderResponse toResponse(Reminder reminder) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRemindAt = ReminderSchedule.nextOccurrence(
            reminder.getRepeatType(),
            reminder.getRemindAt(),
            now
        );
        return new ReminderResponse(
            reminder.getId(),
            reminder.getTitle(),
            reminder.getNote(),
            reminder.getRemindAt(),
            reminder.getRepeatType(),
            nextRemindAt,
            reminder.isDone(),
            reminder.getCreatedAt(),
            reminder.getUpdatedAt()
        );
    }
}
