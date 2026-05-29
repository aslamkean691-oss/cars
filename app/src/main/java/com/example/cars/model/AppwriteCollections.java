package com.example.cars.model;

public final class AppwriteCollections {
    public static final String CARS = "cars";
    public static final String BRANCHES = "branches";
    public static final String LOT_PROFILES = "lot_profiles";
    /** מסמך למנהל ראשי: userId, email, hasChangedDefaultPassword */
    public static final String ADMIN_SECURITY = "admin_security";
    /** התחברות מנהלים: email, password (ללא Appwrite Auth) */
    public static final String ADMIN_ACCOUNTS = "admin_accounts";

    private AppwriteCollections() {}
}
