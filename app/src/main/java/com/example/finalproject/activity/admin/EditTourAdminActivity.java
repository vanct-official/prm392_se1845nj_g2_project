package com.example.finalproject.activity.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        tvGuideNames = findViewById(R.id.tvGuideNames);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        imageSlider = findViewById(R.id.imageSlider);
        progressBar = findViewById(R.id.progressBar);
        spStatus = findViewById(R.id.spStatus);
    }

    private void setupStatusSpinner() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Chưa diễn ra", "Đang diễn ra", "Hoàn thành", "Hủy"}
        );
        spStatus.setAdapter(statusAdapter);
        spStatus.setEnabled(false);
        spStatus.setClickable(false);
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

                    etTitle.setText(doc.getString("title"));
                    etDescription.setText(doc.getString("description"));
                    etDestination.setText(doc.getString("destination"));
                    etDuration.setText(doc.getString("duration"));
                    etItinerary.setText(doc.getString("itinerary"));

                    Double price = doc.getDouble("price");
                    if (price != null) {
                        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new Locale("vi", "VN"));
                        etPrice.setText(nf.format(price));
                    }

                    etStartDate.setText(formatDate(doc.get("start_date")));
                    etEndDate.setText(formatDate(doc.get("end_date")));

                    // Ảnh
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

                    // ====== Hướng dẫn viên ======
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

    /**
     * ✅ Lấy danh sách hướng dẫn viên từ bảng USERS (role = guide)
     */
    private void loadGuidesFromUsers() {
        db.collection("users")
                .whereEqualTo("role", "guide")
                .get()
                .addOnSuccessListener(query -> {
                    guideIds.clear();
                    guideNames.clear();
                    selectedGuideNames.clear();

                    for (DocumentSnapshot d : query) {
                        String id = d.getId();
                        String firstName = d.getString("firstname");
                        String lastName = d.getString("lastname");
                        String fullName = (firstName != null ? firstName : "") + " " +
                                (lastName != null ? lastName : "");
                        fullName = fullName.trim().isEmpty() ? id : fullName.trim();

                        guideIds.add(id);
                        guideNames.add(fullName);

                        if (selectedGuideIds.contains(id)) {
                            selectedGuideNames.add(fullName);
                        }
                    }

                    // Hiển thị danh sách đã chọn
                    if (selectedGuideNames.isEmpty()) {
                        tvGuideNames.setText("(Chưa chọn hướng dẫn viên)");
                    } else {
                        tvGuideNames.setText(String.join(", ", selectedGuideNames));
                    }

                    // Cho phép chọn lại
                    tvGuideNames.setOnClickListener(v -> showGuideSelectDialog());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải hướng dẫn viên: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showGuideSelectDialog() {
        if (guideNames.isEmpty()) {
            Toast.makeText(this, "Danh sách hướng dẫn viên trống!", Toast.LENGTH_SHORT).show();
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
                    if (selectedGuideNames.isEmpty()) {
                        tvGuideNames.setText("(Chưa chọn hướng dẫn viên)");
                    } else {
                        tvGuideNames.setText(String.join(", ", selectedGuideNames));
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
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

    private void saveChanges() {
        try {
            progressBar.setVisibility(android.view.View.VISIBLE);

            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String dest = etDestination.getText().toString().trim();
            String duration = etDuration.getText().toString().trim();
            String itinerary = etItinerary.getText().toString().trim();
            String startStr = etStartDate.getText().toString().trim();
            String endStr = etEndDate.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim().replace(".", "");

            if (title.isEmpty() || desc.isEmpty() || dest.isEmpty() || duration.isEmpty()
                    || itinerary.isEmpty() || startStr.isEmpty() || endStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "⚠️ Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(android.view.View.GONE);
                return;
            }

            // ✅ Kiểm tra bắt buộc chọn ít nhất 1 hướng dẫn viên
            if (selectedGuideIds.isEmpty()) {
                Toast.makeText(this, "⚠️ Vui lòng chọn ít nhất một hướng dẫn viên!", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(android.view.View.GONE);
                return;
            }

            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                Toast.makeText(this, "⚠️ Giá tour phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(android.view.View.GONE);
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date startDate = sdf.parse(startStr);
            Date endDate = sdf.parse(endStr);

            if (endDate.before(startDate)) {
                Toast.makeText(this, "⚠️ Ngày kết thúc phải sau ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(android.view.View.GONE);
                return;
            }

            String status;
            Date now = new Date();
            if (now.before(startDate)) status = "upcoming";
            else if (!now.before(startDate) && !now.after(endDate)) status = "in_progress";
            else status = "completed";

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("description", desc);
            data.put("destination", dest);
            data.put("duration", duration);
            data.put("itinerary", itinerary);
            data.put("price", price);
            data.put("start_date", new Timestamp(startDate));
            data.put("end_date", new Timestamp(endDate));
            data.put("guideIds", selectedGuideIds);
            data.put("images", imageUrls);
            data.put("status", status);
            data.put("updatedAt", new Timestamp(new Date()));

            db.collection("tours").document(tourId)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "✅ Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "❌ Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            progressBar.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    /**
     * 📍 Chọn địa điểm từ bảng locations (Firestore)
     */
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

                                    // Khi chọn xong, load danh sách phường/xã trong tỉnh
                                    FirebaseFirestore.getInstance()
                                            .collection("locations")
                                            .document(provinceCode)
                                            .collection("wards")
                                            .get()
                                            .addOnSuccessListener(wardSnap -> {
                                                List<String> wardNames = new ArrayList<>();
                                                for (DocumentSnapshot w : wardSnap) {
                                                    wardNames.add(w.getString("name"));
                                                }

                                                new AlertDialog.Builder(this)
                                                        .setTitle("Chọn phường/xã")
                                                        .setItems(wardNames.toArray(new String[0]), (d2, idx) -> {
                                                            String full = provinceNames.get(index) + " - " + wardNames.get(idx);
                                                            etDestination.setText(full);
                                                        })
                                                        .show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Lỗi tải danh sách phường/xã: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi tải danh sách tỉnh/thành: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

}
