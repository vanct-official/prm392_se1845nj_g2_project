package com.example.finalproject.activity.authen;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Màn hình quên mật khẩu — gửi link khôi phục đến email người dùng.
 * Giao diện đồng bộ phong cách TourioVN.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText etEmail;
    private MaterialButton btnSendLink, btnBack;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initUI();
        initFirebase();
        setListeners();
    }

    /** 🔹 Khởi tạo Firebase */
    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
    }

    /** 🔹 Ánh xạ UI */
    private void initUI() {
        etEmail = findViewById(R.id.etEmailForgot);
        btnSendLink = findViewById(R.id.btnSendReset);
        btnBack = findViewById(R.id.btnBackLogin);
    }

    /** 🔹 Xử lý sự kiện */
    private void setListeners() {
        btnSendLink.setOnClickListener(v -> sendResetLink());
        btnBack.setOnClickListener(v -> finish());
    }

    /** ✉️ Gửi email khôi phục */
    private void sendResetLink() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã gửi link đặt lại mật khẩu đến: " + email, Toast.LENGTH_LONG).show();
                    finish(); // Quay lại màn hình đăng nhập
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Reset password error: " + e.getMessage());
                    Toast.makeText(this, "Không thể gửi email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
