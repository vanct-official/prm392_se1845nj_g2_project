package com.example.finalproject.activity.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PaymentDetailActivity extends AppCompatActivity {

    private TextView txtPaymentId, txtAmount, txtBookingId, txtMethod, txtNote, txtStatus, txtTransactionRef, txtDate;
    private Button btnRefund;

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
        btnRefund = findViewById(R.id.btnRefund);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentDetailActivity.this, PaymentHistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });


        // Nhận dữ liệu từ Intent
        String id = getIntent().getStringExtra("id");
        double amount = getIntent().getDoubleExtra("amount", 0);
        String bookingId = getIntent().getStringExtra("bookingId");
        String method = getIntent().getStringExtra("method");
        String note = getIntent().getStringExtra("note");
        String status = getIntent().getStringExtra("status");
        String transactionRef = getIntent().getStringExtra("transaction_ref");
        long timestamp = getIntent().getLongExtra("timestamp", 0);
        boolean refund = getIntent().getBooleanExtra("refund", false);

        // Định dạng tiền Việt Nam
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat formatVN = NumberFormat.getCurrencyInstance(localeVN);
        String amountFormatted = formatVN.format(amount);

        // Hiển thị dữ liệu
        txtPaymentId.setText(id);
        txtAmount.setText(amountFormatted);
        txtBookingId.setText(bookingId);
        txtMethod.setText(method);
        txtNote.setText(note);
        txtStatus.setText(status);
        txtTransactionRef.setText(transactionRef);

        if (timestamp > 0) {
            Timestamp ts = new Timestamp(timestamp, 0);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            txtDate.setText(sdf.format(ts.toDate()));
        } else {
            txtDate.setText("Không có dữ liệu");
        }

        // 🔥 Ẩn hoặc hiện nút Refund tùy theo giá trị refund
        if (!refund) {
            btnRefund.setVisibility(android.view.View.GONE);
        } else {
            btnRefund.setVisibility(android.view.View.VISIBLE);
            btnRefund.setOnClickListener(v -> showRefundDialog());
        }
    }

    private void showRefundDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setCancelable(true);

        // Gắn layout custom
        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_refund_form, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Ánh xạ các view trong dialog
        android.widget.EditText edtBankName = dialogView.findViewById(R.id.edtBankName);
        android.widget.EditText edtAccountNumber = dialogView.findViewById(R.id.edtAccountNumber);
        android.widget.EditText edtAccountHolder = dialogView.findViewById(R.id.edtAccountHolder);
        android.widget.EditText edtReason = dialogView.findViewById(R.id.edtReason);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String bankName = edtBankName.getText().toString().trim();
            String accountNumber = edtAccountNumber.getText().toString().trim();
            String accountHolder = edtAccountHolder.getText().toString().trim();
            String reason = edtReason.getText().toString().trim();

            if (bankName.isEmpty() || accountNumber.isEmpty() || accountHolder.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Gửi request hoàn tiền
            android.widget.Toast.makeText(this,
                    "Đã gửi yêu cầu hoàn tiền cho " + accountHolder,
                    android.widget.Toast.LENGTH_LONG).show();

            dialog.dismiss();
        });
    }
}
