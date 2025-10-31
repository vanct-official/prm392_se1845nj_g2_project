package com.example.finalproject;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.finalproject.entity.User;
import com.example.finalproject.utils.CloudinaryManager;
import com.example.finalproject.utils.FileUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Màn hình đăng ký tài khoản.
 * Thêm trường: phone (String) và dob (Timestamp)
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etFirstName, etLastName, etEmail, etPassword, etPhone, etDob;
    private RadioButton rbMale, rbFemale;
    private ImageView imgAvatar;
    private MaterialButton btnRegister;

    private Uri selectedImageUri;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Cloudinary cloudinary;

    private Date selectedDobDate;
    private TextView tvBackToLogin;

    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initUI();
        initFirebase();
        setListeners();
    }

    private void initUI() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etDob = findViewById(R.id.etDob);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        cloudinary = CloudinaryManager.getInstance();
    }

    private void setListeners() {
        imgAvatar.setOnClickListener(v -> openGallery());
        etDob.setOnClickListener(v -> openDatePicker());
        btnRegister.setOnClickListener(v -> registerUser());
        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // đóng màn hình đăng ký để tránh quay lại bằng nút back
        });
    }

    /** Mở thư viện ảnh */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    /** Mở DatePickerDialog chọn ngày sinh */
    private void openDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (DatePicker view, int y, int m, int d) -> {
            calendar.set(y, m, d);
            selectedDobDate = calendar.getTime();
            String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDobDate);
            etDob.setText(formatted);
        }, year, month, day);
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imgAvatar.setImageURI(selectedImageUri);
        }
    }

    private boolean isValidPassword(String password) {
        return password.matches(PASSWORD_PATTERN);
    }

    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        boolean gender = rbMale.isChecked();

        // 🔹 Kiểm tra rỗng
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                password.isEmpty() || phone.isEmpty() || selectedDobDate == null) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Kiểm tra định dạng email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Kiểm tra độ mạnh của mật khẩu (ít nhất 8 ký tự, gồm chữ, số và ký tự đặc biệt)
        if (!isValidPassword(password)) {
            Toast.makeText(this,
                    "Mật khẩu phải ≥8 ký tự, gồm chữ, số và ký tự đặc biệt",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // 🔹 Kiểm tra số điện thoại (phải đúng 10 chữ số)
        if (!phone.matches("^\\d{10}$")) {
            Toast.makeText(this, "Số điện thoại phải gồm đúng 10 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Đang tạo tài khoản...");
        progress.setCancelable(false);
        progress.show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        firebaseUser.sendEmailVerification();

                        uploadAvatarAndSaveUser(
                                firebaseUser,
                                firstName,
                                lastName,
                                gender,
                                phone,
                                selectedDobDate,
                                progress
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(this, "Lỗi đăng ký: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadAvatarAndSaveUser(FirebaseUser firebaseUser, String firstName, String lastName,
                                         boolean gender, String phone, Date dob, ProgressDialog progress) {
        new Thread(() -> {
            String avatarUrl = "";

            try {
                if (selectedImageUri != null) {
                    String path = FileUtils.getPath(this, selectedImageUri);
                    if (path != null) {
                        File file = new File(path);
                        Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
                        avatarUrl = uploadResult.get("secure_url").toString();
                    }
                }

                Map<String, Object> userData = new HashMap<>();
                userData.put("firstname", firstName);
                userData.put("lastname", lastName);
                userData.put("email", firebaseUser.getEmail());
                userData.put("gender", gender);
                userData.put("isActive", true);
                userData.put("role", "customer");
                userData.put("avatarUrl", avatarUrl);
                userData.put("phone", phone);
                userData.put("dob", new Timestamp(dob));
                userData.put("createdAt", new Timestamp(new Date()));
                userData.put("updatedAt", new Timestamp(new Date()));

                db.collection("users")
                        .document(firebaseUser.getUid())
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener(unused -> {
                            progress.dismiss();
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Đăng ký thành công! Vui lòng xác thực email.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                        })
                        .addOnFailureListener(e -> {
                            progress.dismiss();
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Lỗi lưu người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        });

            } catch (Exception e) {
                progress.dismiss();
                runOnUiThread(() -> Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e(TAG, "Upload lỗi: ", e);
            }
        }).start();
    }
}
