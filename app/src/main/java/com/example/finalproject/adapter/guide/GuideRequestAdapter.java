package com.example.finalproject.adapter.guide;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.fragment.guide.CustomersInTourFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter hiển thị danh sách tour / lời mời cho hướng dẫn viên
 */
public class GuideRequestAdapter extends RecyclerView.Adapter<GuideRequestAdapter.RequestViewHolder> {

    private final List<DocumentSnapshot> requests;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public GuideRequestAdapter(List<DocumentSnapshot> requests) {
        this.requests = requests;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guide_request, parent, false);
        return new RequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        DocumentSnapshot doc = requests.get(position);
        holder.bind(doc, db);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvTourTitle, tvDestination, tvDate, tvStatus;
        LinearLayout itemLayout;

        RequestViewHolder(View v) {
            super(v);
            tvTourTitle = v.findViewById(R.id.tvTourTitle);
            tvDestination = v.findViewById(R.id.tvDestination);
            tvDate = v.findViewById(R.id.tvDate);
            tvStatus = v.findViewById(R.id.tvStatus);
            itemLayout = v.findViewById(R.id.itemLayout);
        }

        @SuppressLint("SetTextI18n")
        void bind(DocumentSnapshot doc, FirebaseFirestore db) {
            String tourId = doc.getString("tourId");
            String status = doc.getString("status");
            Timestamp createdAt = doc.getTimestamp("createdAt");

            tvStatus.setText("Trạng thái: " + (status != null ? status : "Không rõ"));
            tvDate.setText("Ngày gửi: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(createdAt != null ? createdAt.toDate() : new java.util.Date()));

            // 🔹 Lấy thông tin tour từ Firestore để hiển thị rõ hơn
            db.collection("tours").document(tourId)
                    .get()
                    .addOnSuccessListener(tourDoc -> {
                        if (tourDoc.exists()) {
                            Map<String, Object> tour = tourDoc.getData();
                            tvTourTitle.setText("📍 " + tour.get("title"));
                            Object destination = tour.get("destination");
                            if (destination != null) {
                                tvDestination.setText("Địa điểm: " + destination.toString());
                            } else {
                                tvDestination.setText("Địa điểm: (Không rõ)");
                            }
                        } else {
                            tvTourTitle.setText("Tour ID: " + tourId);
                            tvDestination.setText("Địa điểm: (Không tìm thấy)");
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvTourTitle.setText("Tour ID: " + tourId);
                        tvDestination.setText("Lỗi tải tour");
                    });

            // 🔸 Khi bấm vào tour → mở màn hình CustomersInTourFragment
            itemLayout.setOnClickListener(v -> {
                if (tourId == null || tourId.isEmpty()) return;

                CustomersInTourFragment fragment = new CustomersInTourFragment();
                Bundle bundle = new Bundle();
                bundle.putString("tourId", tourId);
                fragment.setArguments(bundle);

                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container_guide, fragment) // 🟢 đổi ID này theo ID container trong GuideActivity
                        .addToBackStack(null)
                        .commit();
            });
        }
    }
}
