package fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.cars.Hellper.AuthSessionManager;
import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.LoadExecutor;
import com.example.cars.LoginActivity;
import com.example.cars.R;
import com.example.cars.model.AppwriteCollections;
import com.example.cars.util.LoadingOverlay;
import com.example.cars.model.LotProfile;
import com.example.cars.util.AppwriteGlideUrl;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public class ProfaileFragment extends Fragment {

    private static final String TAG = "ProfaileFragment";
    private final Executor bg = LoadExecutor.io();
    private AuthSessionManager auth;
    private DALAppWriteConnection dal;
    private String profileDocId;
    private ImageView ivPhoto;
    private String profileImageUrl = "";
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> requestCameraPermission;
    private ActivityResultLauncher<String> requestReadImagesPermission;
    @Nullable
    private View loadingOverlay;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestCameraPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCameraIntent();
                    } else {
                        Log.w(TAG, "הרשאת מצלמה נדחתה — לא ניתן לצלם");
                    }
                });
        requestReadImagesPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchGalleryIntent();
                    } else {
                        Log.w(TAG, "הרשאת גלריה נדחתה");
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profaile, container, false);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        auth = new AuthSessionManager(requireContext());
        dal = new DALAppWriteConnection(requireContext());
        auth.restoreDalIfNeeded(dal);

        TextView tvGuest = view.findViewById(R.id.tvProfileGuest);
        ivPhoto = view.findViewById(R.id.ivProfilePhoto);
        MaterialButton btnPick = view.findViewById(R.id.btnPickProfileImage);
        TextInputEditText etName = view.findViewById(R.id.etOwnerName);
        TextInputEditText etBiz = view.findViewById(R.id.etBusinessName);
        TextInputEditText etAddr = view.findViewById(R.id.etProfileAddress);
        TextInputEditText etPhone = view.findViewById(R.id.etProfilePhone);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveProfile);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        boolean loggedIn = auth.hasSession();
        tvGuest.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        btnPick.setEnabled(loggedIn);
        btnSave.setEnabled(loggedIn);
        etName.setEnabled(loggedIn);
        etBiz.setEnabled(loggedIn);
        etAddr.setEnabled(loggedIn);
        etPhone.setEnabled(loggedIn);
        btnLogout.setVisibility(loggedIn ? View.VISIBLE : View.GONE);

        registerPickers();

        btnPick.setOnClickListener(v -> showImageSourceDialog());

        btnSave.setOnClickListener(v -> {
            if (!loggedIn) return;
            LotProfile p = new LotProfile();
            p.setId(profileDocId);
            p.setOwnerUserId(auth.getUserId());
            p.setOwnerName(text(etName));
            p.setBusinessName(text(etBiz));
            p.setAddress(text(etAddr));
            p.setPhone(text(etPhone));
            p.setProfileImageUrl(profileImageUrl);

            btnSave.setEnabled(false);
            LoadingOverlay.show(loadingOverlay);
            bg.execute(() -> {
                DALAppWriteConnection.OperationResult<?> res;
                if (profileDocId != null && !profileDocId.isEmpty()) {
                    res = dal.updateData(p, AppwriteCollections.LOT_PROFILES, profileDocId, null);
                } else {
                    res = dal.saveData(p, AppwriteCollections.LOT_PROFILES, null);
                }
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) {
                        LoadingOverlay.hide(loadingOverlay);
                        return;
                    }
                    btnSave.setEnabled(true);
                    if (res.success) {
                        Log.i(TAG, "profile saved");
                        loadProfile(etName, etBiz, etAddr, etPhone);
                    } else {
                        Log.e(TAG, "profile save failed: " + (res.message != null ? res.message : "שגיאה"));
                        LoadingOverlay.hide(loadingOverlay);
                    }
                });
            });
        });

        btnLogout.setOnClickListener(v -> {
            LoadingOverlay.show(loadingOverlay);
            bg.execute(() -> {
                dal.logoutUser();
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    try {
                        if (!isAdded()) {
                            return;
                        }
                        auth.clear();
                        Intent i = new Intent(getActivity(), LoginActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    } finally {
                        LoadingOverlay.hide(loadingOverlay);
                    }
                });
            });
        });

        if (loggedIn) {
            loadProfile(etName, etBiz, etAddr, etPhone);
        }

        return view;
    }

    private void registerPickers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (getActivity() == null
                            || result.getResultCode() != requireActivity().RESULT_OK
                            || result.getData() == null) return;
                    Bundle extras = result.getData().getExtras();
                    if (extras == null) return;
                    Bitmap bmp = (Bitmap) extras.get("data");
                    if (bmp != null) uploadBitmap(bmp);
                });
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (getActivity() == null
                            || result.getResultCode() != requireActivity().RESULT_OK
                            || result.getData() == null) return;
                    Uri uri = result.getData().getData();
                    if (uri == null) return;
                    try {
                        InputStream is = requireContext().getContentResolver().openInputStream(uri);
                        Bitmap bmp = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                        if (bmp != null) uploadBitmap(bmp);
                        if (is != null) is.close();
                    } catch (Exception e) {
                        Log.e(TAG, "gallery read", e);
                    }
                });
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("תמונת פרופיל")
                .setItems(new CharSequence[]{"מצלמה", "גלריה"}, (d, which) -> {
                    if (which == 0) {
                        openCameraWithRuntimePermission();
                    } else {
                        openGalleryWithRuntimePermission();
                    }
                })
                .show();
    }

    private void openCameraWithRuntimePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCameraIntent();
    }

    private void launchCameraIntent() {
        if (getActivity() == null) {
            return;
        }
        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }

    private void openGalleryWithRuntimePermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestReadImagesPermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestReadImagesPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                return;
            }
        }
        launchGalleryIntent();
    }

    private void launchGalleryIntent() {
        if (getActivity() == null) {
            return;
        }
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");
        galleryLauncher.launch(pick);
    }

    private void uploadBitmap(Bitmap bitmap) {
        Log.d(TAG, "upload profile image start");
        LoadingOverlay.show(loadingOverlay);
        bg.execute(() -> {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] bytes = stream.toByteArray();
                String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
                DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> up =
                        dal.uploadFile(bytes, fileName, "image/jpeg", null);
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    try {
                        if (!isAdded() || getView() == null) {
                            return;
                        }
                        if (up.success && up.data != null) {
                            profileImageUrl = up.data.fileUrl;
                            Glide.with(this).load(AppwriteGlideUrl.withHeaders(profileImageUrl)).centerCrop().into(ivPhoto);
                            Log.i(TAG, "profile image uploaded");
                        } else {
                            Log.e(TAG, "profile image upload failed: " + (up.message != null ? up.message : "שגיאת העלאה"));
                        }
                    } finally {
                        LoadingOverlay.hide(loadingOverlay);
                    }
                });
            } catch (Exception e) {
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Log.e(TAG, "upload bitmap", e);
                        LoadingOverlay.hide(loadingOverlay);
                    });
                }
            }
        });
    }

    private void loadProfile(TextInputEditText etName, TextInputEditText etBiz,
                             TextInputEditText etAddr, TextInputEditText etPhone) {
        LoadingOverlay.show(loadingOverlay);
        bg.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<LotProfile>> res =
                    dal.getData(AppwriteCollections.LOT_PROFILES, null, LotProfile.class);
            LotProfile mine = null;
            if (res.success && res.data != null) {
                String uid = auth.getUserId();
                for (LotProfile p : res.data) {
                    if (uid.equals(p.getOwnerUserId())) {
                        mine = p;
                        break;
                    }
                }
            }
            if (getActivity() == null) return;
            LotProfile finalMine = mine;
            requireActivity().runOnUiThread(() -> {
                try {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    if (finalMine == null) {
                        profileDocId = null;
                        profileImageUrl = "";
                        etName.setText("");
                        etBiz.setText("");
                        etAddr.setText("");
                        etPhone.setText("");
                        ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                        return;
                    }
                    profileDocId = finalMine.getId();
                    profileImageUrl = finalMine.getProfileImageUrl() != null ? finalMine.getProfileImageUrl() : "";
                    etName.setText(nz(finalMine.getOwnerName()));
                    etBiz.setText(nz(finalMine.getBusinessName()));
                    etAddr.setText(nz(finalMine.getAddress()));
                    etPhone.setText(nz(finalMine.getPhone()));
                    if (!profileImageUrl.isEmpty()) {
                        Glide.with(this).load(AppwriteGlideUrl.withHeaders(profileImageUrl)).centerCrop().into(ivPhoto);
                    }
                } finally {
                    LoadingOverlay.hide(loadingOverlay);
                }
            });
        });
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String text(TextInputEditText e) {
        if (e == null || e.getText() == null) return "";
        return e.getText().toString().trim();
    }
}
