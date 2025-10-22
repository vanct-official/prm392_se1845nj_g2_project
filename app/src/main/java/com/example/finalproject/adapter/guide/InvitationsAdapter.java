package com.example.finalproject.adapter.guide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class InvitationsAdapter extends RecyclerView.Adapter<InvitationsAdapter.InviteVH> {

    private final List<DocumentSnapshot> invitations;
    private final OnRespond listener;

    public interface OnRespond {
        void onRespond(String invitationId, String tourId, boolean accepted);
    }

    public InvitationsAdapter(List<DocumentSnapshot> invitations, OnRespond listener) {
        this.invitations = invitations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InviteVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new InviteVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invitation, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull InviteVH h, int pos) {
        DocumentSnapshot doc = invitations.get(pos);
        String tourId = doc.getString("tourId");

        // 🔹 Load tour info từ Firestore
        FirebaseFirestore.getInstance().collection("tours")
                .document(tourId)
                .get()
                .addOnSuccessListener(tour -> {
                    if (tour.exists()) {
                        String title = tour.getString("title");
                        String destination = tour.getString("destination");

                        Object startDateObj = tour.get("start_date");
                        Object endDateObj = tour.get("end_date");

                        String startDateText = "—";
                        String endDateText = "—";

                        if (startDateObj instanceof com.google.firebase.Timestamp) {
                            java.util.Date date = ((com.google.firebase.Timestamp) startDateObj).toDate();
                            startDateText = new java.text.SimpleDateFormat("dd/MM/yyyy").format(date);
                        } else if (startDateObj instanceof String) {
                            startDateText = (String) startDateObj;
                        }

                        if (endDateObj instanceof com.google.firebase.Timestamp) {
                            java.util.Date date = ((com.google.firebase.Timestamp) endDateObj).toDate();
                            endDateText = new java.text.SimpleDateFormat("dd/MM/yyyy").format(date);
                        } else if (endDateObj instanceof String) {
                            endDateText = (String) endDateObj;
                        }

                        List<String> images = (List<String>) tour.get("images");

                        h.tvTourTitle.setText("Tên tour: " + (title != null ? title : "Tour không xác định"));
                        h.tvDestination.setText("Điểm đến: " + (destination != null ? destination : "Chưa rõ"));
                        h.tvStartDate.setText("Ngày bắt đầu: " + startDateText);
                        h.tvEndDate.setText("Ngày kết thúc: " + endDateText);

                        if (images != null && !images.isEmpty()) {
                            h.rvTourImages.setLayoutManager(
                                    new LinearLayoutManager(h.itemView.getContext(),
                                            LinearLayoutManager.HORIZONTAL, false)
                            );
                            h.rvTourImages.setAdapter(new TourImageAdapter(images));
                        }
                    }
                });

        //dịch trạng thái sang tiếng việt
        String status = doc.getString("status");
        String statusText;
        int statusColor;

        if ("pending".equals(status)) {
            statusText = "Đang chờ xác nhận";
            statusColor = 0xFFF59E0B; // cam
        } else if ("accepted".equals(status)) {
            statusText = "Đã chấp nhận";
            statusColor = 0xFF10B981; // xanh lá
        } else if ("declined".equals(status)) {
            statusText = "Đã từ chối";
            statusColor = 0xFFEF4444; // đỏ
        } else {
            statusText = "Không xác định";
            statusColor = 0xFF6B7280; // xám
        }

        h.tvStatus.setText("Trạng thái: " + statusText);
        h.tvStatus.setTextColor(statusColor);


        h.btnAccept.setOnClickListener(v ->
                listener.onRespond(doc.getId(), tourId, true));
        h.btnDecline.setOnClickListener(v ->
                listener.onRespond(doc.getId(), tourId, false));
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    // ✅ Xóa item khỏi danh sách sau khi đồng ý / từ chối
    public void removeInvitation(String invitationId) {
        for (int i = 0; i < invitations.size(); i++) {
            if (invitations.get(i).getId().equals(invitationId)) {
                invitations.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    // 🔹 ViewHolder chính
    static class InviteVH extends RecyclerView.ViewHolder {
        TextView tvTourTitle, tvStatus, tvDestination, tvStartDate, tvEndDate;
        RecyclerView rvTourImages;
        Button btnAccept, btnDecline;

        InviteVH(View v) {
            super(v);
            tvTourTitle = v.findViewById(R.id.tvTourTitle);
            tvStatus = v.findViewById(R.id.tvStatus);
            rvTourImages = v.findViewById(R.id.rvTourImages);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnDecline = v.findViewById(R.id.btnDecline);
            tvDestination = v.findViewById(R.id.tvDestination);
            tvStartDate = v.findViewById(R.id.tvStartDate);
            tvEndDate = v.findViewById(R.id.tvEndDate);
        }
    }

    // 🔹 Adapter phụ để hiển thị nhiều ảnh
    static class TourImageAdapter extends RecyclerView.Adapter<TourImageAdapter.ImageVH> {
        private final List<String> images;

        TourImageAdapter(List<String> images) {
            this.images = images != null ? images : new ArrayList<>();
        }

        @NonNull
        @Override
        public ImageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tour_image_small, parent, false);
            return new ImageVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageVH h, int pos) {
            String url = images.get(pos);
            Glide.with(h.itemView.getContext())
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.ic_tour_placeholder)
                    .into(h.ivImage);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ImageVH extends RecyclerView.ViewHolder {
            ImageView ivImage;
            ImageVH(View v) {
                super(v);
                ivImage = v.findViewById(R.id.ivImage);
            }
        }
    }
}
