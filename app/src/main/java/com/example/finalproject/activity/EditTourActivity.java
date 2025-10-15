package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditTourActivity extends AppCompatActivity {

    private static final String TAG = "EditTourActivity";

    private EditText etTourName, etDescription, etLocation, etSeats, etPrice, etDepositPercent, etStartDate, etEndDate, etGuideId;
    private Button btnSave;
    private FirebaseFirestore db;
    private String tourId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tour);

        db = FirebaseFirestore.getInstance();
        tourId = getIntent().getStringExtra("tourId");

        Log.d(TAG, "Tour ID received: " + tourId);

        if (TextUtils.isEmpty(tourId)) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID tour!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khởi tạo views
        etTourName = findViewById(R.id.etTourName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etSeats = findViewById(R.id.etSeats);
        etPrice = findViewById(R.id.etPrice);
        etDepositPercent = findViewById(R.id.etDepositPercent);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etGuideId = findViewById(R.id.etGuideId);
        btnSave = findViewById(R.id.btnSave);

        loadTourData();

        // Chọn ngày
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnSave.setOnClickListener(v -> saveChanges());
    }

    // Hiển thị DatePicker
    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    target.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    // Load dữ liệu tour từ Firestore
    private void loadTourData() {
        DocumentReference docRef = db.collection("tours").document(tourId);
        docRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Tour không tồn tại!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            try {
                etTourName.setText(getStringSafe(doc.getString("tourName")));
                etDescription.setText(getStringSafe(doc.getString("description")));
                etLocation.setText(getStringSafe(doc.getString("location")));

                Object seatsObj = doc.get("availableSeats");
                etSeats.setText(seatsObj != null ? String.valueOf(seatsObj) : "0");

                Object priceObj = doc.get("price");
                etPrice.setText(priceObj != null ? String.valueOf(priceObj) : "0");

                Object depositObj = doc.get("depositPercent");
                etDepositPercent.setText(depositObj != null ? String.valueOf(depositObj) : "0");

                Object startDateObj = doc.get("startDate");
                etStartDate.setText(startDateObj != null ? startDateObj.toString() : "");

                Object endDateObj = doc.get("endDate");
                etEndDate.setText(endDateObj != null ? endDateObj.toString() : "");

                Object guideIdsObj = doc.get("guideIds");
                if (guideIdsObj instanceof List) {
                    List<?> guideList = (List<?>) guideIdsObj;
                    if (!guideList.isEmpty() && guideList.get(0) != null) {
                        String guideId = guideList.get(0).toString();
                        etGuideId.setText(guideId);
                        loadGuideName(guideId);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Lỗi đọc dữ liệu tour", e);
                Toast.makeText(this, "Lỗi đọc dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi tải dữ liệu tour", e);
            Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        });
    }

    // Load tên hướng dẫn viên
    private void loadGuideName(String guideId) {
        db.collection("guides").document(guideId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String guideName = doc.getString("name");
                        if (guideName != null && !guideName.isEmpty()) {
                            etGuideId.setHint("Hướng dẫn viên: " + guideName);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tải tên hướng dẫn viên", e));
    }

    // Hàm an toàn lấy String
    private String getStringSafe(String value) {
        return value != null ? value : "";
    }

    // Lưu thay đổi
    private void saveChanges() {
        String tourName = etTourName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String depositStr = etDepositPercent.getText().toString().trim();
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();
        String guideId = etGuideId.getText().toString().trim();

        if (TextUtils.isEmpty(tourName) || TextUtils.isEmpty(description) || TextUtils.isEmpty(location) ||
                TextUtils.isEmpty(seatsStr) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(depositStr) ||
                TextUtils.isEmpty(startDate) || TextUtils.isEmpty(endDate) || TextUtils.isEmpty(guideId)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int seats = Integer.parseInt(seatsStr);
            long price = Long.parseLong(priceStr);
            int depositPercent = Integer.parseInt(depositStr);

            if (depositPercent < 0 || depositPercent > 100) {
                Toast.makeText(this, "Phần trăm đặt cọc phải từ 0-100!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("tourName", tourName);
            updates.put("description", description);
            updates.put("location", location);
            updates.put("availableSeats", seats);
            updates.put("price", price);
            updates.put("depositPercent", depositPercent);
            updates.put("startDate", startDate);
            updates.put("endDate", endDate);
            updates.put("guideIds", Collections.singletonList(guideId));

            db.collection("tours").document(tourId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show());

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Dữ liệu số không hợp lệ!", Toast.LENGTH_SHORT).show();
        }
    }
}
