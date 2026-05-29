package com.example.cars.model;

import androidx.annotation.Nullable;

/**
 * אימוג׳י טקסטואלי לתצוגה כשאין לוגו יצרן נבחר (למשל כותרת במסך פרטים).
 */
public final class CarListDisplayEmoji {

    private CarListDisplayEmoji() {}

    /** אימוג׳י לשורת כותרת טקסט — ריק כשיש לוגו יצרן או אימוג׳י בשדה נפרד */
    public static String detailTitlePrefix(@Nullable CarListing c) {
        if (c == null) {
            return CarTypeIcons.prefixEmoji(null) + " ";
        }
        if (CarBrandLogoCatalog.forKey(c.getBrandLogoKey()) != null) {
            return "";
        }
        String custom = c.getListIconEmoji();
        if (custom != null && !custom.trim().isEmpty()) {
            return "";
        }
        return CarTypeIcons.prefixEmoji(c.getVehicleType()) + " ";
    }

    /** לתאימות: אימוג׳י לרשימה כשאין עמודת לוגו (מומלץ להשתמש ב־{@link CarBrandLogoCatalog#bindListMark}). */
    public static String forListing(CarListing c) {
        if (c == null) {
            return "🚗";
        }
        if (CarBrandLogoCatalog.forKey(c.getBrandLogoKey()) != null) {
            return "";
        }
        String custom = c.getListIconEmoji();
        if (custom != null && !custom.trim().isEmpty()) {
            return custom.trim();
        }
        return CarTypeIcons.prefixEmoji(c.getVehicleType());
    }
}
