package com.example.cars;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cars.Hellper.AuthSessionManager;
import com.example.cars.Hellper.DALAppWriteConnection;
import com.example.cars.Hellper.LoadExecutor;
import com.example.cars.Hellper.SuperAdminConfig;
import com.example.cars.util.LoadingOverlay;
import com.example.cars.util.LotToolbarInsets;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    /** סינון ב-Logcat ללא תלות ב-Verbose */
    private static final String TAG_LOGIN_UI = "APPWRITE_LOGIN";
    private final Executor bg = LoadExecutor.io();
    private AuthSessionManager auth;
    private DALAppWriteConnection dal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        View lotBar = findViewById(R.id.lotToolbarRoot);
        LotToolbarInsets.apply(this, lotBar);
        findViewById(R.id.btnLotToolbarBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        ((TextView) findViewById(R.id.tvLotToolbarTitle)).setText(getString(R.string.screen_login_bar));

        auth = new AuthSessionManager(this);
        dal = new DALAppWriteConnection(this);

        if (auth.hasSession()) {
            dal.restoreSessionFromJwt(auth.getSessionJwt(), auth.getUserId(), auth.getEmail());
            goMain();
            return;
        }

        TextInputEditText etEmail = findViewById(R.id.etLoginEmail);
        TextInputEditText etPassword = findViewById(R.id.etLoginPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnGuest = findViewById(R.id.btnGuest);
        final View loadingOverlay = findViewById(R.id.loadingOverlay);

        btnLogin.setOnClickListener(v -> {
            String email = text(etEmail);
            String password = text(etPassword);
            if (email.isEmpty() || password.isEmpty()) {
                Log.w(TAG_LOGIN_UI, "validation: empty email or password");
                return;
            }
            Log.w(TAG_LOGIN_UI, "btnLogin clicked email=" + email);
            btnLogin.setEnabled(false);
            LoadingOverlay.show(loadingOverlay);
            bg.execute(() -> {
                DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> res;
                try {
                    res = dal.loginUser(email, password);
                    if (res.success && res.data != null && res.data.sessionJwt != null) {
                        dal.restoreSessionFromJwt(
                                res.data.sessionJwt, res.data.userId, res.data.email);
                        if (SuperAdminConfig.isSuperAdminEmail(email)) {
                            dal.ensureAdminSecurityRecord(res.data.userId, email);
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG_LOGIN_UI, "login thread crashed", t);
                    Log.e(TAG, "חריגה ב-thread התחברות", t);
                    res = new DALAppWriteConnection.OperationResult<>(false,
                            "שגיאה פנימית: " + t.getClass().getSimpleName() + " — " + t.getMessage());
                }
                final DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> resFinal = res;
                runOnUiThread(() -> {
                    try {
                        btnLogin.setEnabled(true);
                        if (resFinal.success && resFinal.data != null && resFinal.data.sessionJwt != null) {
                            auth.saveSession(resFinal.data.sessionJwt, resFinal.data.userId, resFinal.data.email);
                            Log.i(TAG_LOGIN_UI, "LOGIN_OK userId=" + resFinal.data.userId);
                            goMain();
                        } else {
                            String msg = resFinal.message != null ? resFinal.message : "שגיאת התחברות";
                            Log.e(TAG_LOGIN_UI, "LOGIN_FAIL " + msg);
                            Log.e(TAG, "התחברות נכשלה: " + msg);
                        }
                    } finally {
                        LoadingOverlay.hide(loadingOverlay);
                    }
                });
            });
        });

        btnGuest.setOnClickListener(v -> {
            auth.clear();
            goMain();
        });
    }

    private static String text(TextInputEditText e) {
        if (e == null || e.getText() == null) return "";
        return e.getText().toString().trim();
    }

    private void goMain() {
        Intent i = new Intent(this, StartActivity2.class);
        i.putExtra(StartActivity2.EXTRA_OPENED_AFTER_AUTH, true);
        startActivity(i);
        finish();
    }
}
