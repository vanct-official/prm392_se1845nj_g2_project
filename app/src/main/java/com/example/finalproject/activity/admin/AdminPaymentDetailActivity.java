// java
package com.example.finalproject.activity.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.finalproject.R;
import com.example.finalproject.entity.Payment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminPaymentDetailActivity extends AppCompatActivity {

    private TextView txtPaymentId, txtAmount, txtBookingId, txtMethod, txtNote,
            txtStatus, txtTransactionRef, txtDate, txtUserName, txtRefundInfo;
    private SwitchCompat switchRefund;
    private Button btnCancelPayment, btnConfirmPayment;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_payment_detail);

        // Ánh xạ view
        txtPaymentId = findViewById(R.id.txtPaymentId);
        txtUserName = findViewById(R.id.txtUserName);
        txtAmount = findViewById(R.id.txtAmount);
        txtBookingId = findViewById(R.id.txtBookingId);
        txtMethod = findViewById(R.id.txtMethod);
        txtNote = findViewById(R.id.txtNote);
        txtStatus = findViewById(R.id.txtStatus);
        txtTransactionRef = findViewById(R.id.txtTransactionRef);
        txtDate = findViewById(R.id.txtDate);
        txtRefundInfo = findViewById(R.id.txtRefundInfo);
        switchRefund = findViewById(R.id.switchRefund);
        btnCancelPayment = findViewById(R.id.btnCancelPayment);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();

        // Nhận id từ Intent
        String paymentId = getIntent().getStringExtra("id");
        if (paymentId == null) {
            Toast.makeText(this, "Không có ID thanh toán", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCancelPayment.setOnClickListener(v -> updatePaymentStatus("cancelled"));
        btnConfirmPayment.setOnClickListener(v -> updatePaymentStatus("success"));

        // 🔹 Lấy dữ liệu từ Firestore
        db.collection("payments").document(paymentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            bindPaymentData(payment, doc);
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu thanh toán", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void bindPaymentData(Payment payment, DocumentSnapshot doc) {
        // Format tiền tệ
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat formatVN = NumberFormat.getCurrencyInstance(localeVN);
        String amountFormatted = formatVN.format(payment.getAmount());

        // Hiển thị dữ liệu chính
        txtPaymentId.setText(doc.getId());
        txtAmount.setText(amountFormatted);
        txtBookingId.setText(payment.getBookingId());
        txtMethod.setText(payment.getMethod());
        txtNote.setText(payment.getNote() != null ? payment.getNote() : "(Không có)");
        txtStatus.setText(payment.getStatus());

        // 🔹 Hiển thị tên khách hàng từ bảng "users"
        if (payment.getUserId() != null && !payment.getUserId().isEmpty()) {
            db.collection("users").document(payment.getUserId())
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String firstName = userDoc.getString("firstname");
                            String lastName = userDoc.getString("lastname");
                            String phone = userDoc.getString("phone");
                            String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "") + " - " + (phone != null ? phone : "")).trim();
                            txtUserName.setText(fullName.isEmpty() ? "(Không rõ)" : fullName);
                        } else {
                            txtUserName.setText("(Không tìm thấy người dùng)");
                        }
                    })
                    .addOnFailureListener(e -> {
                        txtUserName.setText("(Lỗi tải dữ liệu)");
                    });
        } else {
            txtUserName.setText("(Không rõ)");
        }

        txtTransactionRef.setText(payment.getTransactionId());

        Timestamp timestamp = payment.getPaymentTime();
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            txtDate.setText(sdf.format(timestamp.toDate()));
        } else {
            txtDate.setText("Không có dữ liệu");
        }

        String status = doc.getString("status");
        if ("success".equals(status) || "cancelled".equals(status)) {
            btnCancelPayment.setEnabled(false);
            btnConfirmPayment.setEnabled(false);
            btnCancelPayment.setAlpha(0.5f);
            btnConfirmPayment.setAlpha(0.5f);
        }

        // 🔹 Trạng thái hoàn tiền
        boolean isRefundChecked = payment.isRefund();
        switchRefund.setChecked(isRefundChecked);
        switchRefund.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("payments").document(doc.getId())
                    .update("refund", isChecked)
                    .addOnSuccessListener(aVoid -> {
                        String msg = isChecked
                                ? "✅ Đã bật hoàn tiền (refund = true)"
                                : "❎ Đã tắt hoàn tiền (refund = false)";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "❌ Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        switchRefund.setChecked(!isChecked);
                    });
        });

        // 🔹 Hiển thị thông tin hoàn tiền nếu có
        db.collection("payments").document(doc.getId()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Payment paymentFromDoc = snapshot.toObject(Payment.class);
                        if (paymentFromDoc != null && paymentFromDoc.getRefund_information() != null) {
                            Payment.RefundInformation refundInfo = paymentFromDoc.getRefund_information();

                            LinearLayout layoutRefundInfo = findViewById(R.id.layoutRefundInfo);

                            layoutRefundInfo.setVisibility(View.VISIBLE);
                            txtRefundInfo.setText(
                                    "👤 Tên tài khoản: " + refundInfo.getAccount_name() + "\n" +
                                            "💳 Số tài khoản: " + refundInfo.getAccount_number() + "\n" +
                                            "🏦 Ngân hàng: " + refundInfo.getBank_name() + "\n" +
                                            "❗ Lý do: " + refundInfo.getReason() + "\n" +
                                            "📘 Trạng thái: " + refundInfo.getStatus()
                            );
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
    private void updatePaymentStatus(String newStatus) {
        String paymentId = getIntent().getStringExtra("id"); // 🔹 dùng đúng key đã truyền từ Intent
        if (paymentId == null || paymentId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", newStatus);
        updateData.put("updatedAt", Timestamp.now());

        db.collection("payments").document(paymentId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    String message;
                    if ("success".equals(newStatus)) {
                        message = "✅ Đã xác nhận thanh toán thành công!";
                        txtStatus.setText("Thành công");
                        txtStatus.setTextColor(getResources().getColor(R.color.success_green));
                    } else {
                        message = "🚫 Đã hủy thanh toán!";
                        txtStatus.setText("Đã hủy");
                        txtStatus.setTextColor(getResources().getColor(R.color.status_cancelled));
                    }

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    // ✅ Khóa 2 nút sau khi cập nhật xong
                    btnCancelPayment.setEnabled(false);
                    btnConfirmPayment.setEnabled(false);
                    btnCancelPayment.setAlpha(0.5f);
                    btnConfirmPayment.setAlpha(0.5f);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
