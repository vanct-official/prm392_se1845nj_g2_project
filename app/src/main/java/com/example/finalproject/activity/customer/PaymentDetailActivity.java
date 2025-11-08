package com.example.finalproject.activity.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentDetailActivity extends AppCompatActivity {

    private TextView txtPaymentId, txtAmount, txtBookingId, txtMethod, txtNote,
            txtStatus, txtTransactionRef, txtDate, txtRefundStatus, txtRefundInfo;
    private Button btnRefund;
    private FirebaseFirestore db;
    private String paymentId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_detail);

        // Ánh xạ view
        txtPaymentId = findViewById(R.id.txtPaymentId);
        txtAmount = findViewById(R.id.txtAmount);
        txtBookingId = findViewById(R.id.txtBookingId);
        txtMethod = findViewById(R.id.txtMethod);
        txtNote = findViewById(R.id.txtNote);
        txtStatus = findViewById(R.id.txtStatus);
        txtTransactionRef = findViewById(R.id.txtTransactionRef);
        txtDate = findViewById(R.id.txtDate);
        txtRefundStatus = findViewById(R.id.txtRefundStatus);
        txtRefundInfo = findViewById(R.id.txtRefundInfo);
        btnRefund = findViewById(R.id.btnRefund);
        ImageView btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();

        // Nhận dữ liệu từ Intent
        paymentId = getIntent().getStringExtra("id");
        double amount = getIntent().getDoubleExtra("amount", 0);
        String bookingId = getIntent().getStringExtra("bookingId");
        String method = getIntent().getStringExtra("method");
        String note = getIntent().getStringExtra("note");
        String status = getIntent().getStringExtra("status");
        String transactionRef = getIntent().getStringExtra("transaction_ref");
        // ✅ Nhận paymentTime dưới dạng millis
        long paymentTimeMillis = getIntent().getLongExtra("paymentTimeMillis", 0);
        Timestamp paymentTime = null;
        if (paymentTimeMillis > 0) {
            paymentTime = new Timestamp(new java.util.Date(paymentTimeMillis));
        }

        boolean refund = getIntent().getBooleanExtra("refund", false);

        // Định dạng tiền tệ Việt Nam
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat formatVN = NumberFormat.getCurrencyInstance(localeVN);
        txtAmount.setText(formatVN.format(amount));

        // Hiển thị thông tin thanh toán
        txtPaymentId.setText(paymentId != null ? paymentId : "N/A");
        txtBookingId.setText(bookingId != null ? bookingId : "N/A");
        txtMethod.setText(method != null ? method : "N/A");
        txtNote.setText(note != null ? note : "Không có ghi chú");
        txtStatus.setText(status != null ? status : "Không xác định");
        txtTransactionRef.setText(transactionRef != null ? transactionRef : "Không có mã giao dịch");

        if (paymentTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            txtDate.setText(sdf.format(paymentTime.toDate()));
        } else {
            txtDate.setText("Không có dữ liệu");
        }

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentDetailActivity.this, PaymentHistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Load refund information
        loadRefundInfo(paymentId, refund);
    }

    private void loadRefundInfo(String paymentId, boolean refundEnabled) {
        db.collection("payments").document(paymentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Boolean refundRequested = doc.getBoolean("refundRequested");
                    Map<String, Object> refundInfo = (Map<String, Object>) doc.get("refund_information");

                    if (refundInfo != null) {
                        String status = (String) refundInfo.get("status");
                        String bankName = (String) refundInfo.get("bank_name");
                        String accountNumber = (String) refundInfo.get("account_number");
                        String accountName = (String) refundInfo.get("account_name");
                        String reason = (String) refundInfo.get("reason");

                        txtRefundStatus.setText(status != null ? status : "Đang xử lý");
                        txtRefundInfo.setText(
                                "Ngân hàng: " + bankName + "\n" +
                                        "Số TK: " + accountNumber + "\n" +
                                        "Chủ TK: " + accountName + "\n" +
                                        "Lý do: " + (reason != null ? reason : "Không có")
                        );
                        txtRefundInfo.setVisibility(View.VISIBLE);

                        if ("pending".equals(status)) {
                            btnRefund.setText("Chỉnh sửa thông tin hoàn tiền");
                            btnRefund.setVisibility(View.VISIBLE);
                            btnRefund.setOnClickListener(v ->
                                    showRefundDialog(paymentId, bankName, accountNumber, accountName, reason)
                            );
                        } else {
                            btnRefund.setVisibility(View.GONE);
                        }

                    } else if (refundEnabled) {
                        txtRefundStatus.setText("Chưa có yêu cầu hoàn tiền");
                        txtRefundInfo.setVisibility(View.GONE);
                        btnRefund.setVisibility(View.VISIBLE);
                        btnRefund.setText("Gửi yêu cầu hoàn tiền");
                        btnRefund.setOnClickListener(v ->
                                showRefundDialog(paymentId, null, null, null, null)
                        );
                    } else {
                        txtRefundStatus.setText("Không áp dụng hoàn tiền");
                        txtRefundInfo.setVisibility(View.GONE);
                        btnRefund.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showRefundDialog(String paymentId, String bankNameOld, String accNumOld, String accHolderOld, String reasonOld) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setCancelable(true);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_refund_form, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        EditText edtBankName = dialogView.findViewById(R.id.edtBankName);
        EditText edtAccountNumber = dialogView.findViewById(R.id.edtAccountNumber);
        EditText edtAccountHolder = dialogView.findViewById(R.id.edtAccountHolder);
        EditText edtReason = dialogView.findViewById(R.id.edtReason);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        if (bankNameOld != null) edtBankName.setText(bankNameOld);
        if (accNumOld != null) edtAccountNumber.setText(accNumOld);
        if (accHolderOld != null) edtAccountHolder.setText(accHolderOld);
        if (reasonOld != null) edtReason.setText(reasonOld);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String bankName = edtBankName.getText().toString().trim();
            String accountNumber = edtAccountNumber.getText().toString().trim();
            String accountHolder = edtAccountHolder.getText().toString().trim();
            String reason = edtReason.getText().toString().trim();

            if (bankName.isEmpty() || accountNumber.isEmpty() || accountHolder.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> refundInfo = new HashMap<>();
            refundInfo.put("bank_name", bankName);
            refundInfo.put("account_number", accountNumber);
            refundInfo.put("account_name", accountHolder);
            refundInfo.put("reason", reason);
            refundInfo.put("status", "pending");
            refundInfo.put("requestTime", Timestamp.now());

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("refundRequested", true);
            updateData.put("refund", true);
            updateData.put("refund_information", refundInfo);

            db.collection("payments").document(paymentId)
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Gửi yêu cầu hoàn tiền thành công!", Toast.LENGTH_LONG).show();
                        txtRefundStatus.setText("pending");
                        loadRefundInfo(paymentId, true);
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi khi gửi yêu cầu: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });
    }
}
