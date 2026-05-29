package com.example.cars.model;

import com.google.gson.annotations.SerializedName;

/**
 * מצב אבטחה למנהל ראשי — נשמר ב-Appwrite (אוסף admin_security).
 */
public class AdminSecurityState {

    @SerializedName("$id")
    private String id;

    private String userId;
    private String email;
    /** false = עדיין בסיסמה ברירת המחדל — להציג תזכורת */
    private boolean hasChangedDefaultPassword;

    public AdminSecurityState() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isHasChangedDefaultPassword() {
        return hasChangedDefaultPassword;
    }

    public void setHasChangedDefaultPassword(boolean hasChangedDefaultPassword) {
        this.hasChangedDefaultPassword = hasChangedDefaultPassword;
    }
}
