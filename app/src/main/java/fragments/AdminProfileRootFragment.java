package fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.cars.R;
import com.google.android.material.button.MaterialButton;

public class AdminProfileRootFragment extends Fragment {

    private MaterialButton btnProfile;
    private MaterialButton btnBusiness;
    private boolean showingProfile = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile_root, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnProfile = view.findViewById(R.id.btnTabProfile);
        btnBusiness = view.findViewById(R.id.btnTabBusiness);

        btnProfile.setOnClickListener(v -> showProfileTab());
        btnBusiness.setOnClickListener(v -> showBusinessTab());

        if (savedInstanceState == null) {
            showProfileTab();
        } else if (savedInstanceState.getBoolean("showing_profile", true)) {
            showProfileTab();
        } else {
            showBusinessTab();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showing_profile", showingProfile);
    }

    private void showProfileTab() {
        showingProfile = true;
        styleTabs(true);
        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction()
                .replace(R.id.flAdminProfileSlot, new ProfaileFragment())
                .commit();
    }

    private void showBusinessTab() {
        showingProfile = false;
        styleTabs(false);
        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction()
                .replace(R.id.flAdminProfileSlot, BusinessInfoFragment.newInstance(true))
                .commit();
    }

    private void styleTabs(boolean profileActive) {
        btnProfile.setAlpha(profileActive ? 1f : 0.55f);
        btnBusiness.setAlpha(profileActive ? 0.55f : 1f);
    }
}
