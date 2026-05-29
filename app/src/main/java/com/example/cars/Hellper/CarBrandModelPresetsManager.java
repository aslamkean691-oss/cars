package com.example.cars.Hellper;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.example.cars.model.CarBrandModelPreset;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * רשימת יצרן+דגם+סמל לאחרונה — לבחירה מהירה בטופס הוספת רכב.
 */
public final class CarBrandModelPresetsManager {

    private static final String PREFS = "autosoket_car_presets";
    private static final String KEY_LIST = "brand_model_presets_v1";
    private static final int MAX = 24;
    private static final Gson GSON = new Gson();

    private CarBrandModelPresetsManager() {}

    public static List<CarBrandModelPreset> load(Context context) {
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY_LIST, "[]");
        Type type = new TypeToken<ArrayList<CarBrandModelPreset>>() {}.getType();
        List<CarBrandModelPreset> list = GSON.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    /** Same pairing rules as {@link #addOrPromote} (trim + case-insensitive). */
    public static boolean hasMatchingPreset(Context context, String brand, String modelName) {
        String b = brand != null ? brand.trim() : "";
        String m = modelName != null ? modelName.trim() : "";
        if (b.isEmpty() && m.isEmpty()) {
            return false;
        }
        String keyB = b.toLowerCase(Locale.ROOT);
        String keyM = m.toLowerCase(Locale.ROOT);
        for (CarBrandModelPreset x : load(context)) {
            String xb = x.getBrand() != null ? x.getBrand().trim().toLowerCase(Locale.ROOT) : "";
            String xm = x.getModelName() != null ? x.getModelName().trim().toLowerCase(Locale.ROOT) : "";
            if (xb.equals(keyB) && xm.equals(keyM)) {
                return true;
            }
        }
        return false;
    }

    public static void addOrPromote(Context context, String brand, String modelName,
            @Nullable String brandLogoKey, @Nullable String listIconEmoji) {
        if (TextUtils.isEmpty(brand) && TextUtils.isEmpty(modelName)) {
            return;
        }
        String b = brand != null ? brand.trim() : "";
        String m = modelName != null ? modelName.trim() : "";
        if (b.isEmpty() && m.isEmpty()) {
            return;
        }
        String icon = listIconEmoji != null ? listIconEmoji.trim() : "";
        String logoKey = brandLogoKey != null ? brandLogoKey.trim() : "";

        List<CarBrandModelPreset> list = new ArrayList<>(load(context));
        String keyB = b.toLowerCase(Locale.ROOT);
        String keyM = m.toLowerCase(Locale.ROOT);
        for (Iterator<CarBrandModelPreset> it = list.iterator(); it.hasNext(); ) {
            CarBrandModelPreset x = it.next();
            String xb = x.getBrand() != null ? x.getBrand().trim().toLowerCase(Locale.ROOT) : "";
            String xm = x.getModelName() != null ? x.getModelName().trim().toLowerCase(Locale.ROOT) : "";
            if (xb.equals(keyB) && xm.equals(keyM)) {
                it.remove();
            }
        }
        CarBrandModelPreset n = new CarBrandModelPreset(b, m, icon, logoKey.isEmpty() ? null : logoKey);
        list.add(0, n);
        while (list.size() > MAX) {
            list.remove(list.size() - 1);
        }
        save(context, list);
    }

    private static void save(Context context, List<CarBrandModelPreset> list) {
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(KEY_LIST, GSON.toJson(list)).apply();
    }
}
