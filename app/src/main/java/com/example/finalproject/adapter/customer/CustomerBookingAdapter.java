package com.example.finalproject.adapter.customer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.activity.customer.BookingDetailActivity;
import com.example.finalproject.entity.Booking;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Comparator;

public class CustomerBookingAdapter extends RecyclerView.Adapter<CustomerBookingAdapter.BookingViewHolder> {
    private List<Booking> items;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CustomerBookingAdapter(List<Booking> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_booking_demo, parent, false);
        return new BookingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking b = (items == null || position < 0 || position >= items.size()) ? null : items.get(position);
        if (b == null) return;

        Context ctx = holder.itemView.getContext();

        // 🔹 Lấy tour title từ Firestore để hiển thị
        db.collection("tours").document(b.getTourId()).get()
                .addOnSuccessListener(tourDoc -> {
                    if (tourDoc.exists()) {
                        String title = tourDoc.getString("title");
                        holder.tvTourName.setText(title != null ? title : b.getTourId());
                    } else {
                        holder.tvTourName.setText(b.getTourId());
                    }
                })
                .addOnFailureListener(e -> holder.tvTourName.setText(b.getTourId()));

        // 🔹 Ngày tạo
        String dateStr = "N/A";
        try {
            if (b.getCreateAt() != null) {
                dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(b.getCreateAt().toDate());
            }
        } catch (Exception ignored) {
        }
        holder.tvDate.setText("🕒 " + dateStr);

        // 🔹 Trạng thái
        String status = b.getStatus() != null ? b.getStatus() : "pending";
        int color;
        switch (status) {
            case "confirmed":
                holder.tvStatus.setText("Đã xác nhận");
                color = ContextCompat.getColor(ctx, R.color.status_confirmed);
                holder.tvStatus.setTextColor(color);
                break;
            case "rejected":
                holder.tvStatus.setText("Đã từ chối");
                color = ContextCompat.getColor(ctx, R.color.status_cancelled);
                holder.tvStatus.setTextColor(color);
                break;
            case "cancelled":
                holder.tvStatus.setText("Đã hủy");
                color = ContextCompat.getColor(ctx, R.color.status_cancelled);
                holder.tvStatus.setTextColor(color);
                break;
            case "successfully":
                holder.tvStatus.setText("Thành công");
                color = ContextCompat.getColor(ctx, R.color.status_confirmed);
                holder.tvStatus.setTextColor(color);
                break;
            case "Ongoing":
                holder.tvStatus.setText("Đang diễn ra");
                color = ContextCompat.getColor(ctx, R.color.status_pending);
                holder.tvStatus.setTextColor(color);
                break;
            default:
                holder.tvStatus.setText("Chờ xử lý");
                color = ContextCompat.getColor(ctx, R.color.status_pending);
                holder.tvStatus.setTextColor(color);
                break;
        }

        holder.ivAvatar.setImageResource(R.drawable.ic_account);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), BookingDetailActivity.class);
            intent.putExtra("bookingId", b.getId());
            v.getContext().startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvTourName, tvDate, tvStatus;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatarCustomer);
            tvTourName = itemView.findViewById(R.id.tvTourNameCustomer);
            tvDate = itemView.findViewById(R.id.tvDateCustomer);
            tvStatus = itemView.findViewById(R.id.tvStatusCustomer);
        }
    }
}
