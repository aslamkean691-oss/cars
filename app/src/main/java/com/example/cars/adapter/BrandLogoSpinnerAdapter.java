package com.example.cars.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cars.R;
import com.example.cars.model.CarBrandLogoCatalog;

import java.util.List;

/**
 * שורות ספינר לוגו יצרן: אם קיים {@code drawable/brand_logo_<key>} — תמונה; אחרת סילואט צבוע.
 */
public final class BrandLogoSpinnerAdapter extends ArrayAdapter<BrandLogoSpinnerAdapter.Row> {

    public static final class Row {
        @Nullable
        public final String key;
        public final String label;
        @ColorInt
        public final int color;

        public Row(@Nullable String key, String label, @ColorInt int color) {
            this.key = key;
            this.label = label;
            this.color = color | 0xFF000000;
        }
    }

    public BrandLogoSpinnerAdapter(@NonNull Context context, @NonNull List<Row> rows) {
        super(context, 0, rows);
        setDropDownViewResource(R.layout.item_brand_logo_spinner_dropdown);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_brand_logo_spinner, parent, false);
        }
        bind(v, getItem(position));
        return v;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_brand_logo_spinner_dropdown, parent, false);
        }
        bind(v, getItem(position));
        return v;
    }

    private static void bind(@NonNull View v, @Nullable Row row) {
        ImageView iv = v.findViewById(R.id.ivBrandLogoSpin);
        TextView tv = v.findViewById(R.id.tvBrandLogoSpin);
        if (row == null) {
            iv.setImageDrawable(null);
            tv.setText("");
            return;
        }
        Context ctx = v.getContext();
        int custom = row.key != null ? CarBrandLogoCatalog.brandLogoDrawableRes(ctx, row.key) : 0;
        if (custom != 0) {
            iv.setImageResource(custom);
            iv.clearColorFilter();
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            iv.setImageResource(R.drawable.ic_car_brand_silhouette);
            iv.clearColorFilter();
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setColorFilter(new PorterDuffColorFilter(row.color, PorterDuff.Mode.MULTIPLY));
        }
        tv.setText(row.label);
    }
}
