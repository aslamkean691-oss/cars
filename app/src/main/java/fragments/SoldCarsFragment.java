package fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executor;

public class SoldCarsFragment extends Fragment {

    private final Executor bg = LoadExecutor.io();
    private CarAdapter adapter;
    private TextView tvEmpty;
    @Nullable
    private View loadingOverlay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        AuthSessionManager auth = new AuthSessionManager(requireContext());
        auth.restoreDalIfNeeded(new DALAppWriteConnection(requireContext()));

        TextView tvTitle = view.findViewById(R.id.tvMenuTitle);
        tvTitle.setText(getString(R.string.sold_cars_title));
        view.findViewById(R.id.btnAddCar).setVisibility(View.GONE);
        View rowTools = view.findViewById(R.id.rowAdminCarTools);
        if (rowTools != null) {
            rowTools.setVisibility(View.GONE);
        }
        View rowCustomerFilter = view.findViewById(R.id.rowCustomerFilterBar);
        if (rowCustomerFilter != null) {
            rowCustomerFilter.setVisibility(View.GONE);
        }

        RecyclerView rv = view.findViewById(R.id.rvCars);
        tvEmpty = view.findViewById(R.id.tvEmptyCars);
        adapter = new CarAdapter(false, null, true);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rv.setAdapter(adapter);

        loadSold();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSold();
    }

    private void loadSold() {
        LoadingOverlay.show(loadingOverlay);
        DALAppWriteConnection dal = new DALAppWriteConnection(requireContext());
        bg.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<CarListing>> res =
                    dal.getData(AppwriteCollections.CARS, null, CarListing.class);
            DALAppWriteConnection.OperationResult<ArrayList<Branch>> brRes =
                    dal.getData(AppwriteCollections.BRANCHES, null, Branch.class);
            Map<String, String> branchMap = BranchLabelMap.from(
                    brRes.success && brRes.data != null ? brRes.data : null);
            ArrayList<CarListing> raw = (res.success && res.data != null) ? res.data : new ArrayList<>();
            ArrayList<CarListing> sold = new ArrayList<>();
            for (CarListing c : raw) {
                if (c.isSold()) {
                    sold.add(c);
                }
            }
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                try {
                    if (isAdded() && getView() != null) {
                        adapter.setBranchIdToLabelMap(branchMap);
                        adapter.setItems(sold);
                        tvEmpty.setVisibility(sold.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } finally {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }
}
