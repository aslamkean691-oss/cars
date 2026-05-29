package com.example.cars;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cars.Hellper.AuthSessionManager;
import com.example.cars.Hellper.CarBrandModelPresetsManager;
import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.LoadExecutor;
import com.example.cars.adapter.BrandLogoSpinnerAdapter;
import com.example.cars.model.AppwriteCollections;
import com.example.cars.model.Branch;
import com.example.cars.model.CarBrandLogoCatalog;
import com.example.cars.model.CarBrandModelPreset;
import com.example.cars.model.CarListing;
import com.example.cars.model.VehicleCatalog;
import com.example.cars.util.AppwriteGlideUrl;
import com.example.cars.util.LoadingOverlay;
import com.example.cars.util.LotToolbarInsets;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class AddCarActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_CAR_ID = "edit_car_document_id";
    public static final String EXTRA_QUICK_ADD = "quick_add_car";

    private static final String TAG = "AddCarActivity";
    private final Executor bg = LoadExecutor.io();
    private DALAppWriteConnection dal;
    private AuthSessionManager auth;
    private Spinner spBranch;
    private Spinner spVehicleType;
    private Spinner spSubType;
    private Spinner spYear;
    private final String[] imageUrls = new String[6];
    private ImageView[] imageViews;
    private int activeSlot = 0;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private final List<String> branchIds = new ArrayList<>();
    private final List<String> branchLabels = new ArrayList<>();
    @Nullable
    private String editingCarId;
    private boolean editingCarSold;
    /** ערך listIconEmoji מהשרת בעריכה — נשמר כי אין שדה אימוג'י בטופס */
    @Nullable
    private String editingListIconEmoji;
    private TextInputEditText etPlate;
    private MaterialAutoCompleteTextView actBrand;
    private MaterialAutoCompleteTextView actModel;
    private TextInputEditText etColor;
    private TextInputEditText etKm;
    private Spinner spPrevOwnership;
    private ArrayAdapter<String> prevOwnershipAdapter;
    private TextInputEditText etDesc;
    private TextInputEditText etCat;
    private TextInputEditText etSale;
    private SwitchMaterial swDisplayToCustomer;
    private LinearLayout llFullOnly;
    private LinearLayout llImagesRow2;
    private MaterialButton btnSave;
    private ArrayAdapter<String> yearAdapter;
    private boolean quickAdd;
    private boolean vehicleSpinnerProgrammatic;
    private boolean logoSpinnerProgrammatic;
    private Spinner spBrandLogo;
    private BrandLogoSpinnerAdapter logoSpinnerAdapter;
    private ArrayAdapter<String> brandSuggestAdapter;
    private ArrayAdapter<String> modelSuggestAdapter;
    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        View lotBar = findViewById(R.id.lotToolbarRoot);
        LotToolbarInsets.apply(this, lotBar);
        findViewById(R.id.btnLotToolbarBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        ((TextView) findViewById(R.id.tvLotToolbarTitle)).setText(getString(R.string.screen_add_car));

        auth = new AuthSessionManager(this);
        dal = new DALAppWriteConnection(this);
        auth.restoreDalIfNeeded(dal);

        if (!auth.hasSession()) {
            Log.w(TAG, "AddCar: no session, finishing");
            finish();
            return;
        }

        quickAdd = getIntent().getBooleanExtra(EXTRA_QUICK_ADD, false);

        String editExtra = getIntent().getStringExtra(EXTRA_EDIT_CAR_ID);
        editingCarId = (editExtra != null && !editExtra.isEmpty()) ? editExtra : null;

        spBranch = findViewById(R.id.spBranch);
        spVehicleType = findViewById(R.id.spVehicleType);
        spSubType = findViewById(R.id.spSubType);
        spYear = findViewById(R.id.spYear);
        etPlate = findViewById(R.id.etPlate);
        actBrand = findViewById(R.id.actBrand);
        actModel = findViewById(R.id.actModel);
        etColor = findViewById(R.id.etColor);
        etKm = findViewById(R.id.etKm);
        spPrevOwnership = findViewById(R.id.spPreviousOwnership);
        etDesc = findViewById(R.id.etDescription);
        etCat = findViewById(R.id.etCatalogPrice);
        etSale = findViewById(R.id.etSalePrice);
        swDisplayToCustomer = findViewById(R.id.swDisplayToCustomer);
        llFullOnly = findViewById(R.id.llFullOnly);
        llImagesRow2 = findViewById(R.id.llImagesRow2);
        btnSave = findViewById(R.id.btnSaveCar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        LoadingOverlay.show(loadingOverlay);

        if (quickAdd) {
            llFullOnly.setVisibility(View.GONE);
            llImagesRow2.setVisibility(View.GONE);
        }

        spBrandLogo = findViewById(R.id.spBrandLogo);
        wireBrandLogoSpinner();

        brandSuggestAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        actBrand.setAdapter(brandSuggestAdapter);
        modelSuggestAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        actModel.setAdapter(modelSuggestAdapter);
        actBrand.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                rebuildModelSuggestions();
            }
        });
        View.OnFocusChangeListener brandModelFocus = (v, hasFocus) -> {
            if (!hasFocus) {
                maybeAutoAddBrandModelPreset();
            }
        };
        actBrand.setOnFocusChangeListener(brandModelFocus);
        actModel.setOnFocusChangeListener(brandModelFocus);
        refreshPresetsUi();

        setupVehicleSpinners();
        setupYearSpinner();
        setupPreviousOwnershipSpinner();

        imageViews = new ImageView[]{
                findViewById(R.id.ivCar1), findViewById(R.id.ivCar2), findViewById(R.id.ivCar3),
                findViewById(R.id.ivCar4), findViewById(R.id.ivCar5), findViewById(R.id.ivCar6)
        };
        for (int i = 0; i < imageViews.length; i++) {
            int slot = i;
            imageViews[i].setOnClickListener(v -> {
                if (quickAdd && slot >= 3) {
                    return;
                }
                activeSlot = slot;
                pickImageSource();
            });
        }

        registerLaunchers();
        loadBranchesSpinner();

        btnSave.setOnClickListener(v -> {
            String plate = text(etPlate);
            String type = spinnerText(spVehicleType);
            if (TextUtils.isEmpty(plate)) {
                Log.w(TAG, "validation: plate empty");
                Toast.makeText(this, R.string.validation_plate_required, Toast.LENGTH_LONG).show();
                scrollAddCarTo(etPlate);
                etPlate.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(type)) {
                Log.w(TAG, "validation: vehicle type empty (adapter=" + (spVehicleType.getAdapter() != null)
                        + " pos=" + spVehicleType.getSelectedItemPosition() + ")");
                Toast.makeText(this, R.string.validation_vehicle_type_required, Toast.LENGTH_LONG).show();
                scrollAddCarTo(spVehicleType);
                spVehicleType.requestFocus();
                return;
            }

            int km = 0;
            try {
                String ks = text(etKm);
                if (!ks.isEmpty()) {
                    km = Integer.parseInt(ks);
                }
            } catch (NumberFormatException ignored) {
            }

            double cat = 0;
            double sale = 0;
            try {
                String c = text(etCat);
                if (!c.isEmpty()) {
                    cat = Double.parseDouble(c);
                }
            } catch (NumberFormatException ignored) {
            }
            try {
                String s = text(etSale);
                if (!s.isEmpty()) {
                    sale = Double.parseDouble(s);
                }
            } catch (NumberFormatException ignored) {
            }

            CarListing car = new CarListing();
            car.setOwnerUserId(auth.getUserId());
            int pos = spBranch.getSelectedItemPosition();
            if (pos > 0 && pos - 1 < branchIds.size()) {
                car.setBranchId(branchIds.get(pos - 1));
            } else {
                car.setBranchId("");
            }
            car.setPlateNumber(plate);
            car.setBrand(text(actBrand));
            car.setModelName(text(actModel));
            String logoKey = selectedLogoKeyFromSpinner();
            car.setBrandLogoKey(logoKey != null && !logoKey.isEmpty() ? logoKey : null);
            // Appwrite דורש מאפיין string listIconEmoji — לא לשלוח null (נעלם מה-JSON)
            if (car.getBrandLogoKey() != null) {
                car.setListIconEmoji("");
            } else if (editingCarId != null && !TextUtils.isEmpty(editingListIconEmoji)) {
                car.setListIconEmoji(editingListIconEmoji.trim());
            } else {
                car.setListIconEmoji("");
            }
            car.setVehicleType(type);
            car.setSubType(spinnerText(spSubType));
            car.setManufactureYear(spinnerText(spYear));
            car.setColor(text(etColor));
            car.setKilometer(km);
            car.setPreviousOwnership(quickAdd ? "" : spinnerText(spPrevOwnership));
            car.setDescription(quickAdd ? "" : text(etDesc));
            car.setCatalogPrice(quickAdd ? 0 : cat);
            car.setSalePrice(sale);
            car.setDisplayedToCustomer(swDisplayToCustomer.isChecked());
            car.setImageUrl1(imageUrls[0] != null ? imageUrls[0] : "");
            car.setImageUrl2(imageUrls[1] != null ? imageUrls[1] : "");
            car.setImageUrl3(imageUrls[2] != null ? imageUrls[2] : "");
            if (quickAdd) {
                car.setImageUrl4("");
                car.setImageUrl5("");
                car.setImageUrl6("");
            } else {
                car.setImageUrl4(imageUrls[3] != null ? imageUrls[3] : "");
                car.setImageUrl5(imageUrls[4] != null ? imageUrls[4] : "");
                car.setImageUrl6(imageUrls[5] != null ? imageUrls[5] : "");
            }

            btnSave.setEnabled(false);
            LoadingOverlay.show(loadingOverlay);
            bg.execute(() -> {
                DALAppWriteConnection.OperationResult<?> res;
                if (editingCarId != null) {
                    car.setId(editingCarId);
                    car.setSold(editingCarSold);
                    DALAppWriteConnection.OperationResult<CarListing> u =
                            dal.updateData(car, AppwriteCollections.CARS, editingCarId, null);
                    res = u;
                } else {
                    res = dal.saveData(car, AppwriteCollections.CARS, null);
                }
                runOnUiThread(() -> {
                    try {
                        btnSave.setEnabled(true);
                        if (res.success) {
                            Log.i(TAG, editingCarId != null ? "car updated" : "car saved");
                            CarBrandModelPresetsManager.addOrPromote(
                                    AddCarActivity.this, text(actBrand), text(actModel),
                                    selectedLogoKeyFromSpinner(), null);
                            finish();
                        } else {
                            Log.e(TAG, "car save failed: " + (res.message != null ? res.message : "שגיאה"));
                        }
                    } finally {
                        LoadingOverlay.hide(loadingOverlay);
                    }
                });
            });
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        maybeAutoAddBrandModelPreset();
    }

    private void scrollAddCarTo(@NonNull View target) {
        NestedScrollView nsv = findViewById(R.id.addCarScroll);
        if (nsv == null) {
            return;
        }
        nsv.post(() -> {
            int y = 0;
            View v = target;
            while (v != null && v != nsv) {
                y += v.getTop();
                ViewParent pv = v.getParent();
                v = pv instanceof View ? (View) pv : null;
            }
            int margin = (int) (72 * getResources().getDisplayMetrics().density);
            nsv.smoothScrollTo(0, Math.max(0, y - margin));
        });
    }

    private void maybeAutoAddBrandModelPreset() {
        String b = text(actBrand);
        String m = text(actModel);
        if (TextUtils.isEmpty(b) && TextUtils.isEmpty(m)) {
            return;
        }
        if (CarBrandModelPresetsManager.hasMatchingPreset(this, b, m)) {
            return;
        }
        CarBrandModelPresetsManager.addOrPromote(this, b, m, selectedLogoKeyFromSpinner(), null);
        refreshPresetsUi();
    }

    private void setupVehicleSpinners() {
        ArrayAdapter<String> mainAd = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>(VehicleCatalog.MAIN_TYPES));
        mainAd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVehicleType.setAdapter(mainAd);

        spVehicleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (vehicleSpinnerProgrammatic) {
                    return;
                }
                String main = VehicleCatalog.MAIN_TYPES.get(position);
                fillSubSpinner(main);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        fillSubSpinner(VehicleCatalog.MAIN_TYPES.get(0));
    }

    private void fillSubSpinner(String mainType) {
        List<String> subs = VehicleCatalog.subTypesFor(mainType);
        ArrayAdapter<String> subAd = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, subs);
        subAd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubType.setAdapter(subAd);
    }

    private void setupYearSpinner() {
        List<String> years = new ArrayList<>();
        int cy = Calendar.getInstance().get(Calendar.YEAR);
        for (int y = cy; y >= 1970; y--) {
            years.add(String.valueOf(y));
        }
        yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spYear.setAdapter(yearAdapter);
    }

    private void setupPreviousOwnershipSpinner() {
        List<String> opts = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.previous_ownership_options)));
        prevOwnershipAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opts);
        prevOwnershipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPrevOwnership.setAdapter(prevOwnershipAdapter);
        spPrevOwnership.setSelection(0);
    }

    private void resetPreviousOwnershipOptions() {
        if (prevOwnershipAdapter == null) {
            return;
        }
        List<String> opts = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.previous_ownership_options)));
        prevOwnershipAdapter.clear();
        prevOwnershipAdapter.addAll(opts);
        prevOwnershipAdapter.notifyDataSetChanged();
    }

    private void applyPreviousOwnershipToSpinner(String saved) {
        if (prevOwnershipAdapter == null) {
            return;
        }
        String t = saved != null ? saved.trim() : "";
        if (t.isEmpty()) {
            spPrevOwnership.setSelection(0);
            return;
        }
        for (int i = 0; i < prevOwnershipAdapter.getCount(); i++) {
            String row = prevOwnershipAdapter.getItem(i);
            if (t.equals(row)) {
                spPrevOwnership.setSelection(i);
                return;
            }
        }
        prevOwnershipAdapter.add(t);
        prevOwnershipAdapter.notifyDataSetChanged();
        spPrevOwnership.setSelection(prevOwnershipAdapter.getCount() - 1);
    }

    private void loadBranchesSpinner() {
        bg.execute(() -> {
            branchIds.clear();
            branchLabels.clear();
            branchLabels.add("ללא סניף");
            DALAppWriteConnection.OperationResult<ArrayList<Branch>> res =
                    dal.getData(AppwriteCollections.BRANCHES, null, Branch.class);
            String uid = auth.getUserId();
            if (res.success && res.data != null) {
                for (Branch b : res.data) {
                    if (!uid.equals(b.getOwnerUserId()) || b.getId() == null) {
                        continue;
                    }
                    if (!isBranchNameAndAddressFilled(b)) {
                        continue;
                    }
                    branchIds.add(b.getId());
                    branchLabels.add(b.getBranchName().trim() + " · " + b.getAddress().trim());
                }
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, branchLabels);
                spBranch.setAdapter(ad);
                if (editingCarId != null) {
                    fetchAndBindEditingCar();
                } else {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }

    private void fetchAndBindEditingCar() {
        bg.execute(() -> {
            DALAppWriteConnection.OperationResult<CarListing> r =
                    dal.getDataById(AppwriteCollections.CARS, editingCarId, null, CarListing.class);
            runOnUiThread(() -> {
                try {
                    if (!r.success || r.data == null) {
                        Log.e(TAG, "edit load failed: " + (r.message != null ? r.message : ""));
                        finish();
                        return;
                    }
                    CarListing c = r.data;
                editingCarSold = c.isSold();
                editingListIconEmoji = c.getListIconEmoji();
                etPlate.setText(nz(c.getPlateNumber()));
                actBrand.setText(nz(c.getBrand()), false);
                rebuildModelSuggestions();
                actModel.setText(nz(c.getModelName()), false);
                applyLogoKeyToSpinner(c.getBrandLogoKey());
                bindVehicleSpinnersFromCar(c);
                selectYearSpinner(nz(c.getManufactureYear()));
                etColor.setText(nz(c.getColor()));
                etKm.setText(c.getKilometer() > 0 ? String.valueOf(c.getKilometer()) : "");
                resetPreviousOwnershipOptions();
                applyPreviousOwnershipToSpinner(nz(c.getPreviousOwnership()));
                etDesc.setText(nz(c.getDescription()));
                etCat.setText(c.getCatalogPrice() > 0 ? String.valueOf(c.getCatalogPrice()) : "");
                etSale.setText(c.getSalePrice() > 0 ? String.valueOf(c.getSalePrice()) : "");
                swDisplayToCustomer.setChecked(c.isDisplayedToCustomer());

                imageUrls[0] = nz(c.getImageUrl1());
                imageUrls[1] = nz(c.getImageUrl2());
                imageUrls[2] = nz(c.getImageUrl3());
                imageUrls[3] = nz(c.getImageUrl4());
                imageUrls[4] = nz(c.getImageUrl5());
                imageUrls[5] = nz(c.getImageUrl6());
                for (int i = 0; i < imageViews.length; i++) {
                    if (imageUrls[i] != null && !imageUrls[i].isEmpty()) {
                        Glide.with(AddCarActivity.this)
                                .load(AppwriteGlideUrl.withHeaders(imageUrls[i]))
                                .centerCrop()
                                .into(imageViews[i]);
                    }
                }

                int sel = 0;
                String bid = c.getBranchId();
                if (bid != null && !bid.isEmpty()) {
                    int idx = branchIds.indexOf(bid);
                    if (idx >= 0) {
                        sel = idx + 1;
                    }
                }
                if (spBranch.getAdapter() != null && sel < spBranch.getAdapter().getCount()) {
                    spBranch.setSelection(sel);
                }
                } finally {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }

    private void bindVehicleSpinnersFromCar(CarListing c) {
        String main = nz(c.getVehicleType());
        vehicleSpinnerProgrammatic = true;
        try {
            int mi = VehicleCatalog.MAIN_TYPES.indexOf(main);
            if (mi < 0) {
                mi = VehicleCatalog.MAIN_TYPES.indexOf("אחר");
            }
            if (mi < 0) {
                mi = 0;
            }
            spVehicleType.setSelection(mi);
            fillSubSpinner(VehicleCatalog.MAIN_TYPES.get(mi));
            String sub = nz(c.getSubType());
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> subAd = (ArrayAdapter<String>) spSubType.getAdapter();
            if (subAd != null) {
                int si = -1;
                for (int i = 0; i < subAd.getCount(); i++) {
                    if (sub.equals(subAd.getItem(i))) {
                        si = i;
                        break;
                    }
                }
                spSubType.setSelection(si >= 0 ? si : 0);
            }
        } finally {
            vehicleSpinnerProgrammatic = false;
        }
    }

    private void selectYearSpinner(String yearStr) {
        if (yearStr == null || yearStr.isEmpty()) {
            return;
        }
        for (int i = 0; i < yearAdapter.getCount(); i++) {
            if (yearStr.equals(yearAdapter.getItem(i))) {
                spYear.setSelection(i);
                return;
            }
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private void registerLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Bundle ex = result.getData().getExtras();
                    if (ex == null) {
                        return;
                    }
                    Bitmap bmp = (Bitmap) ex.get("data");
                    if (bmp != null) {
                        uploadSlot(bmp);
                    }
                });
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    if (uri == null) {
                        return;
                    }
                    try {
                        Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        if (bmp != null) {
                            uploadSlot(bmp);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "gallery", e);
                    }
                });
    }

    private void pickImageSource() {
        new AlertDialog.Builder(this)
                .setTitle("תמונה " + (activeSlot + 1))
                .setItems(new CharSequence[]{"מצלמה", "גלריה"}, (d, which) -> {
                    if (which == 0) {
                        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                    } else {
                        Intent pick = new Intent(Intent.ACTION_PICK);
                        pick.setType("image/*");
                        galleryLauncher.launch(pick);
                    }
                })
                .show();
    }

    private void uploadSlot(Bitmap bitmap) {
        Log.d(TAG, "upload slot image start slot=" + activeSlot);
        int slot = activeSlot;
        bg.execute(() -> {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] bytes = stream.toByteArray();
                String fileName = "car_" + System.currentTimeMillis() + "_" + slot + ".jpg";
                DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> up =
                        dal.uploadFile(bytes, fileName, "image/jpeg", null);
                runOnUiThread(() -> {
                    if (up.success && up.data != null) {
                        imageUrls[slot] = up.data.fileUrl;
                        Glide.with(this).load(AppwriteGlideUrl.withHeaders(up.data.fileUrl))
                                .centerCrop().into(imageViews[slot]);
                        Log.i(TAG, "slot image uploaded");
                    } else {
                        Log.e(TAG, "upload failed: " + (up.message != null ? up.message : "שגיאה"));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Log.e(TAG, "uploadSlot", e));
            }
        });
    }

    private static String text(@Nullable EditText e) {
        if (e == null || e.getText() == null) {
            return "";
        }
        return e.getText().toString().trim();
    }

    private static String spinnerText(Spinner s) {
        Object item = s.getSelectedItem();
        return item != null ? item.toString().trim() : "";
    }

    private static boolean isBranchNameAndAddressFilled(Branch b) {
        if (b == null) {
            return false;
        }
        String n = b.getBranchName();
        String a = b.getAddress();
        return n != null && !n.trim().isEmpty() && a != null && !a.trim().isEmpty();
    }

    private void wireBrandLogoSpinner() {
        List<BrandLogoSpinnerAdapter.Row> rows = new ArrayList<>();
        rows.add(new BrandLogoSpinnerAdapter.Row(null, getString(R.string.brand_logo_auto), 0xFF78909C));
        for (CarBrandLogoCatalog.Entry e : CarBrandLogoCatalog.pickableEntries()) {
            rows.add(new BrandLogoSpinnerAdapter.Row(e.key, e.labelHe, e.color));
        }
        logoSpinnerAdapter = new BrandLogoSpinnerAdapter(this, rows);
        spBrandLogo.setAdapter(logoSpinnerAdapter);
        spBrandLogo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (logoSpinnerProgrammatic) {
                    return;
                }
                maybeAutoAddBrandModelPreset();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Nullable
    private String selectedLogoKeyFromSpinner() {
        int pos = spBrandLogo.getSelectedItemPosition();
        if (pos < 0 || logoSpinnerAdapter == null || pos >= logoSpinnerAdapter.getCount()) {
            return null;
        }
        BrandLogoSpinnerAdapter.Row r = logoSpinnerAdapter.getItem(pos);
        if (r == null || r.key == null || r.key.isEmpty()) {
            return null;
        }
        return r.key;
    }

    private void applyLogoKeyToSpinner(@Nullable String key) {
        logoSpinnerProgrammatic = true;
        try {
            int sel = 0;
            if (key != null && !key.isEmpty() && logoSpinnerAdapter != null) {
                for (int i = 0; i < logoSpinnerAdapter.getCount(); i++) {
                    BrandLogoSpinnerAdapter.Row r = logoSpinnerAdapter.getItem(i);
                    if (r != null && r.key != null && r.key.equalsIgnoreCase(key)) {
                        sel = i;
                        break;
                    }
                }
            }
            spBrandLogo.setSelection(sel, false);
        } finally {
            spBrandLogo.post(() -> logoSpinnerProgrammatic = false);
        }
    }

    private void rebuildBrandSuggestions() {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> brands = new ArrayList<>();
        for (CarBrandModelPreset p : CarBrandModelPresetsManager.load(this)) {
            if (p.getBrand() == null) {
                continue;
            }
            String t = p.getBrand().trim();
            if (t.isEmpty()) {
                continue;
            }
            boolean dup = false;
            for (String x : seen) {
                if (x.equalsIgnoreCase(t)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                seen.add(t);
                brands.add(t);
            }
        }
        Collections.sort(brands, String.CASE_INSENSITIVE_ORDER);
        brandSuggestAdapter.clear();
        brandSuggestAdapter.addAll(brands);
        brandSuggestAdapter.notifyDataSetChanged();
    }

    private void rebuildModelSuggestions() {
        String brand = text(actBrand);
        String brandKey = brand.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> models = new ArrayList<>();
        for (CarBrandModelPreset p : CarBrandModelPresetsManager.load(this)) {
            String m = p.getModelName() != null ? p.getModelName().trim() : "";
            if (m.isEmpty()) {
                continue;
            }
            if (!brandKey.isEmpty()) {
                String pb = p.getBrand() != null ? p.getBrand().trim().toLowerCase(Locale.ROOT) : "";
                if (!pb.equals(brandKey)) {
                    continue;
                }
            }
            boolean dup = false;
            for (String x : seen) {
                if (x.equalsIgnoreCase(m)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                seen.add(m);
                models.add(m);
            }
        }
        Collections.sort(models, String.CASE_INSENSITIVE_ORDER);
        modelSuggestAdapter.clear();
        modelSuggestAdapter.addAll(models);
        modelSuggestAdapter.notifyDataSetChanged();
    }

    private void refreshPresetsUi() {
        rebuildBrandSuggestions();
        rebuildModelSuggestions();
    }
}
