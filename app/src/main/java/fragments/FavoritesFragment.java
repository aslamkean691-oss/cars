package fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.FavoritesManager;
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
import java.util.Set;
import java.util.concurrent.Executor;

public class FavoritesFragment extends Fragment {

    private final Executor bg = LoadExecutor.io();
    private CarAdapter adapter;
    private TextView tvEmpty;
    @Nullable
    private View loadingOverlay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        RecyclerView rv = view.findViewById(R.id.rvFavorites);
        tvEmpty = view.findViewById(R.id.tvFavoritesEmpty);
        adapter = new CarAdapter(false, null);
        adapter.setFavoriteChangeListener(this::loadFavorites);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rv.setAdapter(adapter);
        loadFavorites();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        if (!isAdded()) {
            return;
        }
        Context appCtx = requireContext().getApplicationContext();
        Set<String> ids = FavoritesManager.getFavoriteIds(appCtx);
        LoadingOverlay.show(loadingOverlay);
        bg.execute(() -> {
            DALAppWriteConnection dal = new DALAppWriteConnection(appCtx);
            DALAppWriteConnection.OperationResult<ArrayList<CarListing>> res =
                    dal.getData(AppwriteCollections.CARS, null, CarListing.class);
            DALAppWriteConnection.OperationResult<ArrayList<Branch>> brRes =
                    dal.getData(AppwriteCollections.BRANCHES, null, Branch.class);
            Map<String, String> branchMap = BranchLabelMap.from(
                    brRes.success && brRes.data != null ? brRes.data : null);
            ArrayList<CarListing> out = new ArrayList<>();
            if (res.success && res.data != null && !ids.isEmpty()) {
                for (CarListing c : res.data) {
                    if (c.getId() == null || !ids.contains(c.getId())) {
                        continue;
                    }
                    if (c.isSold() || !c.isDisplayedToCustomer()) {
                        continue;
                    }
                    out.add(c);
                }
            }
            FragmentActivity act = getActivity();
            if (act == null) {
                return;
            }
            act.runOnUiThread(() -> {
                try {
                    if (isAdded() && getView() != null) {
                        adapter.setBranchIdToLabelMap(branchMap);
                        adapter.setItems(out);
                        tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } finally {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }
}
