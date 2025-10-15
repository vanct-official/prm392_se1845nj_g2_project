package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddPromotionActivity extends AppCompatActivity {

    private EditText etPromotionCode, etDescription, etDiscountPercent, etMinOrderValue, etStartDate, etEndDate;
    private SwitchMaterial switchActive;
    private MaterialButton btnCancel, btnCreate;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_promotion);

        // Ánh xạ view
        etPromotionCode = findViewById(R.id.etPromotionCode);
        etDescription = findViewById(R.id.etDescription);
        etDiscountPercent = findViewById(R.id.etDiscountPercent);
        etMinOrderValue = findViewById(R.id.etMinOrderValue);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        switchActive = findViewById(R.id.switchActive);
        btnCancel = findViewById(R.id.btnCancel);
        btnCreate = findViewById(R.id.btnCreate);

        db = FirebaseFirestore.getInstance();

        // 🗓Chọn ngày
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        // Hủy
        btnCancel.setOnClickListener(v -> finish());

        // Tạo khuyến mãi
        btnCreate.setOnClickListener(v -> validateAndCreate());
    }

    // ===========================================================
    // HIỂN THỊ DATE PICKER
    // ===========================================================
    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    target.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    // ===========================================================
    // KIỂM TRA VÀ TẠO KHUYẾN MÃI
    // ===========================================================
    private void validateAndCreate() {
        String code = etPromotionCode.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String discountStr = etDiscountPercent.getText().toString().trim();
        String minValueStr = etMinOrderValue.getText().toString().trim();
        String startStr = etStartDate.getText().toString().trim();
        String endStr = etEndDate.getText().toString().trim();
        boolean isActive = switchActive.isChecked();

        if (code.isEmpty() || desc.isEmpty() || discountStr.isEmpty() || minValueStr.isEmpty()
                || startStr.isEmpty() || endStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        int discount = Integer.parseInt(discountStr);
        double minValue = Double.parseDouble(minValueStr);

        if (discount <= 0 || discount >= 100) {
            Toast.makeText(this, "Phần trăm giảm giá phải từ 1 đến 99!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (minValue <= 0) {
            Toast.makeText(this, "Giá trị đơn hàng tối thiểu phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date startDate, endDate;
        try {
            startDate = sdf.parse(startStr);
            endDate = sdf.parse(endStr);

            if (startDate != null && endDate != null && !startDate.before(endDate)) {
                Toast.makeText(this, "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Định dạng ngày không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra trùng tên khuyến mãi
        db.collection("promotions").document(code)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Toast.makeText(this, "Tên mã khuyến mãi đã tồn tại!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Thêm khuyến mãi
                        Map<String, Object> promo = new HashMap<>();
                        promo.put("name", code);
                        promo.put("description", desc);
                        promo.put("discountPercent", discount);
                        promo.put("minimumValue", minValue);
                        promo.put("isActive", isActive);
                        promo.put("validFrom", new Timestamp(startDate));
                        promo.put("validTo", new Timestamp(endDate));

                        db.collection("promotions").document(code)
                                .set(promo)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Thêm khuyến mãi thành công!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish(); // Quay lại danh sách
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Lỗi khi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi kiểm tra: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
