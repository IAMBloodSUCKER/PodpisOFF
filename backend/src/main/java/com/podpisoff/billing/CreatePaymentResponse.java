package com.podpisoff.billing;

public record CreatePaymentResponse(
    String paymentId,
    String paymentUrl,
    String status
) {
}
