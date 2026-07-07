package com.podpisoff.admin;

public record AdminBlockUserRequest(
    boolean permanent,
    Integer hours
) {
}
