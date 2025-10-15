package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AddTourActivity extends AppCompatActivity {

    private static final String TAG = "AddTourActivity";
    private static final int PICK_IMAGES_REQUEST = 1001;

    private EditText etTourName, etDescription, etLocation, etPrice, etAvailableSeats, etDepositPercent, etStartDate, etEndDate;
    private Button btnChooseImages, btnCancel, btnSave;
    private TextView tvImageCount;
    private ProgressBar progressBar;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tour);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

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

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnChooseImages.setOnClickListener(v -> openImagePicker());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> validateAndSaveTour());
    }

    // ===========================================================
    // 🔹 Chọn ngày
    // ===========================================================
    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    target.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    // ===========================================================
    // 🖼️ Chọn ảnh từ thiết bị
    // ===========================================================
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh tour"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            selectedImageUris.clear();
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        selectedImageUris.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null) {
                    selectedImageUris.add(data.getData());
                }
                tvImageCount.setText("Đã chọn " + selectedImageUris.size() + " ảnh");
                Log.d(TAG, "Đã chọn " + selectedImageUris.size() + " ảnh");
            }
        }
    }

    // ===========================================================
    // 🔍 Kiểm tra và lưu tour
    // ===========================================================
    private void validateAndSaveTour() {
        String name = etTourName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String seatsStr = etAvailableSeats.getText().toString().trim();
        String depositStr = etDepositPercent.getText().toString().trim();
        String startStr = etStartDate.getText().toString().trim();
        String endStr = etEndDate.getText().toString().trim();

        if (name.isEmpty() || desc.isEmpty() || location.isEmpty() ||
                priceStr.isEmpty() || seatsStr.isEmpty() || depositStr.isEmpty() ||
                startStr.isEmpty() || endStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int seats;
        int deposit;

        try {
            price = Double.parseDouble(priceStr);
            seats = Integer.parseInt(seatsStr);
            deposit = Integer.parseInt(depositStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá trị nhập không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (deposit < 0 || deposit > 100) {
            Toast.makeText(this, "Phần trăm đặt cọc phải từ 0-100!", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date startDate, endDate;
        try {
            startDate = sdf.parse(startStr);
            endDate = sdf.parse(endStr);

            if (startDate.after(endDate)) {
                Toast.makeText(this, "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Định dạng ngày không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnSave.setEnabled(false); // Vô hiệu hóa nút để tránh click nhiều lần

        String tourId = db.collection("tours").document().getId();
        Log.d(TAG, "Bắt đầu upload tour ID: " + tourId);

        uploadImagesAndSave(tourId, name, desc, location, price, seats, deposit, startDate, endDate);
    }

    // ===========================================================
    // ☁️ Upload ảnh lên Firebase Storage (ĐÃ SỬA)
    // ===========================================================
    private void uploadImagesAndSave(String tourId, String name, String desc, String location,
                                     double price, int seats, int deposit, Date startDate, Date endDate) {

        List<String> imageUrls = new ArrayList<>();
        StorageReference storageRef = storage.getReference().child("tours/" + tourId);

        // Nếu không có ảnh
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Chưa chọn ảnh!", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(android.view.View.GONE);
            return;
        }

        for (Uri uri : selectedImageUris) {
            StorageReference fileRef = storageRef.child(System.currentTimeMillis() + ".jpg");

            // 🟢 THÊM DÒNG LOG ĐẦU TIÊN — kiểm tra ảnh đang upload
            android.util.Log.d("UPLOAD_DEBUG", "Đang upload ảnh: " + uri);
            // 🔹 Gọi putFile() để upload ảnh
            fileRef.putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        // 🔹 Lấy link tải thực từ Storage
                        return fileRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> {
                        // 🟢 THÊM DÒNG LOG THỨ HAI — kiểm tra link sau khi upload xong
                        android.util.Log.d("UPLOAD_DEBUG", "URL tải thành công: " + downloadUri);
                        imageUrls.add(downloadUri.toString());

                        // ✅ Khi tất cả ảnh đã upload xong → lưu Firestore
                        if (imageUrls.size() == selectedImageUris.size()) {
                            saveTourToFirestore(tourId, name, desc, location, price, seats, deposit, startDate, endDate, imageUrls);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // ===========================================================
    // 🔥 Lưu dữ liệu tour vào Firestore
    // ===========================================================
    private void saveTourToFirestore(String tourId, String name, String desc, String location,
                                     double price, int seats, int deposit, Date startDate, Date endDate, List<String> imageUrls) {

        Log.d(TAG, "Lưu tour vào Firestore với " + imageUrls.size() + " ảnh");

        Map<String, Object> tour = new HashMap<>();
        tour.put("tourName", name);
        tour.put("description", desc);
        tour.put("location", location);
        tour.put("price", price);
        tour.put("availableSeats", seats);
        tour.put("depositPercent", deposit);
        tour.put("startDate", new Timestamp(startDate));
        tour.put("endDate", new Timestamp(endDate));
        tour.put("images", imageUrls);
        tour.put("createAt", new Timestamp(new Date()));
        tour.put("updateAt", new Timestamp(new Date()));

        db.collection("tours").document(tourId)
                .set(tour)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Lưu Firestore thành công!");
                    progressBar.setVisibility(android.view.View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "✅ Thêm tour thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi lưu Firestore: " + e.getMessage(), e);
                    progressBar.setVisibility(android.view.View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Lỗi lưu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}