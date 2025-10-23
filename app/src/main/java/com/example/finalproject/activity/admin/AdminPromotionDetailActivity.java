package com.example.finalproject.activity.admin;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminPromotionDetailActivity extends AppCompatActivity {
    private TextView tvPromotionCode, tvDescription, tvDiscount, tvMinValue, tvStatus;
    private MaterialButton btnBack;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_detail_admin);

        db = FirebaseFirestore.getInstance();

        tvPromotionCode = findViewById(R.id.tvPromotionCode);
        tvDescription = findViewById(R.id.tvDescription);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvMinValue = findViewById(R.id.tvMinValue);
        tvStatus = findViewById(R.id.tvStatus);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        String promotionId = getIntent().getStringExtra("promotionId");
        if (promotionId == null) {
            Toast.makeText(this, "Không tìm thấy ID khuyến mãi!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPromotionDetails(promotionId);
    }

    private void loadPromotionDetails(String promotionId) {
        db.collection("promotions").document(promotionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvPromotionCode.setText(doc.getString("name"));
                        tvDescription.setText(doc.getString("description"));
                        tvDiscount.setText(doc.getLong("discountPercent") + "%");
                        tvMinValue.setText(String.format(Locale.getDefault(), "%,.0f đ", doc.getDouble("minimumValue")));

                        Timestamp from = doc.getTimestamp("validFrom");
                        Timestamp to = doc.getTimestamp("validTo");
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                        boolean isActive = Boolean.TRUE.equals(doc.getBoolean("isActive"));
                        tvStatus.setText(isActive ? "Hoạt động" : "Tạm ngưng");
                        tvStatus.setTextColor(isActive ? getColor(R.color.purple_500) : getColor(android.R.color.holo_red_dark));
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu khuyến mãi!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
