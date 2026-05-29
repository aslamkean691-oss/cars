package com.example.cars.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cars.CarDetailActivity;
import com.example.cars.Hellper.FavoritesManager;
import com.example.cars.R;
import com.example.cars.model.CarBrandLogoCatalog;
import com.example.cars.model.CarListing;
import com.example.cars.util.AppwriteGlideUrl;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.Holder> {

    public interface AdminCarActionsListener {
        void onEditCar(@NonNull CarListing car);

        void onMarkSold(@NonNull CarListing car);
    }

    private final List<CarListing> items = new ArrayList<>();
    private final boolean adminMode;
    @Nullable
    private final AdminCarActionsListener adminListener;
    private final boolean showSoldIsraelBadge;
    @Nullable
    private Runnable favoriteChangeListener;
    private final Map<String, String> branchIdToLabel = new HashMap<>();

    public CarAdapter() {
        this(false, null, false);
    }

    public CarAdapter(boolean adminMode, @Nullable AdminCarActionsListener adminListener) {
        this(adminMode, adminListener, false);
    }

    public CarAdapter(boolean adminMode, @Nullable AdminCarActionsListener adminListener,
            boolean showSoldIsraelBadge) {
        this.adminMode = adminMode;
        this.adminListener = adminListener;
        this.showSoldIsraelBadge = showSoldIsraelBadge;
    }

    /** יקרא אחרי שינוי מועדף (למשל לרענון רשימת המועדפים). */
    public void setFavoriteChangeListener(@Nullable Runnable listener) {
        favoriteChangeListener = listener;
    }

    public void setBranchIdToLabelMap(@Nullable Map<String, String> map) {
        branchIdToLabel.clear();
        if (map != null) {
            branchIdToLabel.putAll(map);
        }
    }

    public void setItems(List<CarListing> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_car, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CarListing c = items.get(position);
        Context ctx = h.itemView.getContext();
        h.soldBadge.setVisibility(showSoldIsraelBadge && c.isSold() ? View.VISIBLE : View.GONE);

        CarBrandLogoCatalog.bindListMark(h.ivBrandMark, h.tvBrandEmoji, c);
        h.title.setText(titleBrandModel(c));
        String meta = metaLine(c);
        if (meta.isEmpty()) {
            h.meta.setVisibility(View.GONE);
        } else {
            h.meta.setVisibility(View.VISIBLE);
            h.meta.setText(meta);
        }
        String branchLabel = branchLabelFor(c);
        if (branchLabel.isEmpty()) {
            h.tvBranch.setVisibility(View.GONE);
        } else {
            h.tvBranch.setVisibility(View.VISIBLE);
            h.tvBranch.setText(ctx.getString(R.string.car_card_branch, branchLabel));
        }
        bindPrice(h.price, c.getSalePrice());
        bindKmHint(h.kmHint, c.getKilometer());

        String img = c.getPrimaryImageUrl();
        if (!img.isEmpty()) {
            h.image.setAlpha(1f);
            h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(ctx)
                    .load(AppwriteGlideUrl.withHeaders(img))
                    .centerCrop()
                    .placeholder(R.drawable.bg_car_grid_placeholder)
                    .into(h.image);
        } else {
            Glide.with(ctx).clear(h.image);
            h.image.setScaleType(ImageView.ScaleType.CENTER);
            h.image.setImageResource(R.drawable.ic_car_toolbar);
            h.image.setAlpha(0.28f);
        }

        boolean showFavorite = !adminMode && !showSoldIsraelBadge;
        if (showFavorite && c.getId() != null && !c.getId().isEmpty()) {
            h.btnFavorite.setVisibility(View.VISIBLE);
            bindFavoriteButton(h.btnFavorite, c.getId(), ctx);
            h.btnFavorite.setOnClickListener(v -> {
                FavoritesManager.toggle(v.getContext(), c.getId());
                bindFavoriteButton(h.btnFavorite, c.getId(), v.getContext());
                if (favoriteChangeListener != null) {
                    favoriteChangeListener.run();
                }
            });
        } else {
            h.btnFavorite.setVisibility(View.GONE);
            h.btnFavorite.setOnClickListener(null);
        }

        boolean showAdmin = adminMode && adminListener != null;
        h.adminRow.setVisibility(showAdmin ? View.VISIBLE : View.GONE);
        if (showAdmin) {
            h.btnEdit.setOnClickListener(v -> adminListener.onEditCar(c));
            h.btnMarkSold.setVisibility(c.isSold() ? View.GONE : View.VISIBLE);
            h.btnMarkSold.setOnClickListener(v -> adminListener.onMarkSold(c));
        }

        h.itemView.setOnClickListener(v -> {
            if (c.getId() == null) return;
            Intent i = new Intent(v.getContext(), CarDetailActivity.class);
            i.putExtra(CarDetailActivity.EXTRA_CAR_ID, c.getId());
            v.getContext().startActivity(i);
        });
    }

    private static void bindFavoriteButton(MaterialButton btn, String carId, Context ctx) {
        boolean on = FavoritesManager.isFavorite(ctx, carId);
        btn.setIconResource(on ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        btn.setContentDescription(ctx.getString(on ? R.string.favorite_remove : R.string.favorite_add));
    }

    private static void bindPrice(TextView tv, double salePrice) {
        if (salePrice > 0) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(String.format(Locale.getDefault(), "₪%,.0f", salePrice));
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText("—");
        }
    }

    private static void bindKmHint(TextView tv, int km) {
        if (km > 0) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(String.format(Locale.getDefault(), "%,d ק״מ", km));
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String titleBrandModel(CarListing c) {
        String brand = nullToEmpty(c.getBrand()).trim();
        String model = nullToEmpty(c.getModelName()).trim();
        String both = (brand + " " + model).trim();
        if (!both.isEmpty()) {
            return both;
        }
        String type = nullToEmpty(c.getVehicleType()).trim();
        if (!type.isEmpty()) {
            return type;
        }
        return nullToEmpty(c.getPlateNumber());
    }

    private String branchLabelFor(CarListing c) {
        String bid = c.getBranchId();
        if (bid == null || bid.isEmpty()) {
            return "";
        }
        String label = branchIdToLabel.get(bid);
        return label != null ? label : "";
    }

    private static String metaLine(CarListing c) {
        String year = nullToEmpty(c.getManufactureYear()).trim();
        String type = nullToEmpty(c.getVehicleType()).trim();
        String plate = nullToEmpty(c.getPlateNumber()).trim();
        List<String> parts = new ArrayList<>();
        if (!year.isEmpty()) {
            parts.add(year);
        }
        if (!type.isEmpty()) {
            parts.add(type);
        }
        if (!plate.isEmpty()) {
            parts.add(plate);
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join(" · ", parts);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView ivBrandMark;
        final TextView tvBrandEmoji;
        final ImageView image;
        final TextView soldBadge;
        final TextView title;
        final TextView meta;
        final TextView tvBranch;
        final TextView price;
        final TextView kmHint;
        final MaterialButton btnFavorite;
        final LinearLayout adminRow;
        final MaterialButton btnEdit;
        final MaterialButton btnMarkSold;

        Holder(@NonNull View itemView) {
            super(itemView);
            ivBrandMark = itemView.findViewById(R.id.ivBrandListMark);
            tvBrandEmoji = itemView.findViewById(R.id.tvBrandListEmoji);
            image = itemView.findViewById(R.id.ivCarThumb);
            soldBadge = itemView.findViewById(R.id.tvSoldIsraelBadge);
            title = itemView.findViewById(R.id.tvCarTitle);
            meta = itemView.findViewById(R.id.tvCarMeta);
            tvBranch = itemView.findViewById(R.id.tvCarBranch);
            price = itemView.findViewById(R.id.tvCarPrice);
            kmHint = itemView.findViewById(R.id.tvCarKmHint);
            btnFavorite = itemView.findViewById(R.id.btnCarFavorite);
            adminRow = itemView.findViewById(R.id.rowAdminCarActions);
            btnEdit = itemView.findViewById(R.id.btnCarEdit);
            btnMarkSold = itemView.findViewById(R.id.btnCarMarkSold);
        }
    }
}
