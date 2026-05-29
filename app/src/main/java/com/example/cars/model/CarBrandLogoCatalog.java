package com.example.cars.model;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cars.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * לוגו יצרן לרשימות: סילואט אחד + צבע ייחודי לכל מותג (יצרנים בינלאומיים נפוצים).
 * <p>
 * תמונה מותאמת אישית: הוסף ב־{@code res/drawable/} קובץ בשם {@code brand_logo_&lt;key&gt;}
 * (אותיות קטנות וקו תחתון כמו המפתח ב־{@code add}, למשל {@code brand_logo_mazda.png}
 * או {@code brand_logo_land_rover.xml}). אם הקובץ קיים — הוא יוצג במקום הסילואט הצבוע.
 */
public final class CarBrandLogoCatalog {

    public static final String KEY_AUTO = "auto";

    public static final class Entry {
        public final String key;
        public final String labelHe;
        @ColorInt
        public final int color;

        public Entry(String key, String labelHe, int color) {
            this.key = key;
            this.labelHe = labelHe;
            this.color = color;
        }
    }

    private static final Map<String, Entry> BY_KEY = new LinkedHashMap<>();
    private static final List<Entry> ORDERED = new ArrayList<>();

    static {
        add("toyota", "טויוטה", 0xFFE53935);
        add("mazda", "מאזדה", 0xFFB71C1C);
        add("honda", "הונדה", 0xFFE65100);
        add("nissan", "ניסאן", 0xFFC62828);
        add("hyundai", "יונדאי", 0xFF1565C0);
        add("kia", "קיה", 0xFF0D47A1);
        add("subaru", "סובארו", 0xFF283593);
        add("mitsubishi", "מיצובישי", 0xFFD84315);
        add("suzuki", "סוזוקי", 0xFF00695C);
        add("vw", "פולקסווגן", 0xFF37474F);
        add("bmw", "ב.מ.וו", 0xFF0277BD);
        add("mercedes", "מרצדס", 0xFF424242);
        add("audi", "אאודי", 0xFF212121);
        add("skoda", "סקודה", 0xFF2E7D32);
        add("seat", "סיאט", 0xFFC2185B);
        add("cupra", "קופרה", 0xFF6A1B9A);
        add("peugeot", "פג׳ו", 0xFF1565C0);
        add("renault", "רנו", 0xFFFF6F00);
        add("citroen", "סיטרואן", 0xFF00838F);
        add("fiat", "פיאט", 0xFFAD1457);
        add("ford", "פורד", 0xFF0D47A1);
        add("chevrolet", "שברולט", 0xFFD32F2F);
        add("tesla", "טסלה", 0xFFE53935);
        add("volvo", "וולוו", 0xFF0277BD);
        add("lexus", "לקסוס", 0xFF1A237E);
        add("infiniti", "אינפיניטי", 0xFF37474F);
        add("genesis", "ג׳נסיס", 0xFF263238);
        add("porsche", "פורשה", 0xFF424242);
        add("jaguar", "יגואר", 0xFF1B5E20);
        add("land_rover", "לנד רובר", 0xFF33691E);
        add("mini", "מיני", 0xFFAD1457);
        add("alfa", "אלפא רומיאו", 0xFFB71C1C);
        add("dacia", "דאצ׳יה", 0xFF6D4C41);
        add("isuzu", "איסוזו", 0xFF37474F);
        add("opel", "אופל", 0xFFFFC107);
        add("mg", "MG", 0xFFE64A19);
        add("byd", "BYD", 0xFF00897B);
        add("geely", "ג׳ילי", 0xFF3949AB);
        add("chery", "צ׳רי", 0xFFC62828);
        add("greatwall", "גרייט וול", 0xFF5D4037);
        add("generic", "אחר / כללי", 0xFF78909C);
    }

    private static void add(String key, String labelHe, int color) {
        Entry e = new Entry(key, labelHe, color | 0xFF000000);
        BY_KEY.put(key, e);
        ORDERED.add(e);
    }

    @Nullable
    public static Entry forKey(@Nullable String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return BY_KEY.get(key.trim().toLowerCase(Locale.ROOT));
    }

    @NonNull
    public static List<Entry> pickableEntries() {
        return Collections.unmodifiableList(ORDERED);
    }

    /**
     * מזהה drawable מותאם {@code R.drawable.brand_logo_<key>} או 0 אם אין קובץ כזה.
     */
    public static int brandLogoDrawableRes(@NonNull Context context, @Nullable String brandLogoKey) {
        if (brandLogoKey == null || brandLogoKey.trim().isEmpty()) {
            return 0;
        }
        String k = brandLogoKey.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (!k.matches("[a-z0-9_]+")) {
            return 0;
        }
        String name = "brand_logo_" + k;
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        return id != 0 ? id : 0;
    }

    /**
     * מציג לוגו צבוע או אימוג׳י ישן או ברירת מחדל לפי סוג רכב.
     */
    public static void bindListMark(
            @NonNull ImageView ivSilhouette,
            @NonNull TextView tvEmoji,
            @NonNull CarListing c) {
        tvEmoji.setVisibility(View.GONE);
        ivSilhouette.clearColorFilter();
        ivSilhouette.setVisibility(View.VISIBLE);

        int custom = brandLogoDrawableRes(ivSilhouette.getContext(), c.getBrandLogoKey());
        if (custom != 0) {
            ivSilhouette.setImageResource(custom);
            ivSilhouette.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return;
        }

        Entry logo = forKey(c.getBrandLogoKey());
        if (logo != null) {
            ivSilhouette.setImageResource(R.drawable.ic_car_brand_silhouette);
            ivSilhouette.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivSilhouette.setColorFilter(new PorterDuffColorFilter(logo.color, PorterDuff.Mode.MULTIPLY));
            return;
        }

        String em = c.getListIconEmoji();
        if (em != null && !em.trim().isEmpty()) {
            ivSilhouette.setImageDrawable(null);
            ivSilhouette.setVisibility(View.GONE);
            tvEmoji.setVisibility(View.VISIBLE);
            tvEmoji.setText(em.trim());
            return;
        }

        ivSilhouette.setImageResource(R.drawable.ic_car_brand_silhouette);
        ivSilhouette.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int fallback = CarTypeIcons.colorForVehicleType(c.getVehicleType());
        ivSilhouette.setColorFilter(new PorterDuffColorFilter(fallback | 0xFF000000, PorterDuff.Mode.MULTIPLY));
    }
}
