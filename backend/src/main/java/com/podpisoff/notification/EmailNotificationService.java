package com.podpisoff.notification;

import com.podpisoff.settings.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    public EmailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    MailProperties mailProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
    }

    public boolean isConfigured() {
        return mailProperties.enabled() && mailSenderProvider.getIfAvailable() != null;
    }

    public void send(String to, String subject, String body) {
        if (!isConfigured() || to == null || to.isBlank()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to.trim());
            message.setFrom(mailProperties.from());
            message.setSubject(subject);
            message.setText(body == null ? "" : body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send email notification to {}", to, ex);
        }
    }
}
