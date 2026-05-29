package com.example.cars.util;

import android.view.View;

import androidx.annotation.Nullable;

public final class LoadingOverlay {

    private LoadingOverlay() {}

    public static void show(@Nullable View overlay) {
        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
        }
    }

    public static void hide(@Nullable View overlay) {
        if (overlay != null) {
            overlay.setVisibility(View.GONE);
        }
    }
}
