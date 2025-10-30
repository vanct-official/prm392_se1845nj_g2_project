package com.example.finalproject.adapter.admin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalproject.R;
import com.example.finalproject.activity.admin.AdminPaymentDetailActivity;
import com.example.finalproject.entity.Payment;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map; // ✅ thêm dòng này

public class AdminPaymentAdapter extends RecyclerView.Adapter<AdminPaymentAdapter.ViewHolder> {

    private List<Payment> paymentList;
    private Map<String, String> userMap; // ✅ nhớ khai báo biến này
    private Context context;

    public AdminPaymentAdapter(Context context, List<Payment> paymentList, Map<String, String> userMap) {
        this.context = context;
        this.paymentList = paymentList;
        this.userMap = userMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_payment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Payment payment = paymentList.get(position);
        String userName = userMap.get(payment.getUserId());

        // Định dạng tiền tệ Việt Nam
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyVN = NumberFormat.getCurrencyInstance(localeVN);

        // Định dạng ngày giờ
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.txtRef.setText("Ref: " + payment.getTransactionId());
        holder.txtAmount.setText("Số tiền: " + currencyVN.format(payment.getAmount()));
        holder.txtUserFullname.setText("Tên khách hàng: " + userMap.getOrDefault(payment.getUserId(), "Không rõ"));
        holder.txtMethod.setText("Phương thức: " + payment.getMethod());
        holder.txtDate.setText("Ngày: " + dateFormat.format(payment.getPaymentTime().toDate()));

        if (payment.getStatus().equalsIgnoreCase("pending")) {
            holder.txtStatus.setText("Đang xử lý");
            holder.txtStatus.setBackgroundResource(R.drawable.status_badge_success);
            holder.txtStatus.setTextColor(Color.parseColor("#FFFFFF")); // Cam
        } else if (payment.getStatus().equalsIgnoreCase("success")) {
            holder.txtStatus.setText("Thành công");
            holder.txtStatus.setBackgroundResource(R.drawable.status_badge_success);
            holder.txtStatus.setTextColor(Color.parseColor("#FFFFFF")); // Xanh lá
        } else if (payment.getStatus().equalsIgnoreCase("cancelled")) {
            holder.txtStatus.setText("Đã hủy");
            holder.txtStatus.setBackgroundResource(R.drawable.status_badge_failed);
            holder.txtStatus.setTextColor(Color.parseColor("#FFFFFF")); // Đỏ
        }


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminPaymentDetailActivity.class);
            intent.putExtra("id", payment.getId());
            intent.putExtra("bookingId", payment.getBookingId());
            intent.putExtra("userName", userName);
            intent.putExtra("method", payment.getMethod());
            intent.putExtra("note", payment.getNote());
            intent.putExtra("status", payment.getStatus());
            intent.putExtra("transaction_ref", payment.getTransactionId());
            intent.putExtra("timestamp", payment.getPaymentTime() != null ? payment.getPaymentTime().getSeconds() : 0);
            intent.putExtra("amount", payment.getAmount());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return paymentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRef, txtAmount, txtUserFullname, txtMethod, txtStatus, txtDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRef = itemView.findViewById(R.id.txtRef);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtUserFullname = itemView.findViewById(R.id.txtUserFullname);
            txtMethod = itemView.findViewById(R.id.txtMethod);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtDate = itemView.findViewById(R.id.txtDate);
        }
    }
}
