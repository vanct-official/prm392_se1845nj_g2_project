package com.example.finalproject.activity.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private TextView tvTourTitle, tvStartDate, tvPrice, tvTotal;
    private EditText etQuantity, etNote;
    private Spinner spPaymentMethod;
    private Button btnConfirm;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String userId, tourId, tourTitle;
    private double tourPrice;
    private Timestamp start_date;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        // Nhận dữ liệu từ Intent
        tourId = getIntent().getStringExtra("tourId");
        tourTitle = getIntent().getStringExtra("tourTitle");
        tourPrice = getIntent().getDoubleExtra("tourPrice", 0);

        // Nhận start date và chuyển ngược lại sang Timestamp
        long startDateMillis = getIntent().getLongExtra("startDateMillis", 0);
        if (startDateMillis != 0) {
            start_date = new Timestamp(new java.util.Date(startDateMillis));
        }


        mapViews();
        bindTourInfo();
        setupConfirmButton();
    }

    private void mapViews() {
        tvTourTitle = findViewById(R.id.tvTourTitleBooking);
        tvStartDate = findViewById(R.id.tvStartDateBooking);
        tvPrice = findViewById(R.id.tvPriceBooking);
        tvTotal = findViewById(R.id.tvTotalBooking);
        etQuantity = findViewById(R.id.etQuantityBooking);
        etNote = findViewById(R.id.etNoteBooking);
        spPaymentMethod = findViewById(R.id.spPaymentMethodBooking);
        btnConfirm = findViewById(R.id.btnConfirmBooking);
        progressBar = findViewById(R.id.progressBarBooking);

        // Spinner setup
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"cash", "online"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPaymentMethod.setAdapter(adapter);
    }

    private void bindTourInfo() {
        tvTourTitle.setText(tourTitle);
        tvPrice.setText("Giá: " + currency.format(tourPrice) + "/người");
        if (start_date != null)
            tvStartDate.setText("Khởi hành: " + start_date.toDate().toString());

        etQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotal();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void updateTotal() {
        int qty = 0;
        try { qty = Integer.parseInt(etQuantity.getText().toString()); } catch (Exception ignored) {}
        double total = qty * tourPrice;
        tvTotal.setText("Tổng cộng: " + currency.format(total));
    }

    private void setupConfirmButton() {
        btnConfirm.setOnClickListener(v -> {
            int quantity;
            try {
                quantity = Integer.parseInt(etQuantity.getText().toString());
            } catch (Exception e) {
                Toast.makeText(this, "Vui lòng nhập số lượng hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (quantity <= 0) {
                Toast.makeText(this, "Số lượng phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId == null) {
                Toast.makeText(this, "Bạn cần đăng nhập để đặt tour", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnConfirm.setEnabled(false);

            double subtotal = quantity * tourPrice;

            Map<String, Object> booking = new HashMap<>();
            booking.put("amountPaid", 0);
            booking.put("amountRemaining", subtotal);
            booking.put("createAt", Timestamp.now());
            booking.put("discountAmount", 0);
            booking.put("discountPercent", 0);
            booking.put("finalPrice", subtotal);
            booking.put("note", etNote.getText().toString());
            booking.put("paymentMethod", spPaymentMethod.getSelectedItem().toString());
            booking.put("paymentStatus", "pending");
            booking.put("promotionId", "");
            booking.put("quantity", quantity);
            booking.put("status", "confirmed");
            booking.put("subtotal", subtotal);
            booking.put("tourId", tourId);
            booking.put("updateAt", Timestamp.now());
            booking.put("userId", userId);

            db.collection("bookings").add(booking)
                    .addOnSuccessListener(ref -> {
                        progressBar.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                        Toast.makeText(this, "Đặt tour thành công!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
