package com.podpisoff.feedback;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public FeedbackResponse submit(@Valid @RequestBody SubmitFeedbackRequest request) {
        return feedbackService.submit(request);
    }

    @GetMapping("/mine")
    public List<FeedbackResponse> mine() {
        return feedbackService.myFeedback();
    }
}
