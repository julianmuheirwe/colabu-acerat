package com.acerat.solidtest.cardpayments;

import java.time.LocalDate;
import java.util.UUID;

public class CardDetails {
    public LocalDate getExpiresAt() {
        return LocalDate.now().plusDays(10);
    }

    public UUID getCardDetailsReference() {
        return null;
    }
}
