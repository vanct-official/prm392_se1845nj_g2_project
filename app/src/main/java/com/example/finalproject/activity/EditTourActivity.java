package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
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

        // ✅ Kiểm tra tourId
        if (tourId == null || tourId.isEmpty()) {
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

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + " " + getMonthName(month) + " " + year + " at 00:00:00 UTC+7";
                    target.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return months[month];
    }

    private void loadTourData() {
        Log.d(TAG, "Loading tour data for: " + tourId);

        DocumentReference docRef = db.collection("tours").document(tourId);
        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Log.d(TAG, "Document exists, data: " + doc.getData());

                try {
                    // ✅ Load tourName
                    String tourName = doc.getString("tourName");
                    etTourName.setText(tourName != null ? tourName : "");

                    // ✅ Load description
                    String description = doc.getString("description");
                    etDescription.setText(description != null ? description : "");

                    // ✅ Load location
                    String location = doc.getString("location");
                    etLocation.setText(location != null ? location : "");

                    // ✅ Load availableSeats
                    Long seats = doc.getLong("availableSeats");
                    etSeats.setText(seats != null ? String.valueOf(seats) : "0");

                    // ✅ Load price
                    Long price = doc.getLong("price");
                    etPrice.setText(price != null ? String.valueOf(price) : "0");

                    // ✅ Load depositPercent (KHÔNG PHẢI deposit)
                    Long depositPercent = doc.getLong("depositPercent");
                    etDepositPercent.setText(depositPercent != null ? String.valueOf(depositPercent) : "0");

                    // ✅ Load startDate
                    String startDate = doc.getString("startDate");
                    etStartDate.setText(startDate != null ? startDate : "");

                    // ✅ Load endDate
                    String endDate = doc.getString("endDate");
                    etEndDate.setText(endDate != null ? endDate : "");

                    // ✅ Load guideIds (FIX CHÍNH)
                    Object guideIdsObj = doc.get("guideIds");
                    if (guideIdsObj instanceof List) {
                        List<?> guideIdsList = (List<?>) guideIdsObj;
                        if (!guideIdsList.isEmpty() && guideIdsList.get(0) != null) {
                            etGuideId.setText(guideIdsList.get(0).toString());
                        } else {
                            etGuideId.setText("");
                        }
                    } else {
                        etGuideId.setText("");
                    }

                    Log.d(TAG, "Data loaded successfully");

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing document data", e);
                    Toast.makeText(this, "Lỗi đọc dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            } else {
                Log.e(TAG, "Document does not exist");
                Toast.makeText(this, "Tour không tồn tại!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load tour data", e);
            Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void saveChanges() {
        // ✅ Validation
        String tourName = etTourName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String depositPercentStr = etDepositPercent.getText().toString().trim();
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();
        String guideId = etGuideId.getText().toString().trim();

        if (tourName.isEmpty() || description.isEmpty() || location.isEmpty() ||
                seatsStr.isEmpty() || priceStr.isEmpty() || depositPercentStr.isEmpty() ||
                startDate.isEmpty() || endDate.isEmpty() || guideId.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int seats = Integer.parseInt(seatsStr);
            long price = Long.parseLong(priceStr);
            int depositPercent = Integer.parseInt(depositPercentStr);

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
            updates.put("guideIds", java.util.Collections.singletonList(guideId));

            Log.d(TAG, "Updating tour with data: " + updates);

            db.collection("tours").document(tourId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Tour updated successfully");
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update tour", e);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Dữ liệu số không hợp lệ!", Toast.LENGTH_SHORT).show();
        }
    }
}