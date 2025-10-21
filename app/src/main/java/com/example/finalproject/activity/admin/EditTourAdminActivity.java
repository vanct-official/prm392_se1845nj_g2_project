package com.example.finalproject.activity.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.example.finalproject.utils.CloudinaryManager;
import com.example.finalproject.utils.RealPathUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.example.finalproject.utils.CloudinaryManager;
import java.io.File;
import java.io.InputStream;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class EditTourAdminActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etDestination, etDuration,
            etItinerary, etStartDate, etEndDate, etPrice;
    private TextView tvGuideNames;
    private MaterialButton btnChooseImages, btnSave, btnBack;
    private ImageSlider imageSlider;
    private ProgressBar progressBar;
    private Spinner spStatus;

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
        setContentView(R.layout.activity_edit_tour_admin);

        db = FirebaseFirestore.getInstance();
        tourId = getIntent().getStringExtra("tourId");
        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy tour ID!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mapViews();
        setupStatusSpinner();
        setupListeners();
        loadTourData();
        setupLocationSelector();
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
        // Format giá khi người dùng nhập
        etPrice.addTextChangedListener(new android.text.TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!s.toString().equals(current)) {
                    etPrice.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d]", "");
                    if (!cleanString.isEmpty()) {
                        double parsed = Double.parseDouble(cleanString);
                        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                        String formatted = formatter.format(parsed);
                        current = formatted;
                        etPrice.setText(formatted);
                        etPrice.setSelection(formatted.length());
                    }

                    etPrice.addTextChangedListener(this);
                }
            }
        });
        tvGuideNames = findViewById(R.id.tvGuideNames);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        imageSlider = findViewById(R.id.imageSlider);
        progressBar = findViewById(R.id.progressBar);
        spStatus = findViewById(R.id.spStatus);

        etTitle.setEnabled(false);
        etStartDate.setEnabled(false);
        etEndDate.setEnabled(false);
    }

    private void setupStatusSpinner() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Chưa diễn ra", "Đang diễn ra", "Hoàn thành", "Hủy"}
        );
        spStatus.setAdapter(statusAdapter);
        spStatus.setEnabled(false);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnChooseImages.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> {
            if (newImageUris.isEmpty()) {
                saveChangesToFirestore();
            } else {
                uploadImagesToCloudinary(this::saveChangesToFirestore);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
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
            Toast.makeText(this, "Đã chọn " + newImageUris.size() + " ảnh mới", Toast.LENGTH_SHORT).show();
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

                    etTitle.setText(doc.getString("title"));
                    etDescription.setText(doc.getString("description"));
                    etDestination.setText(doc.getString("destination"));
                    etDuration.setText(doc.getString("duration"));
                    etItinerary.setText(doc.getString("itinerary"));

                    Double price = doc.getDouble("price");
                    if (price != null) {
                        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
                        etPrice.setText(nf.format(price));
                    }

                    etStartDate.setText(formatDate(doc.get("start_date")));
                    etEndDate.setText(formatDate(doc.get("end_date")));

                    imageUrls = (List<String>) doc.get("images");
                    List<SlideModel> slides = new ArrayList<>();
                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        for (String url : imageUrls)
                            slides.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
                    } else {
                        slides.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
                    }
                    imageSlider.setImageList(slides);

                    updateStatusBasedOnDates();

                    List<String> gids = (List<String>) doc.get("guideIds");
                    selectedGuideIds.clear();
                    if (gids != null) selectedGuideIds.addAll(gids);

                    loadGuidesFromUsers();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Lỗi tải tour: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadGuidesFromUsers() {
        db.collection("users").whereEqualTo("role", "guide")
                .get()
                .addOnSuccessListener(query -> {
                    guideIds.clear();
                    guideNames.clear();
                    selectedGuideNames.clear();

                    for (DocumentSnapshot d : query) {
                        String id = d.getId();
                        String name = (d.getString("firstname") + " " + d.getString("lastname")).trim();
                        if (name.isEmpty()) name = id;

                        guideIds.add(id);
                        guideNames.add(name);
                        if (selectedGuideIds.contains(id))
                            selectedGuideNames.add(name);
                    }

                    tvGuideNames.setText(selectedGuideNames.isEmpty()
                            ? "(Chưa chọn hướng dẫn viên)"
                            : String.join(", ", selectedGuideNames));

                    tvGuideNames.setOnClickListener(v -> showGuideSelectDialog());
                });
    }

    private void showGuideSelectDialog() {
        if (guideNames.isEmpty()) {
            Toast.makeText(this, "Chưa có hướng dẫn viên!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean[] checkedItems = new boolean[guideNames.size()];
        for (int i = 0; i < guideNames.size(); i++) {
            checkedItems[i] = selectedGuideIds.contains(guideIds.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn hướng dẫn viên")
                .setMultiChoiceItems(guideNames.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
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
                .setPositiveButton("Xong", (dialog, which) -> {
                    tvGuideNames.setText(selectedGuideNames.isEmpty()
                            ? "(Chưa chọn hướng dẫn viên)"
                            : String.join(", ", selectedGuideNames));
                })
                .show();
    }

    private void uploadImagesToCloudinary(Runnable onComplete) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        new Thread(() -> {
            try {
                Cloudinary cloudinary = CloudinaryManager.getInstance();

                // 🔹 Xóa ảnh cũ trên Cloudinary trước
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    for (String oldUrl : imageUrls) {
                        try {
                            // Tách public_id từ link Cloudinary
                            String publicId = oldUrl.substring(oldUrl.lastIndexOf("/") + 1, oldUrl.lastIndexOf("."));
                            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                        } catch (Exception ignored) {}
                    }
                }

                // 🔹 Upload ảnh mới
                List<String> uploadedUrls = new ArrayList<>();
                for (Uri uri : newImageUris) {
                    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                        Map upload = cloudinary.uploader().upload(inputStream, ObjectUtils.emptyMap());
                        uploadedUrls.add(upload.get("secure_url").toString());
                    }
                }

                runOnUiThread(() -> {
                    imageUrls.clear(); // Xóa danh sách cũ
                    imageUrls.addAll(uploadedUrls); // Thêm link ảnh mới
                    onComplete.run(); // Tiếp tục lưu Firestore
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Lỗi upload Cloudinary: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveChangesToFirestore() {
        try {
            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String dest = etDestination.getText().toString().trim();
            String duration = etDuration.getText().toString().trim();
            String itinerary = etItinerary.getText().toString().trim();
            String startStr = etStartDate.getText().toString().trim();
            String endStr = etEndDate.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim().replace(".", "");

            if (title.isEmpty() || desc.isEmpty() || dest.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date start = sdf.parse(startStr);
            Date end = sdf.parse(endStr);
            Date now = new Date();

            String status;
            if (now.before(start)) status = "upcoming";
            else if (!now.before(start) && !now.after(end)) status = "in_progress";
            else status = "completed";

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("description", desc);
            data.put("destination", dest);
            data.put("duration", duration);
            data.put("itinerary", itinerary);
            data.put("price", price);
            data.put("start_date", new Timestamp(start));
            data.put("end_date", new Timestamp(end));
            data.put("images", imageUrls);
            data.put("status", status);
            data.put("guideIds", selectedGuideIds);
//            data.put("updatedAt", new Timestamp(new Date()));

            db.collection("tours").document(tourId)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "✅ Cập nhật tour thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "❌ Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            progressBar.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDate(Object obj) {
        if (obj == null) return "";
        try {
            if (obj instanceof Timestamp) {
                Date d = ((Timestamp) obj).toDate();
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d);
            }
            if (obj instanceof String) return (String) obj;
        } catch (Exception ignored) {}
        return "";
    }

    private void updateStatusBasedOnDates() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date start = sdf.parse(etStartDate.getText().toString());
            Date end = sdf.parse(etEndDate.getText().toString());
            Date now = new Date();

            String statusEn;
            if (now.before(start)) statusEn = "upcoming";
            else if (!now.before(start) && !now.after(end)) statusEn = "in_progress";
            else statusEn = "completed";

            String statusVi = convertStatusToVietnamese(statusEn);
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spStatus.getAdapter();
            int pos = adapter.getPosition(statusVi);
            if (pos >= 0) spStatus.setSelection(pos);
        } catch (Exception ignored) {}
    }

    private String convertStatusToVietnamese(String statusEn) {
        switch (statusEn) {
            case "upcoming": return "Chưa diễn ra";
            case "in_progress": return "Đang diễn ra";
            case "completed": return "Hoàn thành";
            case "cancelled": return "Hủy";
            default: return statusEn;
        }
    }

    private void setupLocationSelector() {
        etDestination.setOnClickListener(v -> {
            FirebaseFirestore.getInstance().collection("locations")
                    .get()
                    .addOnSuccessListener(provinceSnap -> {
                        List<String> provinceNames = new ArrayList<>();
                        List<String> provinceCodes = new ArrayList<>();
                        for (DocumentSnapshot doc : provinceSnap) {
                            provinceNames.add(doc.getString("name"));
                            provinceCodes.add(doc.getId());
                        }

                        new AlertDialog.Builder(this)
                                .setTitle("Chọn tỉnh/thành phố")
                                .setItems(provinceNames.toArray(new String[0]), (dialog, index) -> {
                                    String provinceCode = provinceCodes.get(index);
                                    FirebaseFirestore.getInstance()
                                            .collection("locations").document(provinceCode)
                                            .collection("wards").get()
                                            .addOnSuccessListener(wardSnap -> {
                                                List<String> wardNames = new ArrayList<>();
                                                for (DocumentSnapshot w : wardSnap)
                                                    wardNames.add(w.getString("name"));
                                                new AlertDialog.Builder(this)
                                                        .setTitle("Chọn phường/xã")
                                                        .setItems(wardNames.toArray(new String[0]), (d2, idx) -> {
                                                            String full = provinceNames.get(index) + " - " + wardNames.get(idx);
                                                            etDestination.setText(full);
                                                        })
                                                        .show();
                                            });
                                }).show();
                    });
        });
    }
}
