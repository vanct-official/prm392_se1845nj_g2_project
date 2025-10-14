package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditPromotionActivity extends AppCompatActivity {

    private EditText etPromotionCode, etDescription, etDiscountPercent, etMinValue, etFromDate, etToDate;
    private Switch switchActive;
    private Button btnSave, btnCancel;
    private FirebaseFirestore db;
    private String docId;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_promotion);

        docId = getIntent().getStringExtra("promotionId");
        android.util.Log.d("PROMO_DEBUG", "Nhận promotionId = " + docId);

        if (docId == null || docId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID khuyến mãi!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Ánh xạ view
        etPromotionCode = findViewById(R.id.etPromotionCode);
        etDescription = findViewById(R.id.etDescription);
        etDiscountPercent = findViewById(R.id.etDiscountPercent);
        etMinValue = findViewById(R.id.etMinValue);
        etFromDate = findViewById(R.id.etFromDate);
        etToDate = findViewById(R.id.etToDate);
        switchActive = findViewById(R.id.switchActive);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        db = FirebaseFirestore.getInstance();
        docId = getIntent().getStringExtra("promotionId");

        // Kiểm tra ID
        if (docId == null || docId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID khuyến mãi!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Sự kiện chọn ngày
        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        // Nút Hủy
        btnCancel.setOnClickListener(v -> finish());

        // Tải dữ liệu Firestore
        loadPromotion();

        // Nút Lưu thay đổi
        btnSave.setOnClickListener(v -> saveChanges());
    }

    // ============================================================
    // 🔥 LOAD DỮ LIỆU FIRESTORE
    // ============================================================
    private void loadPromotion() {
        db.collection("promotions").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) bindData(doc);
                    else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindData(DocumentSnapshot doc) {
        etPromotionCode.setText(doc.getString("name"));
        etDescription.setText(doc.getString("description") != null ? doc.getString("description") : "");
        etDiscountPercent.setText(doc.getLong("discountPercent") != null
                ? String.valueOf(doc.getLong("discountPercent"))
                : "");
        etMinValue.setText(doc.getDouble("minimumValue") != null
                ? String.valueOf(doc.getDouble("minimumValue"))
                : "");
        switchActive.setChecked(Boolean.TRUE.equals(doc.getBoolean("isActive")));

        Timestamp from = doc.getTimestamp("validFrom");
        Timestamp to = doc.getTimestamp("validTo");

        if (from != null) etFromDate.setText(sdf.format(from.toDate()));
        if (to != null) etToDate.setText(sdf.format(to.toDate()));
    }

    // ============================================================
    // 💾 LƯU THAY ĐỔI
    // ============================================================
    private void saveChanges() {
        String desc = etDescription.getText().toString().trim();
        String discountStr = etDiscountPercent.getText().toString().trim();
        String minValueStr = etMinValue.getText().toString().trim();
        String fromStr = etFromDate.getText().toString().trim();
        String toStr = etToDate.getText().toString().trim();
        boolean isActive = switchActive.isChecked();

        if (desc.isEmpty() || discountStr.isEmpty() || minValueStr.isEmpty() || fromStr.isEmpty() || toStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        int discount;
        double minValue;

        try {
            discount = Integer.parseInt(discountStr);
            minValue = Double.parseDouble(minValueStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá trị nhập không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (discount <= 0 || discount >= 100) {
            Toast.makeText(this, "Phần trăm giảm phải từ 1 đến 99!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (minValue <= 0) {
            Toast.makeText(this, "Giá trị tối thiểu phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date fromDate = sdf.parse(fromStr);
            Date toDate = sdf.parse(toStr);

            if (fromDate.after(toDate)) {
                Toast.makeText(this, "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> update = new HashMap<>();
            update.put("description", desc);
            update.put("discountPercent", discount);
            update.put("minimumValue", minValue);
            update.put("isActive", isActive);
            update.put("validFrom", new Timestamp(fromDate));
            update.put("validTo", new Timestamp(toDate));

            db.collection("promotions").document(docId)
                    .update(update)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (ParseException e) {
            Toast.makeText(this, "Định dạng ngày không hợp lệ!", Toast.LENGTH_SHORT).show();
        }
    }

    // ============================================================
    // 📅 DATE PICKER
    // ============================================================
    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String dateStr = String.format(Locale.getDefault(),
                            "%02d/%02d/%d", dayOfMonth, month + 1, year);
                    target.setText(dateStr);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }
}
