package com.podpisoff.feedback;

import com.podpisoff.common.ApiException;
import com.podpisoff.common.AuthFacade;
import com.podpisoff.notification.NotificationService;
import com.podpisoff.notification.NotificationType;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.User;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AuthFacade authFacade;
    private final NotificationService notificationService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           AuthFacade authFacade,
                           NotificationService notificationService) {
        this.feedbackRepository = feedbackRepository;
        this.authFacade = authFacade;
        this.notificationService = notificationService;
    }

    @Transactional
    public FeedbackResponse submit(SubmitFeedbackRequest request) {
        User user = authFacade.getCurrentUser();
        String message = request.message().trim();
        if (message.length() < 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feedback message is too short");
        }
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setMessage(message);
        feedback.setKind(request.kind());
        feedback = feedbackRepository.save(feedback);
        return toResponse(feedback);
    }

    public List<FeedbackResponse> myFeedback() {
        User user = authFacade.getCurrentUser();
        return feedbackRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminFeedbackResponse> listAllForAdmin() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toAdminResponse)
            .toList();
    }

    @Transactional
    public AdminFeedbackResponse reply(Long feedbackId, AdminReplyRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feedback not found"));
        if (feedback.isReplied()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feedback already has a reply");
        }
        String reply = request.reply().trim();
        if (reply.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reply is too short");
        }
        feedback.setAdminReply(reply);
        feedback.setAdminRepliedAt(Instant.now());

        User user = feedback.getUser();
        boolean russian = user.getLocale() == LocaleCode.RU;
        boolean support = feedback.getKind() == FeedbackKind.SUPPORT;
        String title = support
            ? (russian ? "💬 Ответ поддержки" : "💬 Support reply")
            : (russian ? "📝 Ответ на ваш отзыв" : "📝 Reply to your feedback");
        notificationService.create(
            user,
            NotificationType.FEEDBACK_REPLY,
            title,
            reply,
            feedback.getId()
        );
        return toAdminResponse(feedback);
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
            feedback.getId(),
            feedback.getMessage(),
            feedback.getCreatedAt(),
            feedback.getAdminReply(),
            feedback.getAdminRepliedAt()
        );
    }

    private AdminFeedbackResponse toAdminResponse(Feedback feedback) {
        return new AdminFeedbackResponse(
            feedback.getId(),
            feedback.getUser().getId(),
            feedback.getUser().getUsername(),
            feedback.getMessage(),
            feedback.getKind(),
            feedback.getCreatedAt(),
            feedback.getAdminReply(),
            feedback.getAdminRepliedAt()
        );
    }
}
