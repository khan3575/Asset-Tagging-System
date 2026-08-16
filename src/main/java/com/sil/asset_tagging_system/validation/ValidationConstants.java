package com.sil.asset_tagging_system.validation;

/**
 * Shared validation constants used across request DTOs
 * and validation constraints to avoid duplicating regex patterns
 * and size limits.
 */
public final class ValidationConstants {

    public static final String NAME_PATTERN = "^\\p{L}[\\p{L} .'-]*$";
    public static final int NAME_MAX_LENGTH = 60;

    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$";
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 72;

    public static final int EMAIL_MAX_LENGTH = 100;

    private ValidationConstants() {
        // prevent instantiation
    }
}
