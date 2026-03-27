package org.darius.payment.services.impls;

import org.darius.payment.services.PaymentReferenceService;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PaymentReferenceServiceImpl implements PaymentReferenceService {

    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public String generateReference() {
        long seq = sequence.incrementAndGet();
        return String.format("PAY-%d-%05d", Year.now().getValue(), seq);
    }
}