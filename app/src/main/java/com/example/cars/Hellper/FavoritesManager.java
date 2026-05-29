package com.example.cars.Hellper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * מועדפים ללקוח — מזהי מסמכי רכב ב-SharedPreferences (ללא שרת).
 */
public final class FavoritesManager {

    private static final String PREFS = "autosoket_favorites";
    private static final String KEY_IDS = "car_document_ids";

    private FavoritesManager() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isFavorite(Context ctx, String carDocumentId) {
        if (carDocumentId == null || carDocumentId.isEmpty()) {
            return false;
        }
        Set<String> set = prefs(ctx).getStringSet(KEY_IDS, Collections.emptySet());
        return set != null && set.contains(carDocumentId);
    }

    public static void setFavorite(Context ctx, String carDocumentId, boolean favorite) {
        if (carDocumentId == null || carDocumentId.isEmpty()) {
            return;
        }
        Set<String> cur = prefs(ctx).getStringSet(KEY_IDS, Collections.emptySet());
        HashSet<String> next = new HashSet<>(cur != null ? cur : Collections.emptySet());
        if (favorite) {
            next.add(carDocumentId);
        } else {
            next.remove(carDocumentId);
        }
        prefs(ctx).edit().putStringSet(KEY_IDS, next).apply();
    }

    public static void toggle(Context ctx, String carDocumentId) {
        setFavorite(ctx, carDocumentId, !isFavorite(ctx, carDocumentId));
    }

    public static Set<String> getFavoriteIds(Context ctx) {
        Set<String> s = prefs(ctx).getStringSet(KEY_IDS, Collections.emptySet());
        return s != null ? new HashSet<>(s) : new HashSet<>();
    }
}
