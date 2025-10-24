package com.example.finalproject.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // ✅ thêm import này
import android.widget.Toast;

import com.example.finalproject.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword, etConfirmPassword;
    private Button btnChangePassword;
    private ImageButton btnBack; // ✅ khai báo ở đây
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // ✅ ánh xạ view
        btnBack = findViewById(R.id.btnBack);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // ✅ Firebase
        mAuth = FirebaseAuth.getInstance();

        // ✅ Nút back
        btnBack.setOnClickListener(v -> onBackPressed());

        // ✅ Nút đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Không tìm thấy người dùng đang đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Thực hiện xác thực và đổi mật khẩu
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                        finish(); // Quay lại trang trước
                    } else {
                        Toast.makeText(this, "Lỗi: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
