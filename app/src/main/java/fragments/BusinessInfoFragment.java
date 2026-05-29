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

import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.LoadExecutor;
import com.example.cars.R;
import com.example.cars.model.AppwriteCollections;
import com.example.cars.util.LoadingOverlay;
import com.example.cars.model.Branch;
import com.example.cars.model.LotProfile;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class BusinessInfoFragment extends Fragment {

    private static final String ARG_ALLOW_MANAGE_BRANCHES = "allow_manage_branches";

    private final Executor bg = LoadExecutor.io();

    public static BusinessInfoFragment newInstance(boolean allowManageBranches) {
        BusinessInfoFragment f = new BusinessInfoFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_MANAGE_BRANCHES, allowManageBranches);
        f.setArguments(args);
        return f;
    }

    private boolean allowManageBranches() {
        Bundle a = getArguments();
        return a != null && a.getBoolean(ARG_ALLOW_MANAGE_BRANCHES, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_business_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButton btnManage = view.findViewById(R.id.btnManageBranches);
        btnManage.setVisibility(allowManageBranches() ? View.VISIBLE : View.GONE);
        btnManage.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.btflag, new SettingFragment())
                .addToBackStack("branches")
                .commit());

        TextView tvOwner = view.findViewById(R.id.tvBizOwner);
        TextView tvName = view.findViewById(R.id.tvBizName);
        TextView tvPhone = view.findViewById(R.id.tvBizPhone);
        TextView tvAddr = view.findViewById(R.id.tvBizAddress);
        TextView tvBranches = view.findViewById(R.id.tvBranchesList);
        View loadingOverlay = view.findViewById(R.id.loadingOverlay);
        LoadingOverlay.show(loadingOverlay);

        Context appCtx = requireContext().getApplicationContext();
        bg.execute(() -> {
            DALAppWriteConnection dal = new DALAppWriteConnection(appCtx);
            DALAppWriteConnection.OperationResult<ArrayList<LotProfile>> pr =
                    dal.getData(AppwriteCollections.LOT_PROFILES, null, LotProfile.class);
            DALAppWriteConnection.OperationResult<ArrayList<Branch>> br =
                    dal.getData(AppwriteCollections.BRANCHES, null, Branch.class);

            LotProfile p = null;
            if (pr.success && pr.data != null && !pr.data.isEmpty()) {
                p = pr.data.get(0);
            }

            StringBuilder branchesText = new StringBuilder();
            if (br.success && br.data != null) {
                for (Branch b : br.data) {
                    String line = (b.getBranchName() != null && !b.getBranchName().isEmpty())
                            ? b.getBranchName()
                            : (b.getAddress() != null ? b.getAddress() : "");
                    if (!line.isEmpty()) {
                        branchesText.append("• ").append(line).append("\n");
                    }
                }
            }
            if (branchesText.length() == 0) {
                branchesText.append("—");
            }

            LotProfile finalP = p;
            String branchesStr = branchesText.toString();
            FragmentActivity act = getActivity();
            if (act == null) {
                return;
            }
            act.runOnUiThread(() -> {
                try {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    if (finalP == null) {
                        tvOwner.setText("");
                        tvName.setText(R.string.business_info_title);
                        tvPhone.setText("");
                        tvAddr.setText("—");
                    } else {
                        tvOwner.setText(labelLine("בעלים / איש קשר", finalP.getOwnerName()));
                        tvName.setText(labelLine("שם עסק", finalP.getBusinessName()));
                        tvPhone.setText(labelLine("טלפון", finalP.getPhone()));
                        tvAddr.setText(labelLine("כתובת", finalP.getAddress()));
                    }
                    tvBranches.setText(branchesStr.trim());
                } finally {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }

    private static String labelLine(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return label + ": —";
        }
        return label + ": " + value.trim();
    }
}
