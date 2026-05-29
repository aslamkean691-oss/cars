package fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cars.Hellper.AuthSessionManager;
import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.LoadExecutor;
import com.example.cars.R;
import com.example.cars.adapter.BranchAdapter;
import com.example.cars.util.LoadingOverlay;
import com.example.cars.model.AppwriteCollections;
import com.example.cars.model.Branch;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class SettingFragment extends Fragment {

    private static final String TAG = "SettingFragment";
    private final Executor bg = LoadExecutor.io();
    private AuthSessionManager auth;
    private DALAppWriteConnection dal;
    private BranchAdapter adapter;
    private TextView tvHint;
    @Nullable
    private View loadingOverlay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        auth = new AuthSessionManager(requireContext());
        dal = new DALAppWriteConnection(requireContext());
        auth.restoreDalIfNeeded(dal);

        TextInputEditText etName = view.findViewById(R.id.etBranchName);
        TextInputEditText etAddress = view.findViewById(R.id.etBranchAddress);
        TextInputLayout tilName = view.findViewById(R.id.tilBranchName);
        TextInputLayout tilAddress = view.findViewById(R.id.tilBranchAddress);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveBranch);
        RecyclerView rv = view.findViewById(R.id.rvBranches);
        tvHint = view.findViewById(R.id.tvBranchLoginHint);

        adapter = new BranchAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        updateLoginUi(btnSave, etName, etAddress);
        loadBranches();

        btnSave.setOnClickListener(v -> {
            if (!auth.hasSession()) {
                Log.w(TAG, "save branch: not logged in");
                return;
            }
            String name = text(etName);
            String address = text(etAddress);
            tilName.setError(null);
            tilAddress.setError(null);
            if (TextUtils.isEmpty(name)) {
                tilName.setError(getString(R.string.branch_field_required));
                Log.w(TAG, "save branch: empty name");
                return;
            }
            if (TextUtils.isEmpty(address)) {
                tilAddress.setError(getString(R.string.branch_field_required));
                Log.w(TAG, "save branch: empty address");
                return;
            }
            Branch b = new Branch();
            b.setOwnerUserId(auth.getUserId());
            b.setAddress(address.trim());
            b.setBranchName(name.trim());

            btnSave.setEnabled(false);
            LoadingOverlay.show(loadingOverlay);
            bg.execute(() -> {
                DALAppWriteConnection.OperationResult<ArrayList<Branch>> res =
                        dal.saveData(b, AppwriteCollections.BRANCHES, null);
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) {
                        LoadingOverlay.hide(loadingOverlay);
                        return;
                    }
                    btnSave.setEnabled(true);
                    if (res.success) {
                        etAddress.setText("");
                        etName.setText("");
                        Log.i(TAG, "branch saved");
                        loadBranches();
                    } else {
                        String msg = res.message != null ? res.message : getString(R.string.branch_save_error);
                        Log.e(TAG, "branch save failed: " + msg);
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        LoadingOverlay.hide(loadingOverlay);
                    }
                });
            });
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            updateLoginUi(view.findViewById(R.id.btnSaveBranch),
                    view.findViewById(R.id.etBranchName),
                    view.findViewById(R.id.etBranchAddress));
        }
        loadBranches();
    }

    private void updateLoginUi(MaterialButton btnSave, TextInputEditText etName, TextInputEditText etAddress) {
        boolean in = auth.hasSession();
        tvHint.setVisibility(in ? View.GONE : View.VISIBLE);
        btnSave.setEnabled(in);
        etName.setEnabled(in);
        etAddress.setEnabled(in);
    }

    private void loadBranches() {
        LoadingOverlay.show(loadingOverlay);
        bg.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<Branch>> res =
                    dal.getData(AppwriteCollections.BRANCHES, null, Branch.class);
            List<Branch> mine = new ArrayList<>();
            if (res.success && res.data != null && auth.hasSession()) {
                String uid = auth.getUserId();
                for (Branch b : res.data) {
                    if (uid.equals(b.getOwnerUserId())) {
                        mine.add(b);
                    }
                }
            }
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                try {
                    if (isAdded() && getView() != null) {
                        adapter.setItems(mine);
                    }
                } finally {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }

    private static String text(TextInputEditText e) {
        if (e == null || e.getText() == null) return "";
        return e.getText().toString().trim();
    }
}
