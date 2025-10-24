package com.example.finalproject.activity.guide;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.finalproject.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class GuidePersonalInfoActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgAvatar;
    private EditText edtFirstName, edtLastName, edtUsername, edtPhone, tvEmail, tvDob;
    private ChipGroup chipGenderGroup;
    private Button btnSave;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private Uri selectedImageUri;
    private String cloudUrl = null;
    private String gender = "";

    private Cloudinary cloudinary;
    private static final String CLOUD_NAME = "dvysaf9on";
    private static final String UPLOAD_PRESET = "xuandai";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_personal_info);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Init Cloudinary
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", CLOUD_NAME
        ));

        initUI();
        loadUserData();

        btnBack.setOnClickListener(v -> onBackPressed());
        imgAvatar.setOnClickListener(v -> selectImageFromGallery());
        tvDob.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveUserData());
    }

    private void initUI() {
        btnBack = findViewById(R.id.btnBack);
        imgAvatar = findViewById(R.id.imgAvatar);
        edtFirstName = findViewById(R.id.edtFirstName);
        edtLastName = findViewById(R.id.edtLastName);
        edtUsername = findViewById(R.id.edtUsername);
        edtPhone = findViewById(R.id.edtPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvDob = findViewById(R.id.tvDob);
        chipGenderGroup = findViewById(R.id.chipGenderGroup);
        btnSave = findViewById(R.id.btnSave);

        chipGenderGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                Chip selectedChip = group.findViewById(checkedId);
                gender = selectedChip.getText().toString();
            }
        });
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                edtFirstName.setText(documentSnapshot.getString("firstname"));
                edtLastName.setText(documentSnapshot.getString("lastname"));
                edtUsername.setText(documentSnapshot.getString("username"));
                tvEmail.setText(documentSnapshot.getString("email"));
                edtPhone.setText(documentSnapshot.getString("phone"));

                // ✅ Handle DOB
                Object dobObj = documentSnapshot.get("dob");
                String dobText = "";
                if (dobObj instanceof Timestamp) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    dobText = sdf.format(((Timestamp) dobObj).toDate());
                } else if (dobObj instanceof String) {
                    dobText = (String) dobObj;
                }
                tvDob.setText(dobText);

                // ✅ Handle Gender (Boolean or String)
                Object genderObj = documentSnapshot.get("gender");
                String genderValue = "";
                if (genderObj instanceof Boolean) {
                    boolean isMale = (Boolean) genderObj;
                    genderValue = isMale ? "Nam" : "Nữ";
                } else if (genderObj != null) {
                    genderValue = genderObj.toString();
                }

                if (genderValue.equalsIgnoreCase("Nam")) {
                    chipGenderGroup.check(R.id.chipMale);
                } else if (genderValue.equalsIgnoreCase("Nữ")) {
                    chipGenderGroup.check(R.id.chipFemale);
                }

                // ✅ Avatar
                String avatarUrl = documentSnapshot.getString("avatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(this).load(avatarUrl).into(imgAvatar);
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imgAvatar.setImageURI(selectedImageUri);
            uploadImageToCloudinary();
        }
    }

    private void uploadImageToCloudinary() {
        new Thread(() -> {
            try {
                Map uploadResult = cloudinary.uploader().upload(
                        getContentResolver().openInputStream(selectedImageUri),
                        ObjectUtils.asMap("upload_preset", UPLOAD_PRESET)
                );
                cloudUrl = uploadResult.get("secure_url").toString();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, (month1 + 1), year1);
                    tvDob.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveUserData() {
        String firstName = edtFirstName.getText().toString().trim();
        String lastName = edtLastName.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String dob = tvDob.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstname", firstName);
        updates.put("lastname", lastName);
        updates.put("username", username);
        updates.put("phone", phone);

        // ✅ Convert dob → Timestamp
        try {
            java.util.Date parsedDate = new SimpleDateFormat("dd/MM/yyyy").parse(dob);
            updates.put("dob", new Timestamp(parsedDate));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ✅ Convert gender → Boolean
        if (gender.equalsIgnoreCase("Nam")) {
            updates.put("gender", true);
        } else if (gender.equalsIgnoreCase("Nữ")) {
            updates.put("gender", false);
        }

        if (cloudUrl != null) updates.put("avatarUrl", cloudUrl);

        userRef.update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}
