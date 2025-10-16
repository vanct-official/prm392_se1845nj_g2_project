package com.example.finalproject.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.utils.ObjectUtils;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.example.finalproject.utils.CloudinaryManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
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

    private TextInputEditText etTitle, etDescription, etDestination, etDuration,
            etItinerary, etStartDate, etEndDate, etPrice;
    private TextView tvGuideNames;
    private MaterialButton btnChooseImages, btnSave, btnBack;
    private ImageSlider imageSlider;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String tourId;
    private List<String> imageUrls = new ArrayList<>();
    private List<Uri> newImageUris = new ArrayList<>();
    private List<String> guideIds = new ArrayList<>();
    private List<String> guideNames = new ArrayList<>();
    private List<String> selectedGuideIds = new ArrayList<>();
    private List<String> selectedGuideNames = new ArrayList<>();

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
        loadTourData();
    }

    private void mapViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDestination = findViewById(R.id.etDestination);
        etDuration = findViewById(R.id.etDuration);
        etItinerary = findViewById(R.id.etItinerary);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etPrice = findViewById(R.id.etPrice);
        tvGuideNames = findViewById(R.id.tvGuideNames);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        imageSlider = findViewById(R.id.imageSlider);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnChooseImages.setOnClickListener(v -> openGallery());
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            target.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            newImageUris.clear();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++)
                    newImageUris.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                newImageUris.add(data.getData());
            }
            Toast.makeText(this, "Đã chọn " + newImageUris.size() + " ảnh", Toast.LENGTH_SHORT).show();
        }
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

                    // --- Load dữ liệu
                    etTitle.setText(doc.getString("title"));
                    etDescription.setText(doc.getString("description"));
                    etDestination.setText(doc.getString("destination"));
                    etDuration.setText(doc.getString("duration"));
                    etItinerary.setText(doc.getString("itinerary"));

                    etPrice.setText(String.valueOf(doc.getDouble("price")));

                    etStartDate.setText(formatDate(doc.get("start_date")));
                    etEndDate.setText(formatDate(doc.get("end_date")));

                    // --- Ảnh
                    imageUrls = (List<String>) doc.get("images");
                    List<SlideModel> slides = new ArrayList<>();
                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        for (String url : imageUrls)
                            slides.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
                    } else {
                        slides.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
                    }
                    imageSlider.setImageList(slides);

                    // --- Hướng dẫn viên
                    List<String> gids = (List<String>) doc.get("guideIds");
                    if (gids != null) {
                        selectedGuideIds.clear();
                        selectedGuideIds.addAll(gids);
                    }
                    loadGuides();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Lỗi tải tour: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadGuides() {
        db.collection("guides").get().addOnSuccessListener(query -> {
            guideIds.clear();
            guideNames.clear();

            for (DocumentSnapshot d : query) {
                guideIds.add(d.getId());
                guideNames.add(d.getString("name"));
            }

            selectedGuideNames.clear();
            for (String id : selectedGuideIds) {
                int idx = guideIds.indexOf(id);
                if (idx >= 0) selectedGuideNames.add(guideNames.get(idx));
            }
            updateGuideText();
            tvGuideNames.setOnClickListener(v -> showGuideSelectDialog());
        });
    }

    private void showGuideSelectDialog() {
        boolean[] checked = new boolean[guideIds.size()];
        for (int i = 0; i < guideIds.size(); i++) {
            checked[i] = selectedGuideIds.contains(guideIds.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn hướng dẫn viên")
                .setMultiChoiceItems(guideNames.toArray(new String[0]), checked, (dialog, which, isChecked) -> {
                    String id = guideIds.get(which);
                    String name = guideNames.get(which);
                    if (isChecked) {
                        if (!selectedGuideIds.contains(id)) {
                            selectedGuideIds.add(id);
                            selectedGuideNames.add(name);
                        }
                    } else {
                        selectedGuideIds.remove(id);
                        selectedGuideNames.remove(name);
                    }
                })
                .setPositiveButton("Xong", (d, w) -> updateGuideText())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateGuideText() {
        if (selectedGuideNames.isEmpty())
            tvGuideNames.setText("(Chưa chọn)");
        else
            tvGuideNames.setText(String.join(", ", selectedGuideNames));
    }

    private void saveChanges() {
        try {
            progressBar.setVisibility(android.view.View.VISIBLE);

            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String dest = etDestination.getText().toString().trim();
            String duration = etDuration.getText().toString().trim();
            String itinerary = etItinerary.getText().toString().trim();
            double price = Double.parseDouble(etPrice.getText().toString().trim());

            if (title.isEmpty() || desc.isEmpty() || dest.isEmpty() || duration.isEmpty()
                    || itinerary.isEmpty() || price <= 0) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin hợp lệ!", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(android.view.View.GONE);
                return;
            }

            if (selectedGuideIds.isEmpty()) {
                Toast.makeText(this, "Chọn ít nhất 1 hướng dẫn viên!", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(android.view.View.GONE);
                return;
            }

            if (newImageUris.isEmpty()) {
                updateFirestore(title, desc, dest, duration, itinerary, price, imageUrls);
            } else {
                new Thread(() -> {
                    try {
                        List<String> uploadedUrls = new ArrayList<>();
                        for (Uri uri : newImageUris) {
                            InputStream is = getContentResolver().openInputStream(uri);
                            Map upload = CloudinaryManager.getInstance().uploader().upload(is, ObjectUtils.emptyMap());
                            uploadedUrls.add((String) upload.get("secure_url"));
                        }
                        runOnUiThread(() -> updateFirestore(title, desc, dest, duration, itinerary, price, uploadedUrls));
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

    private void updateFirestore(String title, String desc, String dest, String duration, String itinerary,
                                 double price, List<String> urls) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", desc);
        data.put("destination", dest);
        data.put("duration", duration);
        data.put("itinerary", itinerary);
        data.put("price", price);
        data.put("start_date", etStartDate.getText().toString());
        data.put("end_date", etEndDate.getText().toString());
        data.put("guideIds", selectedGuideIds);
        data.put("images", urls);
        data.put("updateAt", new Timestamp(new Date()));

        db.collection("tours").document(tourId).update(data)
                .addOnSuccessListener(v -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatDate(Object obj) {
        if (obj == null) return "";
        try {
            if (obj instanceof Timestamp) {
                Date d = ((Timestamp) obj).toDate();
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d);
            }
            if (obj instanceof String) return (String) obj;
        } catch (Exception ignored) {
        }
        return "";
    }
}
