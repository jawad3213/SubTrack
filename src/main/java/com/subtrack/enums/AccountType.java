package com.subtrack.enums;

public enum AccountType {
    B2C("Individual"),
    FREELANCE("Freelance"),
    B2B("Business");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
