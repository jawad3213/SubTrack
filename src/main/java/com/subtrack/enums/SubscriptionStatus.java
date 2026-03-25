package com.subtrack.enums;

public enum SubscriptionStatus {
    ACTIVE("Active"),
    PAUSED("Paused"),
    CANCELLED("Cancelled");

    private final String displayName;

    SubscriptionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(SubscriptionStatus newStatus) {
        return switch (this) {
            case ACTIVE -> newStatus == PAUSED || newStatus == CANCELLED;
            case PAUSED -> newStatus == ACTIVE || newStatus == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
