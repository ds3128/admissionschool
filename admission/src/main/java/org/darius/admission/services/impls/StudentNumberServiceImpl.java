package org.darius.admission.services.impls;

import org.darius.admission.services.StudentNumberService;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class StudentNumberServiceImpl implements StudentNumberService {

    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public String generateStudentNumber() {
        long seq = sequence.incrementAndGet();
        return String.format("STU-%d-%05d", Year.now().getValue(), seq);
    }
}
