package com.example.finalproject.activity;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Robust TourDetailActivity:
 * - tương thích với nhiều tên field (tourName/name, images/imageUrl/imageUrls, guideIds/guides)
 * - format Timestamp -> dd/MM/yyyy
 * - load guide names from collection "guides" (nếu có)
 * - hiển thị slide ảnh từ field images (List<String>)
 */
public class TourDetailActivity extends AppCompatActivity {

    private static final String TAG = "TourDetailActivity";

    private TextView tvTourName, tvDescription, tvPrice, tvStartDate, tvEndDate,
            tvLocation, tvGuideName, tvSeats, tvDepositPercent;
    private ImageSlider imageSlider;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_detail);

        // find views (IDs must match your layout)
        tvTourName = findViewById(R.id.tvTourName);
        tvDescription = findViewById(R.id.tvDescription);
        tvPrice = findViewById(R.id.tvPrice);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvLocation = findViewById(R.id.tvLocation);
        tvGuideName = findViewById(R.id.tvGuideName);
        tvSeats = findViewById(R.id.tvSeats);
        tvDepositPercent = findViewById(R.id.tvDepositPercent);
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
                    if (doc != null && doc.exists()) {
                        bindTourData(doc);
                    } else {
                        Toast.makeText(this, "Không tìm thấy tour!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải tour", e);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void bindTourData(DocumentSnapshot doc) {
        // --- Tour name: try multiple keys ---
        String tourName = firstNonNullString(doc, "tourName", "name");
        tvTourName.setText(tourName != null ? tourName : "Không rõ tên");

        // --- Description ---
        String desc = firstNonNullString(doc, "description", "desc");
        tvDescription.setText(desc != null ? desc : "");

        // --- Price (Number) ---
        Double price = getNumberAsDouble(doc, "price", "cost");
        if (price != null) {
            tvPrice.setText("Giá: " + NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price));
        } else {
            tvPrice.setText("Giá: Không có");
        }

        // --- Location ---
        String location = firstNonNullString(doc, "location", "place");
        tvLocation.setText(location != null ? location : "Chưa xác định");

        // --- Seats ---
        Long seats = doc.getLong("availableSeats");
        if (seats == null) {
            // try different key
            Object sObj = doc.get("seats");
            if (sObj instanceof Number) seats = ((Number) sObj).longValue();
        }
        tvSeats.setText("" + (seats != null ? seats : 0) + "ghế");

        // --- Deposit percent ---
        Long depositPercent = doc.getLong("depositPercent");
        if (depositPercent == null) {
            Object dp = doc.get("deposit");
            if (dp instanceof Number) depositPercent = ((Number) dp).longValue();
        }
        tvDepositPercent.setText((depositPercent != null ? depositPercent + "%" : "--%"));

        // --- Status (try a few keys) ---
        String status = firstNonNullString(doc, "status", "state", "tourStatus");
        // --- Dates: startDate / endDate (handle Timestamp / Date / String) ---
        Object startObj = doc.get("startDate");
        Object endObj = doc.get("endDate");
        tvStartDate.setText(safeFormatDate(startObj));
        tvEndDate.setText(safeFormatDate(endObj));

        // --- Images: prefer list field 'images', fallback 'imageUrl' or 'image' ---
        List<String> imageList = null;
        Object imagesObj = doc.get("images");            // preferred
        if (imagesObj instanceof List) {
            // unchecked cast is OK here (we check values later)
            imageList = (List<String>) imagesObj;
        } else {
            // try alternative keys
            String single = firstNonNullString(doc, "imageUrl", "image", "imageUrls");
            if (single != null) {
                imageList = new ArrayList<>();
                imageList.add(single);
            }
        }
        // build slide models
        List<SlideModel> slideModels = new ArrayList<>();
        if (imageList != null && !imageList.isEmpty()) {
            for (String u : imageList) {
                if (u != null && !u.trim().isEmpty()) slideModels.add(new SlideModel(u, ScaleTypes.CENTER_CROP));
            }
        }
        if (slideModels.isEmpty()) {
            // fallback placeholder (use remote placeholder to avoid missing drawable)
            slideModels.add(new SlideModel("https://via.placeholder.com/800x450.png?text=No+Image", ScaleTypes.CENTER_CROP));
        }
        imageSlider.setImageList(slideModels, ScaleTypes.CENTER_CROP);

        // --- Guide: field might be 'guideIds' (IDs), or 'guides' (IDs), or even a single 'guideId' ---
        List<String> guideIds = null;
        Object gids = doc.get("guideIds");
        if (gids instanceof List) guideIds = (List<String>) gids;
        else {
            Object other = doc.get("guides");
            if (other instanceof List) guideIds = (List<String>) other;
            else {
                // try single id
                String singleGuide = firstNonNullString(doc, "guideId", "guide");
                if (singleGuide != null) {
                    guideIds = new ArrayList<>();
                    guideIds.add(singleGuide);
                }
            }
        }

        if (guideIds != null && !guideIds.isEmpty()) {
            tvGuideName.setText("Đang tải hướng dẫn viên...");
            fetchAndShowGuideNames(guideIds);
        } else {
            tvGuideName.setText("Chưa có hướng dẫn viên");
        }
    }

    // fetch each guide doc and show their names (concatenate)
    private void fetchAndShowGuideNames(List<String> guideIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        StringBuilder sb = new StringBuilder();
        final int total = guideIds.size();
        final int[] done = {0};

        for (String gid : guideIds) {
            if (gid == null || gid.trim().isEmpty()) {
                done[0]++;
                continue;
            }
            db.collection("guides").document(gid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String gname = (doc != null && doc.exists()) ? doc.getString("name") : null;
                        if (gname == null) gname = gid; // fallback show id
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(gname);
                        done[0]++;
                        if (done[0] >= total) {
                            tvGuideName.setText("Hướng dẫn viên: " + sb.toString());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(gid);
                        done[0]++;
                        if (done[0] >= total) {
                            tvGuideName.setText("Hướng dẫn viên: " + sb.toString());
                        }
                    });
        }
    }

    // helpers ----------------------------------------------------------------

    private String firstNonNullString(DocumentSnapshot doc, String... keys) {
        for (String k : keys) {
            try {
                String v = doc.getString(k);
                if (v != null && !v.isEmpty()) return v;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Double getNumberAsDouble(DocumentSnapshot doc, String... keys) {
        for (String k : keys) {
            try {
                Object o = doc.get(k);
                if (o instanceof Number) return ((Number) o).doubleValue();
                if (o instanceof String) {
                    try { return Double.parseDouble((String)o); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String safeFormatDate(Object obj) {
        if (obj == null) return "Không có";
        try {
            if (obj instanceof Timestamp) {
                Date d = ((Timestamp) obj).toDate();
                return sdf.format(d);
            } else if (obj instanceof Date) {
                return sdf.format((Date) obj);
            } else if (obj instanceof String) {
                String s = (String) obj;
                // if already dd/MM/yyyy, return it
                if (s.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) return s;
                // try parse ISO-ish
                try {
                    // many Firestore string formats won't parse — fallback to showing original string
                    SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    Date d = iso.parse(s);
                    if (d != null) return sdf.format(d);
                } catch (Exception ignored) {}
                return s;
            } else {
                return obj.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "safeFormatDate error", e);
            return "Không có";
        }
    }
}
