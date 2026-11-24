package edu.university.enrollment;

import java.util.Objects;

/** A person requesting a place in a course. */
public class Participant {
    private final String id;
    private final int age;
    private final int completedCourses;
    private final boolean blacklisted;
    private final boolean placementTestPassed;

    public Participant(String id, int age, int completedCourses,
                       boolean blacklisted, boolean placementTestPassed) {
        this.id = Objects.requireNonNull(id, "id").trim();
        this.age = age;
        this.completedCourses = completedCourses;
        this.blacklisted = blacklisted;
        this.placementTestPassed = placementTestPassed;
    }

    public String getId() { return id; }
    public int getAge() { return age; }
    public int getCompletedCourses() { return completedCourses; }
    public boolean isBlacklisted() { return blacklisted; }
    public boolean hasPassedPlacementTest() { return placementTestPassed; }

    /** Maximum discount available to a regular participant. */
    public int getMaximumDiscountPercent() { return completedCourses > 0 ? 15 : 5; }

    /** Regular participants must pass the placement test for an advanced course. */
    public boolean satisfiesCoursePrerequisite(boolean advancedCourse) {
        return !advancedCourse || placementTestPassed;
    }
}
