//package com.example.finalproject.activity;
//
//import android.app.DatePickerDialog;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.finalproject.R;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.util.Calendar;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class EditTourActivity extends AppCompatActivity {
//
//    private static final String TAG = "EditTourActivity";
//
//    private EditText etTourName, etDescription, etLocation, etSeats, etPrice, etDepositPercent, etStartDate, etEndDate, etGuideId;
//    private Button btnSave;
//    private FirebaseFirestore db;
//    private String tourId;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_edit_tour);
//
//        db = FirebaseFirestore.getInstance();
//        tourId = getIntent().getStringExtra("tourId");
//
//        Log.d(TAG, "Tour ID received: " + tourId);
//
//        if (TextUtils.isEmpty(tourId)) {
//            Toast.makeText(this, "Lỗi: Không tìm thấy ID tour!", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        // Khởi tạo views
//        etTourName = findViewById(R.id.etTourName);
//        etDescription = findViewById(R.id.etDescription);
//        etLocation = findViewById(R.id.etLocation);
//        etSeats = findViewById(R.id.etSeats);
//        etPrice = findViewById(R.id.etPrice);
//        etDepositPercent = findViewById(R.id.etDepositPercent);
//        etStartDate = findViewById(R.id.etStartDate);
//        etEndDate = findViewById(R.id.etEndDate);
//        etGuideId = findViewById(R.id.etGuideId);
//        btnSave = findViewById(R.id.btnSave);
//
//        loadTourData();
//
//        // Chọn ngày
//        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
//        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
//
//        btnSave.setOnClickListener(v -> saveChanges());
//    }
//
//    // Hiển thị DatePicker
//    private void showDatePicker(EditText target) {
//        Calendar calendar = Calendar.getInstance();
//        DatePickerDialog dialog = new DatePickerDialog(this,
//                (view, year, month, dayOfMonth) -> {
//                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
//                    target.setText(date);
//                },
//                calendar.get(Calendar.YEAR),
//                calendar.get(Calendar.MONTH),
//                calendar.get(Calendar.DAY_OF_MONTH));
//        dialog.show();
//    }
//
//    // Load dữ liệu tour từ Firestore
//    private void loadTourData() {
//        DocumentReference docRef = db.collection("tours").document(tourId);
//        docRef.get().addOnSuccessListener(doc -> {
//            if (!doc.exists()) {
//                Toast.makeText(this, "Tour không tồn tại!", Toast.LENGTH_SHORT).show();
//                finish();
//                return;
//            }
//
//            try {
//                etTourName.setText(getStringSafe(doc.getString("tourName")));
//                etDescription.setText(getStringSafe(doc.getString("description")));
//                etLocation.setText(getStringSafe(doc.getString("location")));
//
//                Object seatsObj = doc.get("availableSeats");
//                etSeats.setText(seatsObj != null ? String.valueOf(seatsObj) : "0");
//
//                Object priceObj = doc.get("price");
//                etPrice.setText(priceObj != null ? String.valueOf(priceObj) : "0");
//
//                Object depositObj = doc.get("depositPercent");
//                etDepositPercent.setText(depositObj != null ? String.valueOf(depositObj) : "0");
//
//                Object startDateObj = doc.get("startDate");
//                etStartDate.setText(startDateObj != null ? startDateObj.toString() : "");
//
//                Object endDateObj = doc.get("endDate");
//                etEndDate.setText(endDateObj != null ? endDateObj.toString() : "");
//
//                Object guideIdsObj = doc.get("guideIds");
//                if (guideIdsObj instanceof List) {
//                    List<?> guideList = (List<?>) guideIdsObj;
//                    if (!guideList.isEmpty() && guideList.get(0) != null) {
//                        String guideId = guideList.get(0).toString();
//                        etGuideId.setText(guideId);
//                        loadGuideName(guideId);
//                    }
//                }
//
//            } catch (Exception e) {
//                Log.e(TAG, "Lỗi đọc dữ liệu tour", e);
//                Toast.makeText(this, "Lỗi đọc dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        }).addOnFailureListener(e -> {
//            Log.e(TAG, "Lỗi tải dữ liệu tour", e);
//            Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
//            finish();
//        });
//    }
//
//    // Load tên hướng dẫn viên
//    private void loadGuideName(String guideId) {
//        db.collection("guides").document(guideId)
//                .get()
//                .addOnSuccessListener(doc -> {
//                    if (doc.exists()) {
//                        String guideName = doc.getString("name");
//                        if (guideName != null && !guideName.isEmpty()) {
//                            etGuideId.setHint("Hướng dẫn viên: " + guideName);
//                        }
//                    }
//                })
//                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tải tên hướng dẫn viên", e));
//    }
//
//    // Hàm an toàn lấy String
//    private String getStringSafe(String value) {
//        return value != null ? value : "";
//    }
//
//    // Lưu thay đổi
//    private void saveChanges() {
//        String tourName = etTourName.getText().toString().trim();
//        String description = etDescription.getText().toString().trim();
//        String location = etLocation.getText().toString().trim();
//        String seatsStr = etSeats.getText().toString().trim();
//        String priceStr = etPrice.getText().toString().trim();
//        String depositStr = etDepositPercent.getText().toString().trim();
//        String startDate = etStartDate.getText().toString().trim();
//        String endDate = etEndDate.getText().toString().trim();
//        String guideId = etGuideId.getText().toString().trim();
//
//        if (TextUtils.isEmpty(tourName) || TextUtils.isEmpty(description) || TextUtils.isEmpty(location) ||
//                TextUtils.isEmpty(seatsStr) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(depositStr) ||
//                TextUtils.isEmpty(startDate) || TextUtils.isEmpty(endDate) || TextUtils.isEmpty(guideId)) {
//            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        try {
//            int seats = Integer.parseInt(seatsStr);
//            long price = Long.parseLong(priceStr);
//            int depositPercent = Integer.parseInt(depositStr);
//
//            if (depositPercent < 0 || depositPercent > 100) {
//                Toast.makeText(this, "Phần trăm đặt cọc phải từ 0-100!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            Map<String, Object> updates = new HashMap<>();
//            updates.put("tourName", tourName);
//            updates.put("description", description);
//            updates.put("location", location);
//            updates.put("availableSeats", seats);
//            updates.put("price", price);
//            updates.put("depositPercent", depositPercent);
//            updates.put("startDate", startDate);
//            updates.put("endDate", endDate);
//            updates.put("guideIds", Collections.singletonList(guideId));
//
//            db.collection("tours").document(tourId)
//                    .update(updates)
//                    .addOnSuccessListener(aVoid -> {
//                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
//                        finish();
//                    })
//                    .addOnFailureListener(e ->
//                            Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show());
//
//        } catch (NumberFormatException e) {
//            Toast.makeText(this, "Dữ liệu số không hợp lệ!", Toast.LENGTH_SHORT).show();
//        }
//    }
//}

