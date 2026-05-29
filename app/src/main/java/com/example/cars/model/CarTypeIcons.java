package com.example.cars.model;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * סמל טקסטואלי לפי סוג רכב (לרשימות).
 */
public final class CarTypeIcons {

    private CarTypeIcons() {}

    /** אימוג׳ים לבחירה בטופס (בנוסף ל־״לפי סוג״). */
    public static List<String> pickableListEmojis() {
        return Collections.unmodifiableList(Arrays.asList(
                "🚗", "🚙", "🚕", "🚐", "🚚", "🛻", "🏎", "🏍", "🛵", "✨", "⭐", "🔷"));
    }

    public static String prefixEmoji(String vehicleType) {
        if (vehicleType == null || vehicleType.isEmpty()) {
            return "🚗";
        }
        String t = vehicleType.toLowerCase();
        if (t.contains("suv")) {
            return "🚙";
        }
        if (t.contains("קטנוע") || t.contains("אופנוע")) {
            return "🏍";
        }
        if (t.contains("מסחר") || t.contains("טנדר") || t.contains("פיקאפ")) {
            return "🚚";
        }
        if (t.contains("משפח") || t.contains("מיני")) {
            return "🚐";
        }
        if (t.contains("ספורט") || t.contains("קופה")) {
            return "🏎";
        }
        if (t.contains("יוקר")) {
            return "✨";
        }
        return "🚗";
    }

    /** צבע לסילואט לפי סוג רכב (כשאין לוגו יצרן נבחר). */
    @ColorInt
    public static int colorForVehicleType(@Nullable String vehicleType) {
        if (vehicleType == null || vehicleType.isEmpty()) {
            return 0xFF78909C;
        }
        String t = vehicleType.toLowerCase();
        if (t.contains("suv")) {
            return 0xFF546E7A;
        }
        if (t.contains("קטנוע") || t.contains("אופנוע")) {
            return 0xFF6D4C41;
        }
        if (t.contains("מסחר") || t.contains("טנדר") || t.contains("פיקאפ")) {
            return 0xFF455A64;
        }
        if (t.contains("משפח") || t.contains("מיני")) {
            return 0xFF0277BD;
        }
        if (t.contains("ספורט") || t.contains("קופה")) {
            return 0xFFC62828;
        }
        if (t.contains("יוקר")) {
            return 0xFF5E35B1;
        }
        return 0xFF78909C;
    }
}
