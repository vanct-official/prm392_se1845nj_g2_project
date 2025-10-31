package com.example.finalproject.activity.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.example.finalproject.entity.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminUserDetailActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserPhone, tvUserRole, tvUserStatus;
    private Button btnToggleActive;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private String userId;
    private User currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_detail);

        db = FirebaseFirestore.getInstance();

        // 🔹 Ánh xạ view
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvUserRole = findViewById(R.id.tvUserRole);
        tvUserStatus = findViewById(R.id.tvUserStatus);
        btnToggleActive = findViewById(R.id.btnToggleActive);
        btnBack = findViewById(R.id.btnBackUserDetail);

        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy người dùng!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ⬅️ Quay lại
        btnBack.setOnClickListener(v -> onBackPressed());

        loadUserDetail();

        // ✅ Toggle trạng thái tài khoản
        btnToggleActive.setOnClickListener(v -> {
            if (currentUser != null) {
                toggleUserStatus();
            }
        });
    }

    /** 🔹 Lấy thông tin chi tiết người dùng */
    private void loadUserDetail() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUser = doc.toObject(User.class);
                        if (currentUser != null) {
                            currentUser.setUserid(doc.getId());
                            displayUserInfo(currentUser);
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** 🔹 Hiển thị thông tin người dùng */
    private void displayUserInfo(User user) {
        String fullName = (user.getFirstname() != null ? user.getFirstname() : "") + " " +
                (user.getLastname() != null ? user.getLastname() : "");
        tvUserName.setText(fullName.trim().isEmpty() ? "Không có tên" : fullName.trim());
        tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "Không có email");
        tvUserPhone.setText(user.getPhone() != null ? user.getPhone() : "Không có số điện thoại");

        // ✅ Chuyển đổi vai trò hiển thị
        String role = user.getRole();
        String roleLabel = "Không xác định";
        if (role != null) {
            switch (role) {
                case "customer":
                    roleLabel = "Khách hàng";
                    break;
                case "guide":
                    roleLabel = "Hướng dẫn viên";
                    break;
                case "admin":
                    roleLabel = "Quản trị viên";
                    break;
            }
        }
        tvUserRole.setText(roleLabel);

        // ✅ Trạng thái
        boolean active = user.getIsActive();
        tvUserStatus.setText(active ? "Hoạt động" : "Đã vô hiệu hóa");

        // ✅ Nút
        btnToggleActive.setText(active ? "Vô hiệu hóa" : "Kích hoạt");
        btnToggleActive.setBackgroundColor(
                getColor(active ? R.color.status_cancelled : R.color.status_confirmed)
        );

        // 🚫 Nếu là admin → disable nút vô hiệu hóa
        if ("admin".equalsIgnoreCase(user.getRole())) {
            btnToggleActive.setEnabled(false);
            btnToggleActive.setBackgroundColor(getColor(R.color.gray_disabled));
            btnToggleActive.setText("Vô hiệu hóa");
        }
    }

    /** 🔹 Cập nhật trạng thái người dùng */
    private void toggleUserStatus() {
        // 🚫 Chặn thao tác với admin
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            Toast.makeText(this, "Không thể vô hiệu hóa tài khoản quản trị viên!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newStatus = !currentUser.getIsActive();
        DocumentReference ref = db.collection("users").document(userId);

        ref.update("isActive", newStatus)
                .addOnSuccessListener(unused -> {
                    currentUser.setIsActive(newStatus);
                    displayUserInfo(currentUser);
                    Toast.makeText(this,
                            "Đã " + (newStatus ? "kích hoạt" : "vô hiệu hóa") + " tài khoản",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
