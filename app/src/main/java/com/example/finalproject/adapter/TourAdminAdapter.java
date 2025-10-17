package com.example.finalproject.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.example.finalproject.activity.TourDetailAdminActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.finalproject.activity.EditTourAdminActivity;

public class TourAdminAdapter extends RecyclerView.Adapter<TourAdminAdapter.TourViewHolder> {

    public interface OnTourActionListener {
        void onEdit(DocumentSnapshot doc);
        void onView(DocumentSnapshot doc);
        void onDelete(DocumentSnapshot doc);
    }

    private final Context context;
    private final List<DocumentSnapshot> tours;
    private final OnTourActionListener listener;

    public TourAdminAdapter(Context context, List<DocumentSnapshot> tours, OnTourActionListener listener) {
        this.context = context;
        this.tours = tours;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tour_card_admin, parent, false);
        return new TourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        DocumentSnapshot doc = tours.get(position);

        // 🔹 Đọc đúng field mới
        String title = doc.getString("title");
        String desc = doc.getString("description");
        String destination = doc.getString("destination");
        Double price = doc.getDouble("price");
        String duration = doc.getString("duration");

        List<String> images = (List<String>) doc.get("images");
        List<String> guideIds = (List<String>) doc.get("guideIds");

        // 🔹 Set dữ liệu cơ bản
        holder.tvTourTitle.setText(title != null ? title : "Không có tiêu đề");
        holder.tvDescription.setText(desc != null ? desc : "(Không có mô tả)");
        holder.tvDestination.setText(destination != null ? destination : "Chưa xác định");
        holder.tvDuration.setText(duration != null ? "" + duration : "");

        if (price != null && price > 0) {
            holder.tvPrice.setText("Giá: " + NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price));
        } else {
            holder.tvPrice.setText("Giá: Đang cập nhật");
        }

        // 🔹 Hiển thị danh sách hướng dẫn viên
        if (guideIds != null && !guideIds.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("guides")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), guideIds)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<String> guideNames = new ArrayList<>();
                        for (DocumentSnapshot guideDoc : querySnapshot) {
                            String guideName = guideDoc.getString("name");
                            if (guideName != null) guideNames.add(guideName);
                        }

                        if (!guideNames.isEmpty()) {
                            holder.tvGuides.setText("" + String.join(", ", guideNames));
                        } else {
                            holder.tvGuides.setText("Hướng dẫn viên: (Không rõ)");
                        }
                    })
                    .addOnFailureListener(e ->
                            holder.tvGuides.setText("Hướng dẫn viên: (Lỗi tải)")
                    );
        } else {
            holder.tvGuides.setText("Hướng dẫn viên: (Chưa gán)");
        }

        // 🔹 Hiển thị ảnh
        List<SlideModel> slideModels = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (String url : images) {
                slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
            }
        } else {
            slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
        }
        holder.imageSlider.setImageList(slideModels, ScaleTypes.CENTER_CROP);

        // 🔹 Các nút thao tác
        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TourDetailAdminActivity.class);
            intent.putExtra("tourId", doc.getId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EditTourAdminActivity.class);
            intent.putExtra("tourId", doc.getId());
            v.getContext().startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(doc));
    }

    @Override
    public int getItemCount() {
        return tours.size();
    }

    public static class TourViewHolder extends RecyclerView.ViewHolder {
        ImageSlider imageSlider;
        TextView tvTourTitle, tvDescription, tvDestination, tvDuration, tvGuides, tvPrice;
        MaterialButton btnEdit, btnView, btnDelete;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            imageSlider = itemView.findViewById(R.id.imageSlider);
            tvTourTitle = itemView.findViewById(R.id.tvTourTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvGuides = itemView.findViewById(R.id.tvGuides);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnView = itemView.findViewById(R.id.btnView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
