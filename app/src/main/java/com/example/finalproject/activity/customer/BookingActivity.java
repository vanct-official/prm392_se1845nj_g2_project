package com.example.finalproject.activity.customer;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.CustomerActivity;
import com.example.finalproject.R;
import com.example.finalproject.entity.Promotion;
import com.example.finalproject.interfaces.FirestoreCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Zalo Pay
import com.example.finalproject.Api.CreateOrder;
import org.json.JSONObject;

import vn.zalopay.sdk.Environment;
import vn.zalopay.sdk.ZaloPayError;
import vn.zalopay.sdk.ZaloPaySDK;
import vn.zalopay.sdk.listeners.PayOrderListener;

public class BookingActivity extends AppCompatActivity {

    private TextView tvTourTitle, tvStartDate, tvPrice, tvSubtotal, tvDiscount, tvTotal;
    private EditText etQuantity, etNote, etPromoCode;
    private Spinner spPaymentMethod;
    private Button btnConfirm, btnApplyPromo;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String userId, tourId, tourTitle;
    private double tourPrice;
    private Timestamp start_date;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    // Promotion
    private Promotion selectedPromotion = null;
    private double discountAmount = 0;
    private double finalPrice = 0;

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

        // Nhận start date
        long startDateMillis = getIntent().getLongExtra("startDateMillis", 0);
        if (startDateMillis != 0) {
            start_date = new Timestamp(new java.util.Date(startDateMillis));
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // ZaloPay SDK Init
        ZaloPaySDK.init(2554, Environment.SANDBOX);

        mapViews();
        bindTourInfo();
        setupConfirmButton();
    }

    private void mapViews() {
        tvTourTitle = findViewById(R.id.tvTourTitleBooking);
        tvStartDate = findViewById(R.id.tvStartDateBooking);
        tvPrice = findViewById(R.id.tvPriceBooking);
        tvSubtotal = findViewById(R.id.tvSubtotalBooking);
        tvDiscount = findViewById(R.id.tvDiscountBooking);
        tvTotal = findViewById(R.id.tvTotalBooking);
        etQuantity = findViewById(R.id.etQuantityBooking);
        etNote = findViewById(R.id.etNoteBooking);
        etPromoCode = findViewById(R.id.etPromoCodeBooking);
        btnApplyPromo = findViewById(R.id.btnApplyPromo);
        spPaymentMethod = findViewById(R.id.spPaymentMethodBooking);
        btnConfirm = findViewById(R.id.btnConfirmBooking);
        progressBar = findViewById(R.id.progressBarBooking);

        // Spinner setup
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"cash", "Zalo Pay"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPaymentMethod.setAdapter(adapter);

        btnApplyPromo.setOnClickListener(v -> applyPromotion());
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
        double subtotal = getSubtotal();
        if (selectedPromotion != null) {
            discountAmount = subtotal * selectedPromotion.getDiscountPercent() / 100.0;
            finalPrice = subtotal - discountAmount;
            tvDiscount.setText("Giảm giá: " + currency.format(discountAmount));
            tvTotal.setText("Tổng sau giảm: " + currency.format(finalPrice));
        } else {
            discountAmount = 0;
            finalPrice = subtotal;
            tvDiscount.setText("Giảm giá: " + currency.format(0));
            tvTotal.setText("Tổng cộng: " + currency.format(subtotal));
        }
    }

    private double getSubtotal() {
        int qty = 0;
        try { qty = Integer.parseInt(etQuantity.getText().toString()); } catch (Exception ignored) {}
        return qty * tourPrice;
    }

