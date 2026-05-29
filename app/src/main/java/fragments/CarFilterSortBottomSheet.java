package fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;

import com.example.cars.R;
import com.example.cars.util.CarListingFilterSort;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

/**
 * גיליון תחתון: סינון ומיון רכבים ללקוח.
 */
public class CarFilterSortBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onCarFilterSortResult(@NonNull CarListingFilterSort.FilterState state);
    }

    private static final String ARG_STATE = "state";
    private static final String ARG_BRANCH_LABELS = "branch_labels";
    private static final String ARG_BRANCH_IDS = "branch_ids";

    private static final CarListingFilterSort.SortKind[] SORT_KINDS = {
            CarListingFilterSort.SortKind.DEFAULT,
            CarListingFilterSort.SortKind.SALE_PRICE_ASC,
            CarListingFilterSort.SortKind.SALE_PRICE_DESC,
            CarListingFilterSort.SortKind.CATALOG_PRICE_ASC,
            CarListingFilterSort.SortKind.CATALOG_PRICE_DESC,
            CarListingFilterSort.SortKind.YEAR_DESC,
            CarListingFilterSort.SortKind.YEAR_ASC,
            CarListingFilterSort.SortKind.KM_ASC,
            CarListingFilterSort.SortKind.KM_DESC,
            CarListingFilterSort.SortKind.BRAND_ASC,
            CarListingFilterSort.SortKind.MODEL_ASC,
            CarListingFilterSort.SortKind.PLATE_ASC,
    };

    @Nullable
    private Listener listener;

    private MaterialAutoCompleteTextView actSort;
    private MaterialAutoCompleteTextView actBranch;
    private TextInputEditText etBrand;
    private TextInputEditText etModel;
    private TextInputEditText etVehicleType;
    private TextInputEditText etSubType;
    private TextInputEditText etColor;
    private TextInputEditText etOwnership;
    private TextInputEditText etPlate;
    private TextInputEditText etYearMin;
    private TextInputEditText etYearMax;
    private TextInputEditText etPriceMin;
    private TextInputEditText etPriceMax;
    private TextInputEditText etKmMax;

    private String[] branchLabels = new String[0];
    private String[] branchIds = new String[0];
    private int sortSelectionIndex;
    private int branchSelectionIndex;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    public static CarFilterSortBottomSheet newInstance(
            @NonNull CarListingFilterSort.FilterState state,
            @NonNull String[] branchLabels,
            @NonNull String[] branchIds) {
        CarFilterSortBottomSheet f = new CarFilterSortBottomSheet();
        Bundle b = new Bundle();
        b.putSerializable(ARG_STATE, state);
        b.putStringArray(ARG_BRANCH_LABELS, branchLabels);
        b.putStringArray(ARG_BRANCH_IDS, branchIds);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_car_filter_sort, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        CarListingFilterSort.FilterState state = BundleCompat.getSerializable(
                args, ARG_STATE, CarListingFilterSort.FilterState.class);
        if (state == null) {
            state = new CarListingFilterSort.FilterState();
        }
        branchLabels = args.getStringArray(ARG_BRANCH_LABELS);
        branchIds = args.getStringArray(ARG_BRANCH_IDS);
        if (branchLabels == null) {
            branchLabels = new String[0];
        }
        if (branchIds == null || branchIds.length != branchLabels.length) {
            branchIds = new String[branchLabels.length];
            for (int i = 0; i < branchIds.length; i++) {
                branchIds[i] = "";
            }
        }

        actSort = view.findViewById(R.id.actSort);
        actBranch = view.findViewById(R.id.actBranch);
        etBrand = view.findViewById(R.id.etBrand);
        etModel = view.findViewById(R.id.etModel);
        etVehicleType = view.findViewById(R.id.etVehicleType);
        etSubType = view.findViewById(R.id.etSubType);
        etColor = view.findViewById(R.id.etColor);
        etOwnership = view.findViewById(R.id.etOwnership);
        etPlate = view.findViewById(R.id.etPlate);
        etYearMin = view.findViewById(R.id.etYearMin);
        etYearMax = view.findViewById(R.id.etYearMax);
        etPriceMin = view.findViewById(R.id.etPriceMin);
        etPriceMax = view.findViewById(R.id.etPriceMax);
        etKmMax = view.findViewById(R.id.etKmMax);
        MaterialButton btnReset = view.findViewById(R.id.btnResetFilters);
        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilters);

        String[] sortLabels = buildSortLabels();
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, sortLabels);
        actSort.setAdapter(sortAdapter);
        sortSelectionIndex = indexOfSort(state.sortKind);
        actSort.setText(sortLabels[sortSelectionIndex], false);
        actSort.setOnItemClickListener((parent, v, position, id) -> sortSelectionIndex = position);

        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, branchLabels);
        actBranch.setAdapter(branchAdapter);
        branchSelectionIndex = resolveBranchIndex(state.branchId);
        if (branchSelectionIndex >= 0 && branchSelectionIndex < branchLabels.length) {
            actBranch.setText(branchLabels[branchSelectionIndex], false);
        }
        actBranch.setOnItemClickListener((parent, v, position, id) -> branchSelectionIndex = position);

        etBrand.setText(state.brandContains);
        etModel.setText(state.modelContains);
        etVehicleType.setText(state.vehicleTypeContains);
        etSubType.setText(state.subTypeContains);
        etColor.setText(state.colorContains);
        etOwnership.setText(state.previousOwnershipContains);
        etPlate.setText(state.plateContains);
        if (state.yearMin != null) {
            etYearMin.setText(String.format(Locale.US, "%d", state.yearMin));
        }
        if (state.yearMax != null) {
            etYearMax.setText(String.format(Locale.US, "%d", state.yearMax));
        }
        if (state.priceMin != null) {
            etPriceMin.setText(formatPriceField(state.priceMin));
        }
        if (state.priceMax != null) {
            etPriceMax.setText(formatPriceField(state.priceMax));
        }
        if (state.kmMax != null) {
            etKmMax.setText(String.format(Locale.US, "%d", state.kmMax));
        }

        btnReset.setOnClickListener(v -> {
            CarListingFilterSort.FilterState cleared = new CarListingFilterSort.FilterState();
            cleared.reset();
            if (listener != null) {
                listener.onCarFilterSortResult(cleared);
            }
            dismiss();
        });

        btnApply.setOnClickListener(v -> {
            CarListingFilterSort.FilterState out = readStateFromForm();
            if (listener != null) {
                listener.onCarFilterSortResult(out);
            }
            dismiss();
        });
    }

    @NonNull
    private String[] buildSortLabels() {
        return new String[] {
                getString(R.string.sort_default),
                getString(R.string.sort_sale_price_asc),
                getString(R.string.sort_sale_price_desc),
                getString(R.string.sort_catalog_price_asc),
                getString(R.string.sort_catalog_price_desc),
                getString(R.string.sort_year_desc),
                getString(R.string.sort_year_asc),
                getString(R.string.sort_km_asc),
                getString(R.string.sort_km_desc),
                getString(R.string.sort_brand_asc),
                getString(R.string.sort_model_asc),
                getString(R.string.sort_plate_asc),
        };
    }

    private int indexOfSort(@NonNull CarListingFilterSort.SortKind k) {
        for (int i = 0; i < SORT_KINDS.length; i++) {
            if (SORT_KINDS[i] == k) {
                return i;
            }
        }
        return 0;
    }

    private int resolveBranchIndex(@Nullable String branchId) {
        if (branchId == null || branchId.trim().isEmpty()) {
            return 0;
        }
        String b = branchId.trim();
        for (int i = 0; i < branchIds.length; i++) {
            if (b.equals(branchIds[i])) {
                return i;
            }
        }
        return 0;
    }

    @NonNull
    private CarListingFilterSort.FilterState readStateFromForm() {
        CarListingFilterSort.FilterState out = new CarListingFilterSort.FilterState();
        if (sortSelectionIndex >= 0 && sortSelectionIndex < SORT_KINDS.length) {
            out.sortKind = SORT_KINDS[sortSelectionIndex];
        }
        if (branchSelectionIndex >= 0 && branchSelectionIndex < branchIds.length) {
            String id = branchIds[branchSelectionIndex];
            out.branchId = (id == null || id.isEmpty()) ? null : id;
        }
        out.brandContains = textOf(etBrand);
        out.modelContains = textOf(etModel);
        out.vehicleTypeContains = textOf(etVehicleType);
        out.subTypeContains = textOf(etSubType);
        out.colorContains = textOf(etColor);
        out.previousOwnershipContains = textOf(etOwnership);
        out.plateContains = textOf(etPlate);
        out.yearMin = parseIntOrNull(textOf(etYearMin));
        out.yearMax = parseIntOrNull(textOf(etYearMax));
        out.priceMin = parseDoubleOrNull(textOf(etPriceMin));
        out.priceMax = parseDoubleOrNull(textOf(etPriceMax));
        out.kmMax = parseIntOrNull(textOf(etKmMax));
        return out;
    }

    @NonNull
    private static String textOf(@Nullable TextInputEditText et) {
        if (et == null || et.getText() == null) {
            return "";
        }
        return et.getText().toString();
    }

    @Nullable
    private static Integer parseIntOrNull(@NonNull String s) {
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(t.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static Double parseDoubleOrNull(@NonNull String s) {
        String t = s.trim().replace(',', '.');
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @NonNull
    private static String formatPriceField(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-6) {
            return String.format(Locale.US, "%.0f", v);
        }
        return String.format(Locale.US, "%s", v);
    }
}
