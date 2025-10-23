package com.example.finalproject.activity.guide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.example.finalproject.fragment.ProfileFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GuidePersonalInfoActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private EditText etFirstName, etLastName, etEmail, etPhone, etDob;
    private Button btnSave;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_personal_info);

        ivAvatar = findViewById(R.id.ivAvatar);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDob = findViewById(R.id.etDob);
        btnSave = findViewById(R.id.btnSave);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Không có người dùng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = currentUser.getUid();

        loadUserData();

        etDob.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> updateUserData());
    }

    /**
     * 🔹 Hiển thị thông tin người dùng hiện tại
     */
    private void loadUserData() {
        DocumentReference ref = db.collection("users").document(uid);
        ref.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etFirstName.setText(doc.getString("firstname"));
                etLastName.setText(doc.getString("lastname"));
                etEmail.setText(doc.getString("email"));
                etPhone.setText(doc.getString("phone"));

                Object dobObj = doc.get("dob");
                String dob = "";
                if (dobObj instanceof com.google.firebase.Timestamp) {
                    java.util.Date date = ((com.google.firebase.Timestamp) dobObj).toDate();
                    dob = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
                } else if (dobObj instanceof String) {
                    dob = (String) dobObj;
                }
                etDob.setText(dob);

                String avatarUrl = doc.getString("avatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_person);
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * 🔹 Hiển thị hộp chọn ngày
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    etDob.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    /**
     * 🔹 Cập nhật dữ liệu người dùng vào Firestore
     */
    private void updateUserData() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstname", etFirstName.getText().toString().trim());
        updates.put("lastname", etLastName.getText().toString().trim());
        updates.put("phone", etPhone.getText().toString().trim());

        // 🔹 Chuyển ngày sinh sang Timestamp an toàn
        String dobStr = etDob.getText().toString().trim();
        if (!dobStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                java.util.Date date = sdf.parse(dobStr);
                updates.put("dob", new Timestamp(date));
            } catch (Exception e) {
                Toast.makeText(this, "Định dạng ngày không hợp lệ (dd/MM/yyyy)", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            updates.put("dob", null);
        }

        // 🔹 Thực hiện cập nhật Firestore
        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
                    // ✅ Quay lại trang Hồ sơ sau khi cập nhật thành công
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
