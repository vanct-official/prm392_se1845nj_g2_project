package com.example.finalproject.activity.customer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.example.finalproject.entity.Booking;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BookingDetailActivity extends AppCompatActivity {

    private TextView tvTourTitle, tvBookingDate, tvBookingStatus, tvCustomerName,
            tvNumPeople, tvNotes, tvCreatedAt;
    private Button btnCancelBooking;
    private ImageView ivBack;

    private String bookingId;
    private Booking booking;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        ivBack = findViewById(R.id.ivBack);
        tvTourTitle = findViewById(R.id.tvTourTitle);
        tvBookingDate = findViewById(R.id.tvBookingDate);
        tvBookingStatus = findViewById(R.id.tvBookingStatus);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvNumPeople = findViewById(R.id.tvNumPeople);
        tvNotes = findViewById(R.id.tvNotes);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);

        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null || bookingId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy booking hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivBack.setOnClickListener(v -> finish());
        btnCancelBooking.setOnClickListener(v -> showCancelDialog());

        loadBookingDetail(bookingId);
    }

    private void loadBookingDetail(String bookingId) {
        db.collection("bookings").document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Booking không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    booking = doc.toObject(Booking.class);
                    if (booking == null) return;

                    tvBookingStatus.setText(booking.getStatus() != null ? booking.getStatus() : "Chờ xử lý");
                    tvNumPeople.setText(String.valueOf(booking.getQuantity() != null ? booking.getQuantity() : 1));
                    tvNotes.setText(booking.getNote() != null ? booking.getNote() : "Không có ghi chú");

                    if (booking.getCreateAt() != null) {
                        tvCreatedAt.setText("Tạo lúc: " +
                                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                        .format(booking.getCreateAt().toDate()));
                    }

                    // --- Lấy tên khách hàng ---
                    db.collection("users").document(booking.getUserId())
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc.exists()) {
                                    String name = userDoc.getString("firstname") + " " + userDoc.getString("lastname");
                                    tvCustomerName.setText(name);
                                } else {
                                    tvCustomerName.setText("Không xác định");
                                }
                            });

                    // --- Lấy thông tin tour (title + startDate) ---
                    db.collection("tours").document(booking.getTourId())
                            .get()
                            .addOnSuccessListener(tourDoc -> handleTourInfo(tourDoc))
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi tải tour: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải chi tiết: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleTourInfo(DocumentSnapshot tourDoc) {
        if (!tourDoc.exists()) {
            tvTourTitle.setText("Không tìm thấy tour");
            return;
        }

        String title = tourDoc.getString("title");
        Timestamp startDate = tourDoc.getTimestamp("start_date");

        tvTourTitle.setText(title != null ? title : "Không có tiêu đề");

        if (startDate != null) {
            tvBookingDate.setText(
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(startDate.toDate()));
            checkCancelEligibility(startDate);
        } else {
            btnCancelBooking.setVisibility(View.GONE);
        }
    }

    private void checkCancelEligibility(Timestamp startDate) {
        if (booking == null || startDate == null) {
            btnCancelBooking.setVisibility(View.GONE);
            return;
        }

        String status = booking.getStatus();
        if ("cancelled".equalsIgnoreCase(status) || "successfully".equalsIgnoreCase(status)) {
            btnCancelBooking.setVisibility(View.GONE);
            return;
        }

        long diffMillis = startDate.toDate().getTime() - System.currentTimeMillis();
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

        // Cho phép hủy nếu còn ít nhất 1 ngày trước khi tour bắt đầu
        if (diffDays >= 1) {
            btnCancelBooking.setVisibility(View.VISIBLE);
        } else {
            btnCancelBooking.setVisibility(View.GONE);
        }
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy chuyến")
                .setMessage("Bạn có chắc muốn hủy chuyến đi này không?")
                .setPositiveButton("Hủy chuyến", (dialog, which) -> cancelBooking())
                .setNegativeButton("Không", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void cancelBooking() {
        db.collection("bookings").document(bookingId)
                .update("status", "cancelled")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã hủy chuyến thành công.", Toast.LENGTH_SHORT).show();
                    tvBookingStatus.setText("cancelled");
                    btnCancelBooking.setVisibility(View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi hủy chuyến: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
