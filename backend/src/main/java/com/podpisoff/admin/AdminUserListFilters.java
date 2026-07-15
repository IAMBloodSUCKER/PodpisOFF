package com.podpisoff.admin;

public record AdminUserListFilters(
    String plan,
    String search,
    String emailStatus,
    String telegramStatus
) {
    public static AdminUserListFilters of(String plan, String search, String emailStatus, String telegramStatus) {
        return new AdminUserListFilters(plan, search, emailStatus, telegramStatus);
    }
}