    // Áp dụng mã giảm giá
    private void applyPromotion() {
        String code = etPromoCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã giảm giá", Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = getSubtotal();
        if (subtotal <= 0) {
            Toast.makeText(this, "Hãy nhập số lượng trước khi áp dụng mã", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("promotions")
                .whereEqualTo("name", code)
                .get()
                .addOnSuccessListener(query -> handlePromotionResult(query, subtotal))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi kiểm tra mã: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handlePromotionResult(QuerySnapshot query, double subtotal) {
        if (query.isEmpty()) {
            Toast.makeText(this, "Mã giảm giá không tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentSnapshot doc = query.getDocuments().get(0);
        Promotion promo = doc.toObject(Promotion.class);
        if (promo == null) {
            Toast.makeText(this, "Không thể đọc thông tin mã", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Gắn ID document Firestore (đây chính là promotionId bạn sẽ lưu trong booking)
        promo.setId(doc.getId());

        if (!promo.isActive()) {
            Toast.makeText(this, "Mã giảm giá không hợp lệ hoặc hết hạn", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subtotal < promo.getMinimumValue()) {
            Toast.makeText(this, "Đơn hàng chưa đạt giá trị tối thiểu: " +
                    currency.format(promo.getMinimumValue()), Toast.LENGTH_SHORT).show();
            return;
        }

        selectedPromotion = promo;
        discountAmount = subtotal * promo.getDiscountPercent() / 100.0;
        finalPrice = subtotal - discountAmount;

        tvSubtotal.setText("Tạm tính: " + currency.format(subtotal));
        tvDiscount.setText("Giảm giá: " + currency.format(discountAmount));
        tvTotal.setText("Tổng sau giảm: " + currency.format(finalPrice));

        Toast.makeText(this, "Áp dụng mã thành công! Giảm " +
                promo.getDiscountPercent() + "%", Toast.LENGTH_LONG).show();
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

            double subtotal = getSubtotal();

            double discountPercent = 0;
            String promotionId = "";
            if (selectedPromotion != null) {
                discountPercent = selectedPromotion.getDiscountPercent();
                promotionId = selectedPromotion.getId();
            }

            Map<String, Object> booking = new HashMap<>();
            booking.put("amountPaid", 0);
            booking.put("amountRemaining", finalPrice);
            booking.put("createAt", Timestamp.now());
            booking.put("discountAmount", discountAmount);
            booking.put("discountPercent", discountPercent);
            booking.put("finalPrice", finalPrice);
            booking.put("note", etNote.getText().toString());
            booking.put("paymentMethod", spPaymentMethod.getSelectedItem().toString());
            booking.put("paymentStatus", "pending");
            booking.put("promotionId", promotionId);
            booking.put("quantity", quantity);
            booking.put("status", "confirmed");
            booking.put("subtotal", subtotal);
            booking.put("tourId", tourId);
            booking.put("updateAt", Timestamp.now());
            booking.put("userId", userId);

            String paymentMethod = spPaymentMethod.getSelectedItem().toString();

            if (paymentMethod.equals("cash")) {
                saveBookingToFirestore(booking, new FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(BookingActivity.this, "Đặt tour thành công (Thanh toán tiền mặt)!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(BookingActivity.this, CustomerActivity.class);
                        intent.putExtra("openFragment", "bookingList");
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(BookingActivity.this, "Lưu dữ liệu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                handleOnlinePayment(booking);
            }
        });
    }

    private void saveBookingToFirestore(Map<String, Object> booking, FirestoreCallback callback) {
        db.collection("bookings").add(booking)
                .addOnSuccessListener(ref -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirm.setEnabled(true);

                    // 🔹 Tạo payment record tương ứng
                    createPaymentRecord(ref.getId(), booking);

                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirm.setEnabled(true);
                    callback.onFailure(e);
                });
    }

    private void handleOnlinePayment(Map<String, Object> booking) {
        try {
            CreateOrder orderApi = new CreateOrder();
            JSONObject data = orderApi.createOrder(String.valueOf((int) (double) booking.get("finalPrice")));

            if (data != null && data.has("return_code") && data.getInt("return_code") == 1) {
                String zpTransToken = data.getString("zp_trans_token");

                ZaloPaySDK.getInstance().payOrder(this, zpTransToken, "touriovn://app", new PayOrderListener() {
                    @Override
                    public void onPaymentSucceeded(String transactionId, String transToken, String appTransID) {
                        booking.put("transactionId", transactionId);
                        booking.put("paymentStatus", "paid");

                        saveBookingToFirestore(booking, new FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(BookingActivity.this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(BookingActivity.this, CustomerActivity.class);
                                intent.putExtra("openFragment", "bookingList");
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(BookingActivity.this, "Lưu dữ liệu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onPaymentCanceled(String zpTransToken, String appTransID) {
                        Toast.makeText(BookingActivity.this, "Bạn đã hủy thanh toán", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                    }

                    @Override
                    public void onPaymentError(ZaloPayError error, String zpTransToken, String appTransID) {
                        Toast.makeText(BookingActivity.this, "Lỗi thanh toán: " + error.toString(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                    }
                });

            } else {
                String msg = (data == null) ? "ZaloPay trả về null" : data.toString();
                Toast.makeText(this, "Không thể tạo đơn hàng ZaloPay: " + msg, Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
            }
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnConfirm.setEnabled(true);
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý dữ liệu ZaloPay: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Tạo Payment
    private void createPaymentRecord(String bookingId, Map<String, Object> booking) {
        Map<String, Object> payment = new HashMap<>();
        payment.put("bookingId", bookingId);
        payment.put("userId", booking.get("userId"));
        payment.put("amount", booking.get("finalPrice"));
        payment.put("method", booking.get("paymentMethod"));
        payment.put("status", booking.get("paymentStatus"));
        payment.put("note", booking.get("note"));
        payment.put("paymentTime", Timestamp.now());
        payment.put("refund", false);
        payment.put("refundRequested", false);
        payment.put("refund_information", null); // 🔹 vì khi mới tạo, chưa có refund
        payment.put("transaction_ref", booking.containsKey("transactionId") ? booking.get("transactionId") : "");
        payment.put("createdAt", Timestamp.now());
        payment.put("updatedAt", Timestamp.now());

        db.collection("payments")
                .add(payment)
                .addOnSuccessListener(ref -> {
                    System.out.println("✅ Payment created successfully: " + ref.getId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ZaloPaySDK.getInstance().onResult(intent);
    }
}
