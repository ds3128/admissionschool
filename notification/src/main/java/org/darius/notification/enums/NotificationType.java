package org.darius.notification.enums;

public enum NotificationType {

    // ── Admission (10) ────────────────────────────────────────────────────────
    APPLICATION_SUBMITTED,
    APPLICATION_ADMIN_REVIEW,
    APPLICATION_PENDING_COMMISSION,
    INTERVIEW_SCHEDULED,
    THESIS_APPROVAL_REQUESTED,
    APPLICATION_AWAITING_CONFIRMATION,
    APPLICATION_ACCEPTED,
    APPLICATION_REJECTED,
    WAITLIST_PROMOTED,
    CHOICE_AUTO_CONFIRMED,

    // ── Payment (8) ───────────────────────────────────────────────────────────
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    INVOICE_GENERATED,
    INVOICE_PAID,
    INVOICE_OVERDUE,
    STUDENT_PAYMENT_BLOCKED,
    SCHOLARSHIP_DISBURSED,

    // ── Course (5) ────────────────────────────────────────────────────────────
    STUDENT_ENROLLED,
    ATTENDANCE_THRESHOLD_EXCEEDED,
    GRADES_PUBLISHED,
    SESSION_CANCELLED,
    SEMESTER_VALIDATED
}
