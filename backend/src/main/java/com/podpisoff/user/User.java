package com.podpisoff.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "recovery_key_hash", nullable = false, length = 255)
    private String recoveryKeyHash;

    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Plan plan = Plan.FREE;

    @Column(name = "plan_expires_at")
    private LocalDateTime planExpiresAt;

    @Column(nullable = false, length = 64)
    private String timezone = "Europe/Moscow";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private LocaleCode locale = LocaleCode.RU;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted;

    @Column(name = "billing_reminder_days_before", nullable = false)
    private int billingReminderDaysBefore = 3;

    @Column(name = "blocked_permanently", nullable = false)
    private boolean blockedPermanently = false;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = false;

    @Column(name = "telegram_notifications_enabled", nullable = false)
    private boolean telegramNotificationsEnabled = false;

    @Column(name = "telegram_chat_id", length = 32)
    private String telegramChatId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRecoveryKeyHash() {
        return recoveryKeyHash;
    }

    public void setRecoveryKeyHash(String recoveryKeyHash) {
        this.recoveryKeyHash = recoveryKeyHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public LocalDateTime getPlanExpiresAt() {
        return planExpiresAt;
    }

    public void setPlanExpiresAt(LocalDateTime planExpiresAt) {
        this.planExpiresAt = planExpiresAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public LocaleCode getLocale() {
        return locale;
    }

    public void setLocale(LocaleCode locale) {
        this.locale = locale;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }

    public int getBillingReminderDaysBefore() {
        return billingReminderDaysBefore;
    }

    public void setBillingReminderDaysBefore(int billingReminderDaysBefore) {
        this.billingReminderDaysBefore = billingReminderDaysBefore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isBlockedPermanently() {
        return blockedPermanently;
    }

    public void setBlockedPermanently(boolean blockedPermanently) {
        this.blockedPermanently = blockedPermanently;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(LocalDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public boolean isTelegramNotificationsEnabled() {
        return telegramNotificationsEnabled;
    }

    public void setTelegramNotificationsEnabled(boolean telegramNotificationsEnabled) {
        this.telegramNotificationsEnabled = telegramNotificationsEnabled;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public boolean isCurrentlyBlocked() {
        if (blockedPermanently) {
            return true;
        }
        return blockedUntil != null && blockedUntil.isAfter(LocalDateTime.now());
    }
}
