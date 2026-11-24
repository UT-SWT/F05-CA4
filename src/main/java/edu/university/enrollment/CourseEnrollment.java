package edu.university.enrollment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Stateful enrollment, capacity, payment, wait-list and cancellation workflow. */
public class CourseEnrollment {
    public enum Status { NEW, PENDING_PAYMENT, ENROLLED, WAITLISTED, LOCKED, CANCELLED, REJECTED }
    public enum RegistrationResult { PAYMENT_REQUIRED, WAITLISTED, REJECTED, ALREADY_PROCESSED }
    public enum PaymentResult { PAID, FAILED, LOCKED, NOT_PAYABLE }
    public enum CancelResult { CANCELLED, REFUNDED, NOT_CANCELLABLE }

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final int capacity;
    private final long basePrice;
    private final boolean advancedCourse;
    private final Map<String, EnrollmentRecord> records = new LinkedHashMap<>();
    private final Deque<String> waitlist = new ArrayDeque<>();
    private final List<String> auditLog = new ArrayList<>();

    private static final class EnrollmentRecord {
        private final Participant participant;
        private final long payableAmount;
        private Status status;
        private int failedAttempts;
        private long lockedUntilEpochMinute;

        private EnrollmentRecord(Participant participant, long payableAmount, Status status) {
            this.participant = participant;
            this.payableAmount = payableAmount;
            this.status = status;
        }
    }

