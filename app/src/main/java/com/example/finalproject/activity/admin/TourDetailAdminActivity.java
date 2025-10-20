package com.example.finalproject.activity.admin;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TourDetailAdminActivity extends AppCompatActivity {

    private static final String TAG = "TourDetailAdminActivity";

    private TextView tvTitle, tvDescription, tvDestination, tvDuration, tvPrice,
            tvStartDate, tvEndDate, tvItinerary, tvGuideName, tvStatus;
    private ImageSlider imageSlider;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_detail_admin);

        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvDestination = findViewById(R.id.tvDestination);
        tvDuration = findViewById(R.id.tvDuration);
        tvPrice = findViewById(R.id.tvPrice);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvItinerary = findViewById(R.id.tvItinerary);
        tvGuideName = findViewById(R.id.tvGuideName);
        tvStatus = findViewById(R.id.tvStatus);
        imageSlider = findViewById(R.id.imageSlider);

        String tourId = getIntent().getStringExtra("tourId");
        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin tour", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTourDetail(tourId);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadTourDetail(String tourId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tours").document(tourId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) bindTourData(doc);
                    else {
                        Toast.makeText(this, "Không tìm thấy tour!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void bindTourData(DocumentSnapshot doc) {
        tvTitle.setText(doc.getString("title"));
        tvDescription.setText(doc.getString("description"));
        tvDestination.setText(doc.getString("destination"));
        tvDuration.setText(doc.getString("duration"));
        tvItinerary.setText(doc.getString("itinerary"));

        // Trạng thái
        String status = doc.getString("status");
        if (status != null) {
            switch (status) {
                case "completed": tvStatus.setText("✅ Hoàn thành"); break;
                case "in_progress": tvStatus.setText("🚩 Đang diễn ra"); break;
                case "upcoming": tvStatus.setText("🕓 Chưa bắt đầu"); break;
                case "cancelled": tvStatus.setText("❌ Hủy"); break;
                default: tvStatus.setText("Không xác định"); break;
            }
        } else tvStatus.setText("Không xác định");

        // Giá
        Double price = doc.getDouble("price");
        if (price != null)
            tvPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price));
        else tvPrice.setText("0 ₫");

        tvStartDate.setText(safeFormatDate(doc.get("start_date")));
        tvEndDate.setText(safeFormatDate(doc.get("end_date")));

        // Ảnh
        List<String> imgs = (List<String>) doc.get("images");
        List<SlideModel> slides = new ArrayList<>();
        if (imgs != null && !imgs.isEmpty()) {
            for (String url : imgs) slides.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
        } else {
            slides.add(new SlideModel("https://via.placeholder.com/800x450.png?text=No+Image", ScaleTypes.CENTER_CROP));
        }
        imageSlider.setImageList(slides, ScaleTypes.CENTER_CROP);

        // Hướng dẫn viên
        List<String> guideIds = (List<String>) doc.get("guideIds");
        fetchGuides(guideIds);
    }

    private void fetchGuides(List<String> guideIds) {
        if (guideIds == null || guideIds.isEmpty()) {
            tvGuideName.setText("Chưa có hướng dẫn viên");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), guideIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        List<String> guideNames = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot) {
                            if ("guide".equals(doc.getString("role"))) {
                                String firstName = doc.getString("firstname");
                                String lastName = doc.getString("lastname");
                                String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                                guideNames.add(fullName.trim());
                            }
                        }

                        // Xóa trùng lặp nếu có
                        List<String> uniqueNames = new ArrayList<>(new LinkedHashSet<>(guideNames));

                        if (!uniqueNames.isEmpty()) {
                            tvGuideName.setText(String.join(", ", uniqueNames));
                        } else {
                            tvGuideName.setText("Không có hướng dẫn viên phù hợp");
                        }
                    } else {
                        tvGuideName.setText("Không có hướng dẫn viên");
                    }
                })
                .addOnFailureListener(e -> {
                    tvGuideName.setText("Lỗi tải hướng dẫn viên");
                    Log.e(TAG, "Error fetching guides", e);
                });
    }

    private String safeFormatDate(Object obj) {
        if (obj == null) return "--/--/----";
        try {
            if (obj instanceof Timestamp) {
                Date d = ((Timestamp) obj).toDate();
                return sdf.format(d);
            } else if (obj instanceof Date) {
                return sdf.format((Date) obj);
            } else if (obj instanceof String) {
                String s = (String) obj;
                if (s.contains(" at ")) return s.split(" at ")[0];
                return s;
            }
        } catch (Exception e) {
            Log.e(TAG, "Date parse error", e);
        }
        return "--/--/----";
    }
}
