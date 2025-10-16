package com.example.finalproject.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.utils.ObjectUtils;
import com.example.finalproject.R;
import com.example.finalproject.utils.CloudinaryManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTourActivity extends AppCompatActivity {

    private EditText etTourName, etDescription, etLocation, etPrice, etAvailableSeats,
            etDepositPercent, etStartDate, etEndDate;
    private Button btnChooseImages, btnCancel, btnSave;
    private TextView tvImageCount, tvSelectedGuides;
    private ProgressBar progressBar;

    private static final int PICK_IMAGES_REQUEST = 100;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> guideIds = new ArrayList<>();
    private List<String> guideNames = new ArrayList<>();
    private List<String> selectedGuideIds = new ArrayList<>();
    private List<String> selectedGuideNames = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tour);

        db = FirebaseFirestore.getInstance();

        // Ánh xạ view
        etTourName = findViewById(R.id.etTourName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etPrice = findViewById(R.id.etPrice);
        etAvailableSeats = findViewById(R.id.etAvailableSeats);
        etDepositPercent = findViewById(R.id.etDepositPercent);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        tvImageCount = findViewById(R.id.tvImageCount);
        progressBar = findViewById(R.id.progressBar);
        tvSelectedGuides = findViewById(R.id.tvSelectedGuides);

        // Load danh sách hướng dẫn viên
        loadGuides();

        // Chọn ngày
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        // Chọn ảnh
        btnChooseImages.setOnClickListener(v -> openGallery());

        // Hủy
        btnCancel.setOnClickListener(v -> finish());

        // Lưu tour
        btnSave.setOnClickListener(v -> saveTour());
    }

    // ===========================================================
    // LOAD DANH SÁCH HƯỚNG DẪN VIÊN
    // ===========================================================
    private void loadGuides() {
        db.collection("guides")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    guideIds.clear();
                    guideNames.clear();

                    for (DocumentSnapshot doc : querySnapshot) {
                        guideIds.add(doc.getId());
                        String name = doc.getString("name");
                        guideNames.add(name != null ? name : doc.getId());
                    }

                    // Bấm để mở dialog chọn nhiều hướng dẫn viên
                    tvSelectedGuides.setOnClickListener(v -> showMultiSelectDialog());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải hướng dẫn viên: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ===========================================================
    // DIALOG CHỌN NHIỀU HƯỚNG DẪN VIÊN
    // ===========================================================
    private void showMultiSelectDialog() {
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
                        tvSelectedGuides.setText("Chọn hướng dẫn viên");
                    } else {
                        tvSelectedGuides.setText(String.join(", ", selectedGuideNames));
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ===========================================================
    // CHỌN NGÀY
    // ===========================================================
    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            target.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ===========================================================
    // CHỌN ẢNH TỪ THƯ VIỆN
    // ===========================================================
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
            selectedImageUris.clear();

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    selectedImageUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedImageUris.add(data.getData());
            }

            tvImageCount.setText("Đã chọn " + selectedImageUris.size() + " ảnh");
        }
    }

    // ===========================================================
    // LƯU TOUR VÀO FIRESTORE
    // ===========================================================
    private void saveTour() {
        String name = etTourName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String loc = etLocation.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String seatStr = etAvailableSeats.getText().toString().trim();
        String depositStr = etDepositPercent.getText().toString().trim();
        String startStr = etStartDate.getText().toString().trim();
        String endStr = etEndDate.getText().toString().trim();

        // Kiểm tra rỗng
        if (name.isEmpty() || desc.isEmpty() || loc.isEmpty() ||
                priceStr.isEmpty() || seatStr.isEmpty() || depositStr.isEmpty() ||
                startStr.isEmpty() || endStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int seats, deposit;
        Date startDate, endDate;

        try {
            price = Double.parseDouble(priceStr);
            seats = Integer.parseInt(seatStr);
            deposit = Integer.parseInt(depositStr);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            startDate = sdf.parse(startStr);
            endDate = sdf.parse(endStr);
        } catch (Exception e) {
            Toast.makeText(this, "Dữ liệu nhập không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate logic
        if (price <= 0) {
            Toast.makeText(this, "Giá tour phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (deposit < 1 || deposit > 99) {
            Toast.makeText(this, "% đặt cọc phải từ 1 đến 99!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endDate.before(startDate) || endDate.equals(startDate)) {
            Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedGuideIds.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một hướng dẫn viên!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);

        // Kiểm tra trùng tên tour
        db.collection("tours")
                .whereEqualTo("tourName", name)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "Tên tour đã tồn tại!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(() -> {
                        try {
                            List<String> imageUrls = new ArrayList<>();
                            for (Uri uri : selectedImageUris) {
                                InputStream is = getContentResolver().openInputStream(uri);
                                Bitmap bitmap = BitmapFactory.decodeStream(is);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                                byte[] data = baos.toByteArray();

                                Map uploadResult = CloudinaryManager.getInstance()
                                        .uploader()
                                        .upload(data, ObjectUtils.emptyMap());
                                String url = (String) uploadResult.get("secure_url");
                                imageUrls.add(url);
                            }

                            Map<String, Object> tour = new HashMap<>();
                            tour.put("tourName", name);
                            tour.put("description", desc);
                            tour.put("location", loc);
                            tour.put("price", price);
                            tour.put("availableSeats", seats);
                            tour.put("depositPercent", deposit);
                            tour.put("startDate", new Timestamp(startDate));
                            tour.put("endDate", new Timestamp(endDate));
                            tour.put("images", imageUrls);
                            tour.put("guideIds", selectedGuideIds); // ✅ nhiều hướng dẫn viên
                            tour.put("createAt", new Timestamp(new Date()));
                            tour.put("updateAt", new Timestamp(new Date()));

                            db.collection("tours")
                                    .add(tour)
                                    .addOnSuccessListener(doc -> runOnUiThread(() -> {
                                        progressBar.setVisibility(android.view.View.GONE);
                                        Toast.makeText(this, "Thêm tour thành công!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }))
                                    .addOnFailureListener(e -> runOnUiThread(() -> {
                                        progressBar.setVisibility(android.view.View.GONE);
                                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }));

                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(android.view.View.GONE);
                                Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Lỗi khi kiểm tra tên tour: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
