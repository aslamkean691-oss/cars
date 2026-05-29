package com.example.cars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.cars.Hellper.AuthSessionManager;
import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.LoadExecutor;
import com.example.cars.Hellper.SuperAdminConfig;
import com.example.cars.model.AdminSecurityState;
import com.example.cars.model.AppwriteCollections;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

import fragments.AdminProfileRootFragment;
import fragments.BusinessInfoFragment;
import fragments.FavoritesFragment;
import fragments.MenuFragment;
import fragments.SoldCarsFragment;

public class StartActivity2 extends AppCompatActivity {

    /**
     * כאשר true (למשל אחרי {@link LoginActivity}) — יוצאים ממעטפת הלקוח ומציגים ממשק מנהל מיד.
     * בהפעלה מהמשגר בלי extra — נכנסים למעטפת לקוח; סשן מנהל נשמר בדיסק ואינו נמחק.
     */
    public static final String EXTRA_OPENED_AFTER_AUTH = "opened_after_auth";

    private static final String STATE_CUSTOMER_SHELL = "customer_shell_mode";

    private static final String TAG = "StartActivity2";
    private static final int ADMIN_TAPS_REQUIRED = 4;
    private static final long ADMIN_TAP_RESET_MS = 2200L;
    private static final long PASSWORD_REMINDER_DEBOUNCE_MS = 700L;

