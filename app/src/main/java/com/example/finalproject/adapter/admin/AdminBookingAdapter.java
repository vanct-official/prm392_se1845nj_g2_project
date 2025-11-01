package com.example.finalproject.adapter.admin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.activity.admin.AdminTourDetailActivity;
import com.example.finalproject.entity.Booking;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.BookingViewHolder> {

    private final List<Booking> items;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Context context;

    // ✅ Cache để tránh query trùng (hiệu năng cao hơn)
    private final Map<String, String> userCache = new HashMap<>();
    private final Map<String, String> tourCache = new HashMap<>();

    public AdminBookingAdapter(List<Booking> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_booking, parent, false);
        return new BookingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking b = items.get(position);

        // 🧩 1. Hiển thị tên khách (user)
        if (userCache.containsKey(b.getUserId())) {
            holder.tvCustomerName.setText("Khách: " + userCache.get(b.getUserId()));
        } else {
            db.collection("users").document(b.getUserId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String first = doc.getString("firstname");
                            String last = doc.getString("lastname");
                            String fullName = (first != null ? first : "") + " " + (last != null ? last : "");
                            userCache.put(b.getUserId(), fullName.trim());
                            holder.tvCustomerName.setText("Khách: " + fullName.trim());
                        } else {
                            holder.tvCustomerName.setText("Khách: N/A");
                        }
                    })
                    .addOnFailureListener(e -> holder.tvCustomerName.setText("Khách: N/A"));
        }

        // 🧩 2. Hiển thị tên tour
        if (tourCache.containsKey(b.getTourId())) {
            holder.tvTourName.setText("Tour: " + tourCache.get(b.getTourId()));
        } else {
            db.collection("tours").document(b.getTourId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String title = doc.getString("title");
                            tourCache.put(b.getTourId(), title != null ? title : b.getTourId());
                            holder.tvTourName.setText("Tour: " + (title != null ? title : b.getTourId()));
                        } else {
                            holder.tvTourName.setText("Tour: " + b.getTourId());
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.tvTourName.setText("Tour: " + b.getTourId());
                        Log.e("BookingAdapter", "Error loading tour title", e);
                    });
        }

        // 🧩 3. Hiển thị thời gian tạo booking
        String dateStr = "N/A";
        try {
            if (b.getCreateAt() != null)
                dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(b.getCreateAt().toDate());
        } catch (Exception ignored) {}
        holder.tvDate.setText("🕒 " + dateStr);

        // 🧩 4. Hiển thị trạng thái
        String status = b.getStatus() != null ? b.getStatus() : "pending";
        String statusLabel;
        int colorRes;

        switch (status) {
            case "confirmed":
                statusLabel = "Trạng thái: Đã xác nhận";
                colorRes = R.color.status_confirmed;
                holder.btnConfirm.setVisibility(View.GONE);
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;
            case "rejected":
                statusLabel = "Trạng thái: Đã từ chối";
                colorRes = R.color.status_cancelled;
                holder.btnConfirm.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.GONE);
                break;
            default:
                statusLabel = "Trạng thái: Chờ xử lý";
                colorRes = R.color.status_pending;
                holder.btnConfirm.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;
        }

        holder.tvStatus.setText(statusLabel);
        holder.tvStatus.setTextColor(holder.itemView.getResources().getColor(colorRes, null));
        holder.ivAvatar.setImageResource(R.drawable.ic_account);

        // 🧩 5. Cập nhật trạng thái
        holder.btnConfirm.setOnClickListener(v -> updateStatus(b.getId(), "confirmed", position));
        holder.btnCancel.setOnClickListener(v -> updateStatus(b.getId(), "rejected", position));

        // 🧩 6. Nút Chi tiết
        holder.btnDetail.setOnClickListener(v -> {
            String tourId = b.getTourId();
            if (tourId == null || tourId.trim().isEmpty()) {
                Toast.makeText(v.getContext(), "Booking này chưa có mã tour hợp lệ", Toast.LENGTH_SHORT).show();
                Log.e("BookingDebug", "tourId null hoặc rỗng cho bookingId: " + b.getId());
                return;
            }

            Intent intent = new Intent(v.getContext(), AdminTourDetailActivity.class);
            intent.putExtra("tourId", tourId.trim());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 🧩 Hàm cập nhật trạng thái
    private void updateStatus(String bookingId, String newStatus, int position) {
        DocumentReference ref = db.collection("bookings").document(bookingId);
        ref.update("status", newStatus)
                .addOnSuccessListener(unused -> {
                    items.get(position).setStatus(newStatus);
                    notifyItemChanged(position);
                    Toast.makeText(context, "Đã cập nhật: " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvCustomerName, tvTourName, tvDate, tvStatus;
        Button btnConfirm, btnCancel, btnDetail;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvCustomerName = itemView.findViewById(R.id.tvBookingName);
            tvTourName = itemView.findViewById(R.id.tvBookingTour);
            tvDate = itemView.findViewById(R.id.tvBookingDate);
            tvStatus = itemView.findViewById(R.id.tvBookingStatus);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnDetail = itemView.findViewById(R.id.btnDetail);
        }
    }
}
