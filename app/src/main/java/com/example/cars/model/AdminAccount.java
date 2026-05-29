package com.example.cars.model;

import com.google.gson.annotations.SerializedName;

/**
 * שורת מנהל באוסף Appwrite — התחברות לפי אימייל וסיסמה (ללא Auth).
 * יש ליצור אוסף {@code admin_accounts} עם שדות string: {@code email}, {@code password}.
 */
public class AdminAccount {

    @SerializedName("$id")
    private String id;
    private String email;
    private String password;

    public AdminAccount() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
