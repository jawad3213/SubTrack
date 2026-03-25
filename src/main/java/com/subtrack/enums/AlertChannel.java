package com.subtrack.enums;

public enum AlertChannel {
    EMAIL("Email"),
    TELEGRAM("Telegram"),
    WHATSAPP("WhatsApp");

    private final String displayName;

    AlertChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
