package org.darius.payment.services;

public interface PaymentReferenceService {
    /** Génère une référence unique PAY-{year}-{seq}. Thread-safe. */
    String generateReference();
}