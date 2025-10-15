package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditTourActivity extends AppCompatActivity {

    private static final String TAG = "EditTourActivity";

    private EditText etTourName, etDescription, etLocation, etSeats, etPrice, etDepositPercent, etStartDate, etEndDate, etGuideIds;
    private TextView tvGuideNames;
    private ImageSlider imageSlider;
    private Button btnSave, btnBack;
    private FirebaseFirestore db;
    private String tourId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tour);

        db = FirebaseFirestore.getInstance();
        tourId = getIntent().getStringExtra("tourId");

        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy tour ID!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ánh xạ view
        imageSlider = findViewById(R.id.imageSlider);
        etTourName = findViewById(R.id.etTourName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etSeats = findViewById(R.id.etSeats);
        etPrice = findViewById(R.id.etPrice);
        etDepositPercent = findViewById(R.id.etDepositPercent);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etGuideIds = findViewById(R.id.etGuideIds);
        tvGuideNames = findViewById(R.id.tvGuideNames);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnSave.setOnClickListener(v -> saveChanges());

        loadTourData();
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                    target.setText(date);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private void loadTourData() {
        db.collection("tours").document(tourId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Tour không tồn tại!", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    etTourName.setText(doc.getString("tourName"));
                    etDescription.setText(doc.getString("description"));
                    etLocation.setText(doc.getString("location"));
                    etSeats.setText(String.valueOf(doc.getLong("availableSeats")));
                    etPrice.setText(String.valueOf(doc.getLong("price")));
                    etDepositPercent.setText(String.valueOf(doc.getLong("depositPercent")));
                    etStartDate.setText(formatDate(doc.get("startDate")));
                    etEndDate.setText(formatDate(doc.get("endDate")));

                    // --- Load nhiều ảnh ---
                    List<String> imageUrls = (List<String>) doc.get("imageUrls");
                    ArrayList<SlideModel> slideModels = new ArrayList<>();

                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        for (String url : imageUrls) {
                            slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
                        }
                    } else {
                        slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
                    }
                    imageSlider.setImageList(slideModels);

                    // --- Load nhiều hướng dẫn viên ---
                    List<String> guideIds = (List<String>) doc.get("guideIds");
                    if (guideIds != null && !guideIds.isEmpty()) {
                        etGuideIds.setText(String.join(",", guideIds));
                        loadGuidesNames(guideIds);
                    } else {
                        tvGuideNames.setText("(Chưa có hướng dẫn viên)");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadGuidesNames(List<String> guideIds) {
        StringBuilder allNames = new StringBuilder();
        for (String id : guideIds) {
            db.collection("guides").document(id)
                    .get()
                    .addOnSuccessListener(guideDoc -> {
                        if (guideDoc.exists()) {
                            String guideName = guideDoc.getString("name");
                            allNames.append("• ").append(guideName).append("\n");
                            tvGuideNames.setText(allNames.toString().trim());
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Lỗi tải hướng dẫn viên: " + id, e));
        }
    }

    private void saveChanges() {
        try {
            String tourName = etTourName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String guideIdsRaw = etGuideIds.getText().toString().trim();

            if (tourName.isEmpty() || description.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> guideIds = new ArrayList<>();
            if (!guideIdsRaw.isEmpty()) {
                String[] split = guideIdsRaw.split(",");
                for (String s : split) guideIds.add(s.trim());
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("tourName", tourName);
            updates.put("description", description);
            updates.put("location", location);
            updates.put("availableSeats", Integer.parseInt(etSeats.getText().toString()));
            updates.put("price", Long.parseLong(etPrice.getText().toString()));
            updates.put("depositPercent", Integer.parseInt(etDepositPercent.getText().toString()));
            updates.put("startDate", convertToTimestamp(etStartDate.getText().toString()));
            updates.put("endDate", convertToTimestamp(etEndDate.getText().toString()));
            updates.put("guideIds", guideIds);

            db.collection("tours").document(tourId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Timestamp convertToTimestamp(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            return new Timestamp(date);
        } catch (Exception e) {
            return Timestamp.now();
        }
    }

    private String formatDate(Object obj) {
        if (obj instanceof Timestamp) {
            Date date = ((Timestamp) obj).toDate();
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
        }
        return "";
    }
}
