package com.subtrack.enums;

import java.time.LocalDate;

public enum Frequency {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    ANNUAL("Annual");

    private final String displayName;

    Frequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The date {@code periods} billing periods after {@code start} (computed from start, so month ends don't drift). */
    public LocalDate addPeriods(LocalDate start, long periods) {
        return switch (this) {
            case WEEKLY -> start.plusWeeks(periods);
            case MONTHLY -> start.plusMonths(periods);
            case QUARTERLY -> start.plusMonths(3 * periods);
            case ANNUAL -> start.plusYears(periods);
        };
    }

    public double getMonthlyMultiplier() {
        return switch (this) {
            case WEEKLY -> 4.33;
            case MONTHLY -> 1.0;
            case QUARTERLY -> 1.0 / 3.0;
            case ANNUAL -> 1.0 / 12.0;
        };
    }

    public double getAnnualMultiplier() {
        return switch (this) {
            case WEEKLY -> 52.0;
            case MONTHLY -> 12.0;
            case QUARTERLY -> 4.0;
            case ANNUAL -> 1.0;
        };
    }
}
