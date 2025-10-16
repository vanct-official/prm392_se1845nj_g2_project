package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.utils.ObjectUtils;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.example.finalproject.utils.CloudinaryManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditTourActivity extends AppCompatActivity {

    private static final String TAG = "EditTourActivity";

    private EditText etTourName, etDescription, etLocation, etSeats, etPrice,
            etDepositPercent, etStartDate, etEndDate;
    private TextView tvGuideName;
    private Spinner spinnerGuide;
    private ImageSlider imageSlider;
    private Button btnSave, btnBack, btnChooseImages;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String tourId;

    private List<String> imageUrls = new ArrayList<>();
    private List<Uri> newImageUris = new ArrayList<>();
    private List<String> guideIds = new ArrayList<>();
    private List<String> guideNames = new ArrayList<>();
    private String selectedGuideId;

    private static final int PICK_IMAGES_REQUEST = 200;

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

        mapViews();
        setupListeners();
        loadGuides(); // tải danh sách hướng dẫn viên
        loadTourData(); // tải tour
    }

    private void mapViews() {
        imageSlider = findViewById(R.id.imageSlider);
        etTourName = findViewById(R.id.etTourName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etSeats = findViewById(R.id.etSeats);
        etPrice = findViewById(R.id.etPrice);
        etDepositPercent = findViewById(R.id.etDepositPercent);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        spinnerGuide = findViewById(R.id.spinnerGuide);
        tvGuideName = findViewById(R.id.tvGuideNames);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        btnChooseImages.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
                    target.setText(date);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh mới"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            newImageUris.clear();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    newImageUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                newImageUris.add(data.getData());
            }

            Toast.makeText(this, "Đã chọn " + newImageUris.size() + " ảnh mới", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadGuides() {
        db.collection("guides")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    guideIds.clear();
                    guideNames.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        guideIds.add(doc.getId());
                        guideNames.add(doc.getString("name"));
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_dropdown_item, guideNames);
                    spinnerGuide.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải hướng dẫn viên: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadTourData() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        db.collection("tours").document(tourId)
                .get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    if (!doc.exists()) {
                        Toast.makeText(this, "Tour không tồn tại!", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    etTourName.setText(doc.getString("tourName"));
                    etDescription.setText(doc.getString("description"));
                    etLocation.setText(doc.getString("location"));
                    etSeats.setText(String.valueOf(doc.getLong("availableSeats")));
                    etPrice.setText(String.valueOf(doc.getDouble("price")));
                    etDepositPercent.setText(String.valueOf(doc.getLong("depositPercent")));
                    etStartDate.setText(formatDate(doc.get("startDate")));
                    etEndDate.setText(formatDate(doc.get("endDate")));

                    // Load ảnh Cloudinary
                    imageUrls = (List<String>) doc.get("images");
                    List<SlideModel> slides = new ArrayList<>();
                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        for (String url : imageUrls) slides.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
                    } else {
                        slides.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
                    }
                    imageSlider.setImageList(slides);

                    // Load hướng dẫn viên
                    List<String> gids = (List<String>) doc.get("guideIds");
                    if (gids != null && !gids.isEmpty()) {
                        selectedGuideId = gids.get(0);
                        tvGuideName.setText("Hướng dẫn viên: " + gids.get(0));
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Lỗi tải tour: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveChanges() {
        try {
            progressBar.setVisibility(android.view.View.VISIBLE);
            String tourName = etTourName.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String loc = etLocation.getText().toString().trim();

            selectedGuideId = guideIds.get(spinnerGuide.getSelectedItemPosition());

            if (newImageUris.isEmpty()) {
                updateFirestore(tourName, desc, loc, imageUrls);
            } else {
                // Upload lại ảnh mới lên Cloudinary
                new Thread(() -> {
                    try {
                        List<String> uploadedUrls = new ArrayList<>();
                        for (Uri uri : newImageUris) {
                            InputStream is = getContentResolver().openInputStream(uri);
                            Map uploadResult = CloudinaryManager.getInstance()
                                    .uploader()
                                    .upload(is, ObjectUtils.emptyMap());
                            uploadedUrls.add((String) uploadResult.get("secure_url"));
                        }
                        runOnUiThread(() -> updateFirestore(tourName, desc, loc, uploadedUrls));
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(android.view.View.GONE);
                            Toast.makeText(this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }

        } catch (Exception e) {
            progressBar.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFirestore(String tourName, String desc, String loc, List<String> urls) {
        Map<String, Object> data = new HashMap<>();
        data.put("tourName", tourName);
        data.put("description", desc);
        data.put("location", loc);
        data.put("availableSeats", Integer.parseInt(etSeats.getText().toString()));
        data.put("price", Double.parseDouble(etPrice.getText().toString()));
        data.put("depositPercent", Integer.parseInt(etDepositPercent.getText().toString()));
        data.put("startDate", convertToTimestamp(etStartDate.getText().toString()));
        data.put("endDate", convertToTimestamp(etEndDate.getText().toString()));
        data.put("guideIds", List.of(selectedGuideId));
        data.put("images", urls);
        data.put("updateAt", new Timestamp(new Date()));

        db.collection("tours").document(tourId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
