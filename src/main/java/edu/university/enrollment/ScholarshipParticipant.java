package edu.university.enrollment;

/** A participant whose scholarship changes pricing and prerequisite rules. */
public final class ScholarshipParticipant extends Participant {
    private final int scholarshipPercent;
    private final boolean prerequisiteWaiver;

    public ScholarshipParticipant(String id, int age, int completedCourses,
                                  boolean blacklisted, boolean placementTestPassed,
                                  int scholarshipPercent, boolean prerequisiteWaiver) {
        super(id, age, completedCourses, blacklisted, placementTestPassed);
        this.scholarshipPercent = scholarshipPercent;
        this.prerequisiteWaiver = prerequisiteWaiver;
    }

    public int getScholarshipPercent() { return scholarshipPercent; }
    public boolean hasPrerequisiteWaiver() { return prerequisiteWaiver; }

    @Override
    public int getMaximumDiscountPercent() {
        return Math.max(super.getMaximumDiscountPercent(), scholarshipPercent);
    }

    @Override
    public boolean satisfiesCoursePrerequisite(boolean advancedCourse) {
        return super.satisfiesCoursePrerequisite(advancedCourse) || prerequisiteWaiver;
    }
}
