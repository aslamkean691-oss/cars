package com.example.cars.util;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cars.R;

/**
 * ריפוד בטוח מתחת לסטטוס-בר לבר הכתום העליון (כמו ב־{@link com.example.cars.StartActivity2}).
 */
public final class LotToolbarInsets {

    private LotToolbarInsets() {}

    public static void apply(@NonNull AppCompatActivity activity, @NonNull View toolbarRoot) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbarRoot, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            int extra = activity.getResources().getDimensionPixelSize(R.dimen.app_bar_padding_top_extra);
            v.setPadding(v.getPaddingLeft(), insets.top + extra, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(toolbarRoot);
    }
}
