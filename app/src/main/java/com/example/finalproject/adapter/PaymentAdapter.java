package com.example.finalproject.adapter;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.entity.Payment;
import com.example.finalproject.activity.customer.PaymentDetailActivity;
import com.google.firebase.Timestamp;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {
    private List<Payment> payments;
    private final Locale localeVN = new Locale("vi", "VN");
    private final NumberFormat currencyVN = NumberFormat.getCurrencyInstance(localeVN);

    public PaymentAdapter(List<Payment> payments) {
        this.payments = payments;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = payments.get(position);

        holder.txtRef.setText("Ref: " + payment.getTransactionId());
        holder.txtAmount.setText("Số tiền: " + currencyVN.format(payment.getAmount()));
        holder.txtMethod.setText("Phương thức: " + payment.getMethod());

        // Đặt màu và trạng thái
        if (payment.getStatus().equalsIgnoreCase("pending")) {
            holder.txtStatus.setText("Đang xử lý");
            holder.txtStatus.setBackgroundResource(R.drawable.status_badge_pending);
            holder.txtStatus.setTextColor(Color.WHITE);
        } else if (payment.getStatus().equalsIgnoreCase("success")) {
            holder.txtStatus.setText("Thành công");
            holder.txtStatus.setBackgroundResource(R.drawable.status_badge_success);
            holder.txtStatus.setTextColor(Color.WHITE);
        } else if (payment.getStatus().equalsIgnoreCase("cancelled")) {
            holder.txtStatus.setText("Đã hủy");
            holder.txtStatus.setBackgroundResource(R.drawable.status_badge_failed);
            holder.txtStatus.setTextColor(Color.WHITE);
        }

        // Ngày thanh toán
        Timestamp ts = payment.getPaymentTime();
        if (ts != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.txtTime.setText("Ngày giao dịch: " + sdf.format(ts.toDate()));
        }

        // 👉 Bắt sự kiện click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PaymentDetailActivity.class);
            intent.putExtra("id", payment.getId());
            intent.putExtra("amount", payment.getAmount());
            intent.putExtra("bookingId", payment.getBookingId());
            intent.putExtra("method", payment.getMethod());
            intent.putExtra("note", payment.getNote());
            intent.putExtra("status", payment.getStatus());
            intent.putExtra("transaction_ref", payment.getTransactionId());
            intent.putExtra("userId", payment.getUserId());
            intent.putExtra("paymentTimeMillis",
                    payment.getPaymentTime() != null ? payment.getPaymentTime().toDate().getTime() : 0L);
            intent.putExtra("refund", payment.isRefund()); // ✅ dòng này rất quan trọng
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return payments.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView txtRef, txtAmount, txtMethod, txtStatus, txtTime;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRef = itemView.findViewById(R.id.txtRef);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtMethod = itemView.findViewById(R.id.txtMethod);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}
