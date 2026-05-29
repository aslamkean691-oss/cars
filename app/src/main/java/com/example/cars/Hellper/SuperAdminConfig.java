package com.example.cars.Hellper;

/**
 * מנהל ראשי — האימייל והסיסמה הראשוניים (לשנות אחרי כניסה ראשונה).
 * <p>ההתחברות דרך אוסף {@code admin_accounts} — ערכים אלה משמשים לזריעה אוטומטית בפעם הראשונה.</p>
 */
public final class SuperAdminConfig {

    public static final String EMAIL = "admin@admin";
    /** כינוי לאותו מנהל (שורת זריעה נפרת באוסף). */
    public static final String EMAIL_ALIAS = "admin@admin.com";
    /** סיסמת ברירת מחדל בשורות הזריעה — לשנות אחרי כניסה ראשונה. */
    public static final String DEFAULT_PASSWORD = "admin1234";

    private SuperAdminConfig() {}

    public static boolean isSuperAdminEmail(String email) {
        if (email == null) {
            return false;
        }
        String t = email.trim();
        return EMAIL.equalsIgnoreCase(t) || EMAIL_ALIAS.equalsIgnoreCase(t);
    }
}