package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditTourActivity extends AppCompatActivity {

    private static final String TAG = "EditTourActivity";

    private EditText etTourName, etDescription, etLocation, etSeats, etPrice, etDepositPercent, etStartDate, etEndDate, etGuideId;
    private TextView tvGuideName;
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
        tvGuideName = findViewById(R.id.tvGuideName);
        btnSave = findViewById(R.id.btnSave);

        loadTourData();

        // Chọn ngày
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        // Tự động load tên guide khi nhập ID
        etGuideId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String guideId = s.toString().trim();
                if (!guideId.isEmpty() && guideId.length() > 10) {
                    loadGuideName(guideId);
                } else {
                    tvGuideName.setText("Nhập ID hướng dẫn viên để xem tên");
                }
            }
        });

        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        // Parse ngày hiện tại từ EditText nếu có
        String currentDate = target.getText().toString();
        if (!currentDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = sdf.parse(currentDate);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date", e);
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    // Lưu dạng dd/MM/yyyy để dễ hiển thị
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                    target.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void loadTourData() {
        Log.d(TAG, "Loading tour data for: " + tourId);

        DocumentReference docRef = db.collection("tours").document(tourId);
        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Log.d(TAG, "Document exists, data: " + doc.getData());

                try {
                    // Load tourName
                    String tourName = doc.getString("tourName");
                    etTourName.setText(tourName != null ? tourName : "");

                    // Load description
                    String description = doc.getString("description");
                    etDescription.setText(description != null ? description : "");

                    // Load location
                    String location = doc.getString("location");
                    etLocation.setText(location != null ? location : "");

                    // Load availableSeats
                    Object seatsObj = doc.get("availableSeats");
                    if (seatsObj != null) {
                        etSeats.setText(String.valueOf(seatsObj));
                    }

                    // Load price
                    Object priceObj = doc.get("price");
                    if (priceObj != null) {
                        etPrice.setText(String.valueOf(priceObj));
                    }

                    // Load depositPercent
                    Object depositPercentObj = doc.get("depositPercent");
                    if (depositPercentObj != null) {
                        etDepositPercent.setText(String.valueOf(depositPercentObj));
                    }

                    // 🔥 Load startDate - xử lý Timestamp
                    Object startDateObj = doc.get("startDate");
                    etStartDate.setText(formatDate(startDateObj));

                    // 🔥 Load endDate - xử lý Timestamp
                    Object endDateObj = doc.get("endDate");
                    etEndDate.setText(formatDate(endDateObj));

                    // 🔥 Load guideIds
                    Object guideIdsObj = doc.get("guideIds");
                    if (guideIdsObj instanceof List) {
                        List<?> guideIdsList = (List<?>) guideIdsObj;
                        if (!guideIdsList.isEmpty() && guideIdsList.get(0) != null) {
                            String guideId = guideIdsList.get(0).toString();
                            etGuideId.setText(guideId);
                            loadGuideName(guideId);
                        }
                    }

                    Log.d(TAG, "Data loaded successfully");

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing document data", e);
                    Toast.makeText(this, "Lỗi đọc dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(this, "Tour không tồn tại!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load tour data", e);
            Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        });
    }

    // 🔥 Format date từ Timestamp hoặc String
    private String formatDate(Object dateObj) {
        if (dateObj == null) return "";

        try {
            if (dateObj instanceof Timestamp) {
                // Nếu là Timestamp, convert sang dd/MM/yyyy
                Timestamp timestamp = (Timestamp) dateObj;
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return sdf.format(date);
            } else if (dateObj instanceof String) {
                // Nếu đã là String, parse và format lại
                String dateStr = (String) dateObj;
                // Thử parse nhiều format
                String[] formats = {
                        "dd/MM/yyyy",
                        "d MMMM yyyy 'at' HH:mm:ss 'UTC'Z",
                        "dd MMMM yyyy 'at' HH:mm:ss 'UTC'Z"
                };

                for (String format : formats) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                        Date date = sdf.parse(dateStr);
                        if (date != null) {
                            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            return outputFormat.format(date);
                        }
                    } catch (Exception e) {
                        // Thử format tiếp theo
                    }
                }
                return dateStr; // Trả về chuỗi gốc nếu không parse được
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
        }

        return dateObj.toString();
    }

    // 🔥 Load tên hướng dẫn viên
    private void loadGuideName(String guideId) {
        if (guideId == null || guideId.trim().isEmpty()) {
            tvGuideName.setText("Chưa có hướng dẫn viên");
            return;
        }

        tvGuideName.setText("⏳ Đang tải...");

        db.collection("guides").document(guideId.trim())
                .get()
                .addOnSuccessListener(guideDoc -> {
                    if (guideDoc.exists()) {
                        String guideName = guideDoc.getString("name");
                        if (guideName != null && !guideName.isEmpty()) {
                            tvGuideName.setText("👤 " + guideName);
                            Log.d(TAG, "Guide name loaded: " + guideName);
                        } else {
                            tvGuideName.setText("⚠️ Hướng dẫn viên không có tên");
                        }
                    } else {
                        tvGuideName.setText("❌ ID không tồn tại trong hệ thống");
                        Log.w(TAG, "Guide document not found: " + guideId);
                    }
                })
                .addOnFailureListener(e -> {
                    tvGuideName.setText("❌ Lỗi tải thông tin: " + e.getMessage());
                    Log.e(TAG, "Failed to load guide name", e);
                });
    }

    private void saveChanges() {
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

            // 🔥 Convert date string thành Timestamp
            Timestamp startTimestamp = convertToTimestamp(startDate);
            Timestamp endTimestamp = convertToTimestamp(endDate);

            Map<String, Object> updates = new HashMap<>();
            updates.put("tourName", tourName);
            updates.put("description", description);
            updates.put("location", location);
            updates.put("availableSeats", seats);
            updates.put("price", price);
            updates.put("depositPercent", depositPercent);
            updates.put("startDate", startTimestamp);
            updates.put("endDate", endTimestamp);
            updates.put("guideIds", java.util.Collections.singletonList(guideId));

            Log.d(TAG, "Updating tour with data: " + updates);

            db.collection("tours").document(tourId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Tour updated successfully");
                        Toast.makeText(this, "✅ Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update tour", e);
                        Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Dữ liệu số không hợp lệ!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 🔥 Convert string date (dd/MM/yyyy) thành Timestamp
    private Timestamp convertToTimestamp(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date != null) {
                return new Timestamp(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting date to timestamp", e);
        }
        // Fallback: trả về timestamp hiện tại
        return Timestamp.now();
    }
}