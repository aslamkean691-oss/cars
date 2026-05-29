package fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cars.AddCarActivity;
import com.example.cars.Hellper.AuthSessionManager;
import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.LoadExecutor;
import com.example.cars.R;
import com.example.cars.adapter.CarAdapter;
import com.example.cars.util.LoadingOverlay;
import com.example.cars.model.AppwriteCollections;
import com.example.cars.model.Branch;
import com.example.cars.model.CarListing;
import com.example.cars.util.BranchLabelMap;
import com.example.cars.util.CarListingFilterSort;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class MenuFragment extends Fragment implements CarAdapter.AdminCarActionsListener {

    private static final String TAG = "MenuFragment";
    private static final String ARG_ADMIN_MODE = "admin_mode";

    private final Executor bg = LoadExecutor.io();
    private CarAdapter adapter;
    private TextView tvEmpty;
    private TextView tvMenuTitle;
    private MaterialButton btnAddCar;
    private View rowAdminCarTools;
    private MaterialButton btnBranches;
    private MaterialButton btnQuickAdd;
    private MaterialButton btnFullAdd;
    private DALAppWriteConnection dal;
    private AuthSessionManager auth;
    private boolean adminMode;
    @Nullable
    private View loadingOverlay;
    /** רשימה מלאה לפני סינון — רק במצב לקוח */
    @Nullable
    private ArrayList<CarListing> sourceCars;
    @NonNull
    private final Map<String, String> branchMap = new HashMap<>();
    @NonNull
    private CarListingFilterSort.FilterState customerFilterState = new CarListingFilterSort.FilterState();
    @Nullable
    private View rowCustomerFilterBar;
    @Nullable
    private TextView tvFilterActiveBadge;
    @Nullable
    private TextView tvCarResultsCount;

    public static MenuFragment newInstance(boolean adminMode) {
        MenuFragment f = new MenuFragment();
        Bundle b = new Bundle();
        b.putBoolean(ARG_ADMIN_MODE, adminMode);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle a = getArguments();
        adminMode = a != null && a.getBoolean(ARG_ADMIN_MODE, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        auth = new AuthSessionManager(requireContext());
        dal = new DALAppWriteConnection(requireContext());
        auth.restoreDalIfNeeded(dal);

        RecyclerView rv = view.findViewById(R.id.rvCars);
        tvEmpty = view.findViewById(R.id.tvEmptyCars);
        btnAddCar = view.findViewById(R.id.btnAddCar);
        tvMenuTitle = view.findViewById(R.id.tvMenuTitle);
        rowAdminCarTools = view.findViewById(R.id.rowAdminCarTools);
        btnBranches = view.findViewById(R.id.btnBranches);
        btnQuickAdd = view.findViewById(R.id.btnQuickAdd);
        btnFullAdd = view.findViewById(R.id.btnFullAddCar);

        if (adminMode) {
            tvMenuTitle.setText(getString(R.string.nav_admin_cars));
            tvMenuTitle.append("\n");
            tvMenuTitle.append(getString(R.string.menu_admin_cars_subtitle));
            btnAddCar.setVisibility(View.GONE);
            rowAdminCarTools.setVisibility(View.VISIBLE);
            btnBranches.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.btflag, new SettingFragment())
                    .addToBackStack("branches")
                    .commit());
            btnQuickAdd.setOnClickListener(v -> {
                Intent i = new Intent(getActivity(), AddCarActivity.class);
                i.putExtra(AddCarActivity.EXTRA_QUICK_ADD, true);
                startActivity(i);
            });
            btnFullAdd.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), AddCarActivity.class)));
        } else {
            tvMenuTitle.setText(getString(R.string.nav_customer_cars));
            btnAddCar.setVisibility(View.GONE);
            rowAdminCarTools.setVisibility(View.GONE);
        }

        rowCustomerFilterBar = view.findViewById(R.id.rowCustomerFilterBar);
        tvFilterActiveBadge = view.findViewById(R.id.tvFilterActiveBadge);
        tvCarResultsCount = view.findViewById(R.id.tvCarResultsCount);
        if (rowCustomerFilterBar != null) {
            rowCustomerFilterBar.setVisibility(adminMode ? View.GONE : View.VISIBLE);
        }
        if (!adminMode) {
            MaterialButton btnOpenFilter = view.findViewById(R.id.btnOpenFilterSort);
            if (btnOpenFilter != null) {
                btnOpenFilter.setOnClickListener(v -> openCustomerFilterSortSheet());
            }
        }

        adapter = adminMode
                ? new CarAdapter(true, this)
                : new CarAdapter(false, null);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rv.setAdapter(adapter);

        loadCars();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCars();
    }

    @Override
    public void onEditCar(@NonNull CarListing car) {
        if (car.getId() == null) return;
        Intent i = new Intent(getActivity(), AddCarActivity.class);
        i.putExtra(AddCarActivity.EXTRA_EDIT_CAR_ID, car.getId());
        startActivity(i);
    }

    @Override
    public void onMarkSold(@NonNull CarListing car) {
        if (car.getId() == null) return;
        LoadingOverlay.show(loadingOverlay);
        bg.execute(() -> {
            DALAppWriteConnection.OperationResult<CarListing> r =
                    dal.getDataById(AppwriteCollections.CARS, car.getId(), null, CarListing.class);
            if (!r.success || r.data == null) {
                Log.e(TAG, "mark sold load failed: " + (r.message != null ? r.message : ""));
                finishMarkSoldUi(null);
                return;
            }
            r.data.setSold(true);
            DALAppWriteConnection.OperationResult<CarListing> u =
                    dal.updateData(r.data, AppwriteCollections.CARS, car.getId(), null);
            finishMarkSoldUi(u);
        });
    }

    private void finishMarkSoldUi(@Nullable DALAppWriteConnection.OperationResult<CarListing> u) {
        if (getActivity() == null) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (!isAdded() || getView() == null) {
                LoadingOverlay.hide(loadingOverlay);
                return;
            }
            if (u != null && u.success) {
                Log.i(TAG, "car marked sold");
                loadCars();
                return;
            }
            if (u != null) {
                Log.e(TAG, "mark sold failed: " + (u.message != null ? u.message : ""));
            }
            LoadingOverlay.hide(loadingOverlay);
        });
    }

    private void openCustomerFilterSortSheet() {
        if (adminMode || sourceCars == null) {
            return;
        }
        String allBranches = getString(R.string.car_filter_all_branches);
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        labels.add(allBranches);
        ids.add("");
        for (Map.Entry<String, String> e : branchMap.entrySet()) {
            labels.add(e.getValue() != null ? e.getValue() : "");
            ids.add(e.getKey());
        }
        CarFilterSortBottomSheet sheet = CarFilterSortBottomSheet.newInstance(
                customerFilterState.clone(),
                labels.toArray(new String[0]),
                ids.toArray(new String[0]));
        sheet.setListener(state -> {
            customerFilterState = state;
            applyCustomerFilterToListUi();
        });
        sheet.show(getParentFragmentManager(), "carFilterSort");
    }

    private void applyCustomerFilterToListUi() {
        if (adminMode) {
            return;
        }
        ArrayList<CarListing> base = sourceCars != null ? sourceCars : new ArrayList<>();
        ArrayList<CarListing> filtered = CarListingFilterSort.apply(base, customerFilterState);
        adapter.setItems(filtered);
        boolean empty = filtered.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && !base.isEmpty()) {
            tvEmpty.setText(R.string.cars_empty_filtered);
        } else {
            tvEmpty.setText(R.string.cars_empty);
        }
        if (tvFilterActiveBadge != null) {
            tvFilterActiveBadge.setVisibility(
                    customerFilterState.hasActiveSortOrFilters() ? View.VISIBLE : View.GONE);
        }
        if (tvCarResultsCount != null) {
            if (!base.isEmpty()) {
                tvCarResultsCount.setVisibility(View.VISIBLE);
                if (customerFilterState.hasActiveSortOrFilters()) {
                    tvCarResultsCount.setText(getString(
                            R.string.car_results_count_filtered, filtered.size(), base.size()));
                } else {
                    tvCarResultsCount.setText(getString(R.string.car_results_count, base.size()));
                }
            } else {
                tvCarResultsCount.setVisibility(View.GONE);
            }
        }
    }

    private void loadCars() {
        LoadingOverlay.show(loadingOverlay);
        bg.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<CarListing>> res =
                    dal.getData(AppwriteCollections.CARS, null, CarListing.class);
            DALAppWriteConnection.OperationResult<ArrayList<Branch>> brRes =
                    dal.getData(AppwriteCollections.BRANCHES, null, Branch.class);
            Map<String, String> branchMap = BranchLabelMap.from(
                    brRes.success && brRes.data != null ? brRes.data : null);
            ArrayList<CarListing> raw = (res.success && res.data != null) ? res.data : new ArrayList<>();
            ArrayList<CarListing> list = new ArrayList<>();
            for (CarListing c : raw) {
                if (c.isSold()) {
                    continue;
                }
                if (!adminMode && !c.isDisplayedToCustomer()) {
                    continue;
                }
                list.add(c);
            }
            if (getActivity() == null) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                try {
                    if (isAdded() && getView() != null) {
                        MenuFragment.this.branchMap.clear();
                        MenuFragment.this.branchMap.putAll(branchMap);
                        adapter.setBranchIdToLabelMap(branchMap);
                        if (adminMode) {
                            sourceCars = null;
                            adapter.setItems(list);
                            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                            tvEmpty.setText(R.string.cars_empty);
                            if (tvCarResultsCount != null) {
                                tvCarResultsCount.setVisibility(View.GONE);
                            }
                            if (tvFilterActiveBadge != null) {
                                tvFilterActiveBadge.setVisibility(View.GONE);
                            }
                        } else {
                            sourceCars = new ArrayList<>(list);
                            applyCustomerFilterToListUi();
                        }
                    }
                } finally {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }
}
