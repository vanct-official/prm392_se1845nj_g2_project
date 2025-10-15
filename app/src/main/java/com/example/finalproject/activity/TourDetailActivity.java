package com.example.finalproject.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TourDetailActivity extends AppCompatActivity {

    private ImageSlider imageSlider;
    private TextView tvTourName, tvDescription, tvLocation, tvPrice, tvGuideName;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_detail);

        imageSlider = findViewById(R.id.imageSlider);
        tvTourName = findViewById(R.id.tvTourName);
        tvDescription = findViewById(R.id.tvDescription);
        tvLocation = findViewById(R.id.tvLocation);
        tvPrice = findViewById(R.id.tvPrice);
        tvGuideName = findViewById(R.id.tvGuideName);

        db = FirebaseFirestore.getInstance();

        String tourId = getIntent().getStringExtra("tourId");
        if (tourId != null) {
            loadTourDetail(tourId);
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin tour", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Nút quay lại
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadTourDetail(String tourId) {
        db.collection("tours").document(tourId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        bindTourData(doc);
                    } else {
                        Toast.makeText(this, "Không tìm thấy tour!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bindTourData(DocumentSnapshot doc) {
        String name = doc.getString("tourName");
        String desc = doc.getString("description");
        String location = doc.getString("location");
        Double price = doc.getDouble("price");
        String guideId = doc.getString("guideId");
        List<String> images = (List<String>) doc.get("images");

        tvTourName.setText(name != null ? name : "Không rõ tên");
        tvDescription.setText(desc != null ? desc : "");
        tvLocation.setText(location != null ? "Địa điểm: " + location : "Địa điểm: Chưa xác định");
        tvPrice.setText(price != null
                ? "Giá: " + NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price)
                : "");

        // Ảnh
        List<SlideModel> slideModels = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (String url : images) {
                slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
            }
        } else {
            slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
        }
        imageSlider.setImageList(slideModels, ScaleTypes.CENTER_CROP);

        // Lấy tên hướng dẫn viên
        if (guideId != null && !guideId.isEmpty()) {
            db.collection("guides").document(guideId).get()
                    .addOnSuccessListener(guideDoc -> {
                        String guideName = guideDoc.getString("name");
                        tvGuideName.setText("Hướng dẫn viên: " + (guideName != null ? guideName : "Không rõ"));
                    })
                    .addOnFailureListener(e ->
                            tvGuideName.setText("Hướng dẫn viên: Lỗi tải dữ liệu"));
        } else {
            tvGuideName.setText("Hướng dẫn viên: Không có");
        }
    }


}
