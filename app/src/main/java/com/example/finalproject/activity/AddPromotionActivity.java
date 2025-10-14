package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
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

    private EditText etCode, etDescription, etDiscount, etMinValue, etFromDate, etToDate;
    private CheckBox cbActive;
    private Button btnCancel, btnCreate;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_promotion);

        etCode = findViewById(R.id.etCode);
        etDescription = findViewById(R.id.etDescription);
        etDiscount = findViewById(R.id.etDiscount);
        etMinValue = findViewById(R.id.etMinValue);
        etFromDate = findViewById(R.id.etFromDate);
        etToDate = findViewById(R.id.etToDate);
        cbActive = findViewById(R.id.cbActive);
        btnCancel = findViewById(R.id.btnCancel);
        btnCreate = findViewById(R.id.btnCreate);

        db = FirebaseFirestore.getInstance();

        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        btnCancel.setOnClickListener(v -> finish()); // Quay lại

        btnCreate.setOnClickListener(v -> addPromotion());
    }

    private void addPromotion() {
        String name = etCode.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String discountStr = etDiscount.getText().toString().trim();
        String minValueStr = etMinValue.getText().toString().trim();
        String fromStr = etFromDate.getText().toString().trim();
        String toStr = etToDate.getText().toString().trim();
        boolean isActive = cbActive.isChecked();

        // 🧩 Kiểm tra dữ liệu
        if (name.isEmpty() || desc.isEmpty() || discountStr.isEmpty() ||
                minValueStr.isEmpty() || fromStr.isEmpty() || toStr.isEmpty()) {
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

        if (discount < 1 || discount >= 100) {
            Toast.makeText(this, "Phần trăm giảm giá phải từ 1 đến dưới 100!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (minValue <= 0) {
            Toast.makeText(this, "Giá trị tối thiểu phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date fromDate = sdf.parse(fromStr);
            Date toDate = sdf.parse(toStr);
            if (fromDate.after(toDate)) {
                Toast.makeText(this, "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("promotions").document(name).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Toast.makeText(this, "Tên mã khuyến mãi đã tồn tại!", Toast.LENGTH_SHORT).show();
                        } else {
                            Map<String, Object> promotion = new HashMap<>();
                            promotion.put("name", name);
                            promotion.put("description", desc);
                            promotion.put("discountPercent", discount);
                            promotion.put("minimumValue", minValue);
                            promotion.put("isActive", isActive);
                            promotion.put("validFrom", new Timestamp(fromDate));
                            promotion.put("validTo", new Timestamp(toDate));

                            db.collection("promotions").document(name)
                                    .set(promotion)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Thêm khuyến mãi thành công!", Toast.LENGTH_SHORT).show();
                                        finish(); // Quay lại sau khi thêm
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Lỗi khi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
        } catch (ParseException e) {
            Toast.makeText(this, "Định dạng ngày không hợp lệ!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                    target.setText(dateStr);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)).show();
    }
}
