package com.example.cars.Hellper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * שומר נתוני כניסה ב-SharedPreferences: טוקן סשן (JWT Appwrite או טוקן מקומי לסשן מטבלה), userId, email.
 */
public final class AuthSessionManager {

    private static final String PREFS = "autosoket_auth";
    private static final String KEY_JWT = "session_jwt";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "user_email";

    private final SharedPreferences prefs;

    public AuthSessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasSession() {
        String jwt = prefs.getString(KEY_JWT, null);
        return jwt != null && !jwt.isEmpty();
    }

    public String getSessionJwt() {
        return prefs.getString(KEY_JWT, "");
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void saveSession(String jwt, String userId, String email) {
        prefs.edit()
                .putString(KEY_JWT, jwt != null ? jwt : "")
                .putString(KEY_USER_ID, userId != null ? userId : "")
                .putString(KEY_EMAIL, email != null ? email : "")
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    /** משחזר JWT ב-DAL אם קיימת כניסה שמורה ב-SharedPreferences */
    public void restoreDalIfNeeded(DALAppWriteConnection dal) {
        if (hasSession()) {
            dal.restoreSessionFromJwt(getSessionJwt(), getUserId(), getEmail());
        }
    }
}