    public CourseEnrollment(int capacity, long basePrice, boolean advancedCourse) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        if (basePrice < 0) throw new IllegalArgumentException("basePrice cannot be negative");
        this.capacity = capacity;
        this.basePrice = basePrice;
        this.advancedCourse = advancedCourse;
    }

    public RegistrationResult register(Participant participant, long nowEpochMinute) {
        Objects.requireNonNull(participant, "participant");
        if (records.containsKey(participant.getId())) return RegistrationResult.ALREADY_PROCESSED;

        String rejection = validateParticipant(participant);
        if (rejection != null) {
            records.put(participant.getId(), new EnrollmentRecord(participant, 0, Status.REJECTED));
            audit("REJECTED:" + participant.getId() + ":" + rejection);
            return RegistrationResult.REJECTED;
        }

        long amount = calculatePayableAmount(participant);
        if (activeEnrollmentCount() >= capacity) {
            EnrollmentRecord record = new EnrollmentRecord(participant, amount, Status.WAITLISTED);
            records.put(participant.getId(), record);
            waitlist.addLast(participant.getId());
            audit("WAITLISTED:" + participant.getId());
            return RegistrationResult.WAITLISTED;
        }

        records.put(participant.getId(), new EnrollmentRecord(participant, amount, Status.PENDING_PAYMENT));
        audit("PAYMENT_REQUIRED:" + participant.getId() + ":" + amount + ":" + nowEpochMinute);
        return RegistrationResult.PAYMENT_REQUIRED;
    }

    /** Returns null when valid, otherwise a stable reason code. */
    private String validateParticipant(Participant p) {
        // TARGET: CFG -- intentionally structured for manual CFG and Edge-Pair analysis.
        if (p.getId().isBlank()) return "EMPTY_ID";
        if (p.getAge() < 16 || p.getAge() > 100) return "AGE_OUT_OF_RANGE";
        if (p.isBlacklisted()) return "BLACKLISTED";
        if (p.getCompletedCourses() < 0) return "INVALID_HISTORY";
        if (!p.satisfiesCoursePrerequisite(advancedCourse)) return "PREREQUISITE";
        if (p instanceof ScholarshipParticipant scholarship) {
            if (scholarship.getScholarshipPercent() < 0 || scholarship.getScholarshipPercent() > 100)
                return "INVALID_SCHOLARSHIP";
        }
        return null;
    }

    public long calculatePayableAmount(Participant participant) {
        Objects.requireNonNull(participant, "participant");
        int discount = Math.min(participant.getMaximumDiscountPercent(), 100);
        // TARGET: CACC -- each clause can independently affect the discount predicate.
        boolean eligibleForLoyaltyDiscount = participant.getCompletedCourses() > 0
                && participant.hasPassedPlacementTest()
                && (participant.getAge() <= 30 || participant instanceof ScholarshipParticipant);
        if (eligibleForLoyaltyDiscount) discount = Math.min(100, discount + 10);
        if (participant instanceof ScholarshipParticipant scholarship) {
            discount = Math.min(100, Math.max(discount, scholarship.getScholarshipPercent()));
        }
        return basePrice - (basePrice * discount / 100);
    }

    public PaymentResult pay(String participantId, boolean paymentAccepted, long nowEpochMinute) {
        EnrollmentRecord record = records.get(participantId);
        if (record == null || record.status != Status.PENDING_PAYMENT && record.status != Status.LOCKED)
            return PaymentResult.NOT_PAYABLE;
        if (record.status == Status.LOCKED) {
            if (nowEpochMinute < record.lockedUntilEpochMinute) return PaymentResult.LOCKED;
            record.status = Status.PENDING_PAYMENT;
            audit("UNLOCKED:" + participantId);
        }
        if (paymentAccepted) {
            record.status = Status.ENROLLED;
            record.failedAttempts = 0;
            audit("PAYMENT_ACCEPTED:" + participantId);
            return PaymentResult.PAID;
        }
        record.failedAttempts++;
        audit("PAYMENT_FAILED:" + participantId + ":" + record.failedAttempts);
        if (record.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            record.status = Status.LOCKED;
            record.lockedUntilEpochMinute = nowEpochMinute + LOCK_DURATION_MINUTES;
            audit("LOCKED:" + participantId + ":" + record.lockedUntilEpochMinute);
            return PaymentResult.LOCKED;
        }
        return PaymentResult.FAILED;
    }

    public CancelResult cancel(String participantId) {
        EnrollmentRecord record = records.get(participantId);
        if (record == null || (record.status != Status.ENROLLED && record.status != Status.PENDING_PAYMENT
                && record.status != Status.WAITLISTED)) return CancelResult.NOT_CANCELLABLE;
        boolean refund = record.status == Status.ENROLLED;
        record.status = Status.CANCELLED;
        waitlist.remove(participantId);
        audit((refund ? "REFUNDED:" : "CANCELLED:") + participantId);
        if (refund) promoteNextWaitlisted();
        return refund ? CancelResult.REFUNDED : CancelResult.CANCELLED;
    }

    private void promoteNextWaitlisted() {
        while (!waitlist.isEmpty()) {
            String nextId = waitlist.removeFirst();
            EnrollmentRecord next = records.get(nextId);
            if (next != null && next.status == Status.WAITLISTED) {
                next.status = Status.PENDING_PAYMENT;
                audit("PROMOTED:" + nextId);
                return;
            }
        }
    }

    private int activeEnrollmentCount() {
        int count = 0;
        for (EnrollmentRecord record : records.values()) {
            if (record.status == Status.PENDING_PAYMENT || record.status == Status.ENROLLED
                    || record.status == Status.LOCKED) count++;
        }
        return count;
    }

    private void audit(String event) { auditLog.add(event); }

    public Status getStatus(String participantId) {
        EnrollmentRecord record = records.get(participantId);
        return record == null ? null : record.status;
    }

    public long getPayableAmount(String participantId) {
        EnrollmentRecord record = records.get(participantId);
        if (record == null) throw new IllegalArgumentException("unknown participant: " + participantId);
        return record.payableAmount;
    }

    public int getFailedAttempts(String participantId) {
        EnrollmentRecord record = records.get(participantId);
        return record == null ? 0 : record.failedAttempts;
    }

    public long getLockedUntil(String participantId) {
        EnrollmentRecord record = records.get(participantId);
        return record == null ? 0 : record.lockedUntilEpochMinute;
    }

    public List<String> getWaitlistSnapshot() { return List.copyOf(waitlist); }
    public List<String> getAuditLog() { return Collections.unmodifiableList(auditLog); }
    public int getCapacity() { return capacity; }
}