    BottomNavigationView bth;
    private AuthSessionManager auth;
    private DALAppWriteConnection dal;
    private int adminTapCount = 0;
    private long lastAdminTapTime = 0;
    private long lastPasswordReminderAt;
    private final Executor bg = LoadExecutor.io();
    /**
     * true = ממשק לקוח (תפריט תחתון של לקוח) גם כשיש סשן מנהל שמור — בלי למחוק JWT.
     * מעבר למנהל: 4 הקשות על הלוגו; אם יש סשן — בלי מסך סיסמה.
     */
    private boolean customerShellMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start2);

        auth = new AuthSessionManager(this);
        dal = new DALAppWriteConnection(this);
        if (savedInstanceState != null) {
            customerShellMode = savedInstanceState.getBoolean(STATE_CUSTOMER_SHELL, true);
        } else {
            customerShellMode = !getIntent().getBooleanExtra(EXTRA_OPENED_AFTER_AUTH, false);
        }
        auth.restoreDalIfNeeded(dal);

        LinearLayout appBarHeader = findViewById(R.id.appBarHeader);
        applyStatusBarPadding(appBarHeader);

        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvToolbarTitle.setText(showingAdminUi()
                ? getString(R.string.admin_toolbar_title)
                : getString(R.string.app_name));

        ImageView ivLogo = findViewById(R.id.ivToolbarCarLogo);
        ivLogo.setOnClickListener(v -> onCarLogoTap());

        bth = findViewById(R.id.bth);
        setupBottomNavigation();

        if (savedInstanceState == null) {
            changeFragment(MenuFragment.newInstance(showingAdminUi()));
        }

        bth.setOnItemSelectedListener(menuItem -> {
            onBottomItemSelected(menuItem.getItemId());
            return true;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_CUSTOMER_SHELL, customerShellMode);
    }

    /** ממשק מנהל (תפריט + כותרת) — רק כשיש סשן ולא במעטפת לקוח */
    private boolean showingAdminUi() {
        return auth.hasSession() && !customerShellMode;
    }

    private void setupBottomNavigation() {
        bth.getMenu().clear();
        if (showingAdminUi()) {
            getMenuInflater().inflate(R.menu.bottom_admin, bth.getMenu());
            bth.setSelectedItemId(R.id.nav_admin_cars);
        } else {
            getMenuInflater().inflate(R.menu.bottom_customer, bth.getMenu());
            bth.setSelectedItemId(R.id.nav_customer_cars);
        }
    }

    private void onBottomItemSelected(int id) {
        if (showingAdminUi()) {
            if (id == R.id.nav_admin_cars) {
                changeFragment(MenuFragment.newInstance(true));
            } else if (id == R.id.nav_admin_sold) {
                changeFragment(new SoldCarsFragment());
            } else if (id == R.id.nav_admin_profile) {
                changeFragment(new AdminProfileRootFragment());
            }
        } else {
            if (id == R.id.nav_customer_cars) {
                changeFragment(MenuFragment.newInstance(false));
            } else if (id == R.id.nav_customer_favorites) {
                changeFragment(new FavoritesFragment());
            } else if (id == R.id.nav_customer_business) {
                changeFragment(BusinessInfoFragment.newInstance(false));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSuperAdminPasswordReminderIfNeeded();
    }

    private void checkSuperAdminPasswordReminderIfNeeded() {
        if (!showingAdminUi() || !SuperAdminConfig.isSuperAdminEmail(auth.getEmail())) {
            return;
        }

        bg.execute(() -> {
            dal.restoreSessionFromJwt(auth.getSessionJwt(), auth.getUserId(), auth.getEmail());
            AdminSecurityState state = dal.findAdminSecurityState(auth.getUserId());
            boolean mustRemind = state == null || !state.isHasChangedDefaultPassword();
            runOnUiThread(() -> {
                if (isFinishing() || !mustRemind) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - lastPasswordReminderAt < PASSWORD_REMINDER_DEBOUNCE_MS) {
                    return;
                }
                lastPasswordReminderAt = now;
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.super_admin_change_password_title)
                        .setMessage(R.string.super_admin_change_password_message)
                        .setPositiveButton(R.string.change_password_now, (d, w) -> showChangePasswordDialog())
                        .setNegativeButton(android.R.string.ok, null)
                        .show();
            });
        });
    }

    private void showChangePasswordDialog() {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null, false);
        TextInputEditText etOld = form.findViewById(R.id.etOldPassword);
        TextInputEditText etNew = form.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = form.findViewById(R.id.etConfirmPassword);

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_password_now)
                .setView(form)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = b.create();
        dialog.setOnShowListener(d -> {
            Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            pos.setOnClickListener(v -> {
                String oldP = text(etOld);
                String newP = text(etNew);
                String confirm = text(etConfirm);
                if (oldP.isEmpty() || newP.isEmpty()) {
                    Log.w(TAG, "change password: empty fields");
                    return;
                }
                if (newP.length() < 8) {
                    Log.w(TAG, "change password: new password too short");
                    return;
                }
                if (!newP.equals(confirm)) {
                    Log.w(TAG, "change password: mismatch");
                    return;
                }
                pos.setEnabled(false);
                bg.execute(() -> {
                    dal.restoreSessionFromJwt(auth.getSessionJwt(), auth.getUserId(), auth.getEmail());
                    DALAppWriteConnection.OperationResult<Void> res =
                            dal.updateAccountPassword(oldP, newP);
                    if (res.success) {
                        AdminSecurityState existing = dal.findAdminSecurityState(auth.getUserId());
                        if (existing == null) {
                            AdminSecurityState n = new AdminSecurityState();
                            n.setUserId(auth.getUserId());
                            n.setEmail(auth.getEmail());
                            n.setHasChangedDefaultPassword(true);
                            dal.saveData(n, AppwriteCollections.ADMIN_SECURITY, null);
                        } else {
                            dal.markSuperAdminPasswordChanged(existing.getId());
                        }
                    }
                    runOnUiThread(() -> {
                        pos.setEnabled(true);
                        if (res.success) {
                            dialog.dismiss();
                            Log.i(TAG, getString(R.string.password_updated));
                        } else {
                            Log.e(TAG, "change password failed: " + (res.message != null ? res.message : "שגיאה"));
                        }
                    });
                });
            });
        });
        dialog.show();
    }

    private static String text(TextInputEditText e) {
        if (e == null || e.getText() == null) return "";
        return e.getText().toString();
    }

    private void applyStatusBarPadding(@NonNull View header) {
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            int extra = getResources().getDimensionPixelSize(R.dimen.app_bar_padding_top_extra);
            v.setPadding(v.getPaddingLeft(), insets.top + extra, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    private void onCarLogoTap() {
        long now = System.currentTimeMillis();
        if (now - lastAdminTapTime > ADMIN_TAP_RESET_MS) {
            adminTapCount = 0;
        }
        lastAdminTapTime = now;
        adminTapCount++;
        if (adminTapCount >= ADMIN_TAPS_REQUIRED) {
            adminTapCount = 0;
            Log.i(TAG, getString(R.string.admin_mode_login));
            if (auth.hasSession()) {
                enterAdminModeFromSavedSession();
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        }
    }

    private void enterAdminModeFromSavedSession() {
        customerShellMode = false;
        auth.restoreDalIfNeeded(dal);
        setupBottomNavigation();
        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvToolbarTitle.setText(getString(R.string.admin_toolbar_title));
        changeFragment(MenuFragment.newInstance(true));
        bth.setSelectedItemId(R.id.nav_admin_cars);
    }

    private void changeFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.btflag, fragment);
        ft.commit();
    }
}
