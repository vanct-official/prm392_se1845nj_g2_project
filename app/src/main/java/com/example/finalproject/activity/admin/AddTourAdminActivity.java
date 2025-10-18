package com.example.finalproject.activity.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.TimeZone;


public class AddTourAdminActivity extends AppCompatActivity {

    private static final String TAG = "AddTourAdminActivity";
    private EditText etTitle, etDescription, etDestination, etDuration, etItinerary, etPrice, etStartDate, etEndDate;
    private Button btnChooseImages, btnCancel, btnSave;
    private TextView tvImageCount, tvSelectedGuides;
    private ProgressBar progressBar;
    private Spinner spStatus;

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
        setContentView(R.layout.activity_add_tour_admin);

        db = FirebaseFirestore.getInstance();

        // Ánh xạ view
        spStatus = findViewById(R.id.spStatus);
        etTitle = findViewById(R.id.etTourName);
        etDescription = findViewById(R.id.etDescription);
        etDestination = findViewById(R.id.etLocation);
        etDuration = findViewById(R.id.etAvailableSeats);
        etItinerary = findViewById(R.id.etDepositPercent);
        etPrice = findViewById(R.id.etPrice);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        tvImageCount = findViewById(R.id.tvImageCount);
        progressBar = findViewById(R.id.progressBar);
        tvSelectedGuides = findViewById(R.id.tvSelectedGuides);

        // Load hướng dẫn viên
        loadGuides();

        // Khởi tạo Spinner (trạng thái – chỉ hiển thị, không dùng để lưu)
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Chưa diễn ra", "Đang diễn ra", "Hoàn thành", "Hủy"}
        );
        spStatus.setAdapter(statusAdapter);
        spStatus.setEnabled(false); // chỉ hiển thị, không cho chọn

        // Sự kiện chọn ngày
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnChooseImages.setOnClickListener(v -> openGallery());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> validateAndSaveTour());
    }

    // ===========================================================
    // 🔹 Tải danh sách hướng dẫn viên
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

                    tvSelectedGuides.setOnClickListener(v -> showMultiSelectDialog());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải hướng dẫn viên: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

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

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            target.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh tour"), PICK_IMAGES_REQUEST);
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

    /**
     * Chuẩn hóa ngày về 00:00:00 để so sánh chính xác
     */
    private Date normalizeDate(Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Tính toán status dựa trên ngày
     */
    private String calculateStatus(Date startDate, Date endDate) {
        Date now = normalizeDate(new Date());
        Date start = normalizeDate(startDate);
        Date end = normalizeDate(endDate);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        Log.d(TAG, "Now: " + sdf.format(now));
        Log.d(TAG, "Start: " + sdf.format(start));
        Log.d(TAG, "End: " + sdf.format(end));

        String status;

        if (now.before(start)) {
            // Ngày hiện tại < ngày bắt đầu → upcoming
            status = "upcoming";
            Log.d(TAG, "Status: upcoming (now < start)");
        } else if (now.equals(start) || (now.after(start) && (now.before(end) || now.equals(end)))) {
            // Ngày hiện tại >= ngày bắt đầu VÀ <= ngày kết thúc → in_progress
            status = "in_progress";
            Log.d(TAG, "Status: in_progress (start <= now <= end)");
        } else {
            // Ngày hiện tại > ngày kết thúc → completed
            status = "completed";
            Log.d(TAG, "Status: completed (now > end)");
        }

        return status;
    }

    // ===========================================================
    // ✅ Validate trước khi lưu
    // ===========================================================
    private void validateAndSaveTour() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String dest = etDestination.getText().toString().trim();
        String duration = etDuration.getText().toString().trim();
        String itinerary = etItinerary.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String startStr = etStartDate.getText().toString().trim();
        String endStr = etEndDate.getText().toString().trim();

        if (title.isEmpty() || desc.isEmpty() || dest.isEmpty() || duration.isEmpty() ||
                itinerary.isEmpty() || priceStr.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
            Toast.makeText(this, "⚠️ Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedGuideIds.isEmpty()) {
            Toast.makeText(this, "⚠️ Vui lòng chọn ít nhất một hướng dẫn viên!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "⚠️ Vui lòng chọn ít nhất một ảnh tour!", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        Date startDate, endDate;

        try {
            price = Double.parseDouble(priceStr);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

            startDate = sdf.parse(startStr);
            endDate = sdf.parse(endStr);

            if (startDate == null || endDate == null) {
                throw new Exception("Không thể parse ngày");
            }

        } catch (Exception e) {
            Toast.makeText(this, "⚠️ Dữ liệu nhập không hợp lệ! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Parse error", e);
            return;
        }

        if (price <= 0) {
            Toast.makeText(this, "⚠️ Giá tour phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
            return;
        }

        // So sánh ngày đã chuẩn hóa
        Date normalizedStart = normalizeDate(startDate);
        Date normalizedEnd = normalizeDate(endDate);

        if (normalizedEnd.before(normalizedStart)) {
            Toast.makeText(this, "⚠️ Ngày kết thúc phải sau hoặc bằng ngày bắt đầu!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra trùng tiêu đề tour
        progressBar.setVisibility(android.view.View.VISIBLE);
        db.collection("tours")
                .whereEqualTo("title", title)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "⚠️ Tiêu đề tour đã tồn tại!", Toast.LENGTH_SHORT).show();
                    } else {
                        saveTour(title, desc, dest, duration, itinerary, price, startDate, endDate);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Lỗi kiểm tra tiêu đề: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ===========================================================
    // 💾 Lưu tour vào Firestore
    // ===========================================================
    private void saveTour(String title, String desc, String dest, String duration,
                          String itinerary, double price, Date startDate, Date endDate) {

        new Thread(() -> {
            try {
                // Upload ảnh lên Cloudinary
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
                    imageUrls.add((String) uploadResult.get("secure_url"));
                }

                // ✅ Tính toán status tự động
                String status = calculateStatus(startDate, endDate);

                Log.d(TAG, "Final status to save: " + status);

                // ✅ Tạo dữ liệu tour
                Map<String, Object> tour = new HashMap<>();
                tour.put("title", title);
                tour.put("description", desc);
                tour.put("destination", dest);
                tour.put("duration", duration);
                tour.put("itinerary", itinerary);
                tour.put("price", price);
                tour.put("start_date", new Timestamp(startDate));
                tour.put("end_date", new Timestamp(endDate));
                tour.put("guideIds", selectedGuideIds);
                tour.put("images", imageUrls);
                tour.put("status", status);
                tour.put("created_at", new Timestamp(new Date()));

                // Lưu vào Firestore
                db.collection("tours")
                        .add(tour)
                        .addOnSuccessListener(doc -> runOnUiThread(() -> {
                            progressBar.setVisibility(android.view.View.GONE);
                            Toast.makeText(this, "✅ Thêm tour thành công! Status: " + status, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Tour saved successfully with status: " + status);
                            finish();
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            progressBar.setVisibility(android.view.View.GONE);
                            Toast.makeText(this, "❌ Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to save tour", e);
                        }));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "❌ Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Upload error", e);
                });
            }
        }).start();
    }
}