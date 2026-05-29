package com.example.cars;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cars.Hellper.AuthSessionManager;
import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.LoadExecutor;
import com.example.cars.Hellper.FavoritesManager;
import com.example.cars.model.AppwriteCollections;
import com.example.cars.model.CarBrandLogoCatalog;
import com.example.cars.model.CarListDisplayEmoji;
import com.example.cars.model.CarListing;
import com.example.cars.model.LotProfile;
import com.example.cars.util.AppwriteGlideUrl;
import com.example.cars.util.LoadingOverlay;
import com.example.cars.util.LotToolbarInsets;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class CarDetailActivity extends AppCompatActivity {

    private static final String TAG = "CarDetailActivity";
    public static final String EXTRA_CAR_ID = "car_document_id";

    private final Executor bg = LoadExecutor.io();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);

        View lotBar = findViewById(R.id.lotToolbarRoot);
        LotToolbarInsets.apply(this, lotBar);
        findViewById(R.id.btnLotToolbarBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        ((TextView) findViewById(R.id.tvLotToolbarTitle)).setText(getString(R.string.screen_car_detail));

        String carId = getIntent().getStringExtra(EXTRA_CAR_ID);
        if (carId == null || carId.isEmpty()) {
            finish();
            return;
        }

        LinearLayout rowImages = findViewById(R.id.rowImages);
        ImageView ivDetailBrandMark = findViewById(R.id.ivDetailBrandMark);
        TextView tvDetailBrandEmoji = findViewById(R.id.tvDetailBrandEmoji);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvBody = findViewById(R.id.tvDetailBody);
        MaterialButton btnWa = findViewById(R.id.btnWhatsapp);
        MaterialButton btnCall = findViewById(R.id.btnCall);
        MaterialButton btnFavorite = findViewById(R.id.btnFavorite);

        DALAppWriteConnection dal = new DALAppWriteConnection(this);
        AuthSessionManager auth = new AuthSessionManager(this);
        auth.restoreDalIfNeeded(dal);

        View loadingOverlay = findViewById(R.id.loadingOverlay);
        LoadingOverlay.show(loadingOverlay);

        bg.execute(() -> {
            DALAppWriteConnection.OperationResult<CarListing> carRes =
                    dal.getDataById(AppwriteCollections.CARS, carId, null, CarListing.class);
            DALAppWriteConnection.OperationResult<ArrayList<LotProfile>> profRes =
                    dal.getData(AppwriteCollections.LOT_PROFILES, null, LotProfile.class);

            CarListing car = carRes.success ? carRes.data : null;
            LotProfile profile = null;
            if (profRes.success && profRes.data != null && car != null) {
                for (LotProfile p : profRes.data) {
                    if (car.getOwnerUserId() != null && car.getOwnerUserId().equals(p.getOwnerUserId())) {
                        profile = p;
                        break;
                    }
                }
                if (profile == null && !profRes.data.isEmpty()) {
                    profile = profRes.data.get(0);
                }
            }

            CarListing finalCar = car;
            LotProfile finalProfile = profile;
            boolean adminSession = auth.hasSession();
            runOnUiThread(() -> {
                try {
                    if (isFinishing()) {
                        return;
                    }
                    if (finalCar == null) {
                        Log.e(TAG, "car not found: " + (carRes.message != null ? carRes.message : ""));
                        finish();
                        return;
                    }

                    if (!adminSession && !finalCar.isDisplayedToCustomer()) {
                        Log.w(TAG, "car hidden from customers");
                        Toast.makeText(this, R.string.car_not_visible_to_customers, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    updateFavoriteButton(btnFavorite, carId);
                btnFavorite.setOnClickListener(v -> {
                    FavoritesManager.toggle(CarDetailActivity.this, carId);
                    updateFavoriteButton(btnFavorite, carId);
                });

                CarBrandLogoCatalog.bindListMark(ivDetailBrandMark, tvDetailBrandEmoji, finalCar);
                String prefix = CarListDisplayEmoji.detailTitlePrefix(finalCar);
                String brand = nz(finalCar.getBrand()).trim();
                String model = nz(finalCar.getModelName()).trim();
                String head = (brand + " " + model).trim();
                if (!head.isEmpty()) {
                    tvTitle.setText(String.format(Locale.getDefault(), "%s%s · %s · %s",
                            prefix, head, nz(finalCar.getVehicleType()), nz(finalCar.getPlateNumber())));
                } else {
                    tvTitle.setText(String.format(Locale.getDefault(), "%s%s · %s",
                            prefix, nz(finalCar.getVehicleType()), nz(finalCar.getPlateNumber())));
                }

                StringBuilder b = new StringBuilder();
                line(b, getString(R.string.detail_label_brand), finalCar.getBrand());
                line(b, getString(R.string.detail_label_model), finalCar.getModelName());
                CarBrandLogoCatalog.Entry logoEntry = CarBrandLogoCatalog.forKey(finalCar.getBrandLogoKey());
                line(b, getString(R.string.detail_label_brand_logo),
                        logoEntry != null ? logoEntry.labelHe : null);
                line(b, getString(R.string.detail_label_list_icon), finalCar.getListIconEmoji());
                line(b, "תת־סוג", finalCar.getSubType());
                line(b, "שנת ייצור", finalCar.getManufactureYear());
                line(b, "צבע", finalCar.getColor());
                line(b, "ק״מ", String.valueOf(finalCar.getKilometer()));
                line(b, "בעלות קודמת", finalCar.getPreviousOwnership());
                line(b, "מחירון", formatMoney(finalCar.getCatalogPrice()));
                line(b, "מחיר בפועל", formatMoney(finalCar.getSalePrice()));
                if (finalCar.getDescription() != null && !finalCar.getDescription().isEmpty()) {
                    b.append("\n").append(finalCar.getDescription());
                }
                tvBody.setText(b.toString());

                List<String> urls = imageUrls(finalCar);
                rowImages.removeAllViews();
                int dp = (int) (160 * getResources().getDisplayMetrics().density);
                for (String u : urls) {
                    ImageView iv = new ImageView(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp, (int) (dp * 0.65f));
                    lp.setMargins(8, 0, 8, 0);
                    iv.setLayoutParams(lp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(this).load(AppwriteGlideUrl.withHeaders(u)).into(iv);
                    rowImages.addView(iv);
                }

                String phone = finalProfile != null ? nz(finalProfile.getPhone()) : "";
                String digits = digitsOnly(phone);

                btnWa.setOnClickListener(v -> {
                    if (digits.isEmpty()) {
                        Log.w(TAG, "whatsapp: no phone in profile");
                        return;
                    }
                    String msg = Uri.encode("שלום, אשמח לקבל פרטים על הרכב: " + nz(finalCar.getPlateNumber()));
                    Uri uri = Uri.parse("https://wa.me/" + digits + "?text=" + msg);
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                });

                btnCall.setOnClickListener(v -> {
                    if (phone.isEmpty()) {
                        Log.w(TAG, "dial: no phone in profile");
                        return;
                    }
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                });
                } finally {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }

    private static void line(StringBuilder b, String label, String value) {
        if (value == null || value.isEmpty()) return;
        b.append(label).append(": ").append(value).append("\n");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String formatMoney(double v) {
        if (v <= 0) return "";
        return String.format(Locale.getDefault(), "%,.0f ₪", v);
    }

    private static List<String> imageUrls(CarListing c) {
        ArrayList<String> u = new ArrayList<>();
        add(u, c.getImageUrl1());
        add(u, c.getImageUrl2());
        add(u, c.getImageUrl3());
        add(u, c.getImageUrl4());
        add(u, c.getImageUrl5());
        add(u, c.getImageUrl6());
        return u;
    }

    private static void add(ArrayList<String> list, String url) {
        if (url != null && !url.trim().isEmpty()) {
            list.add(url.trim());
        }
    }

    private void updateFavoriteButton(MaterialButton btn, String carId) {
        boolean on = FavoritesManager.isFavorite(this, carId);
        btn.setAlpha(on ? 1f : 0.45f);
        btn.setContentDescription(on
                ? getString(R.string.favorite_remove)
                : getString(R.string.favorite_add));
    }

    private static String digitsOnly(String phone) {
        if (phone == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phone.length(); i++) {
            char ch = phone.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
