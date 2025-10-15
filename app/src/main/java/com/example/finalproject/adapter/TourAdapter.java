package com.example.finalproject.adapter;

import android.content.Context;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TourAdapter extends RecyclerView.Adapter<TourAdapter.TourViewHolder> {

    public interface OnTourActionListener {
        void onEdit(DocumentSnapshot doc);
        void onView(DocumentSnapshot doc);
        void onDelete(DocumentSnapshot doc);
    }

    private final Context context;
    private final List<DocumentSnapshot> tours;
    private final OnTourActionListener listener;

    public TourAdapter(Context context, List<DocumentSnapshot> tours, OnTourActionListener listener) {
        this.context = context;
        this.tours = tours;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tour_card, parent, false);
        return new TourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        DocumentSnapshot doc = tours.get(position);

        String name = doc.getString("tourName");
        String desc = doc.getString("description");
        String location = doc.getString("location");
        Double price = doc.getDouble("price");
        List<String> images = (List<String>) doc.get("images");

        holder.tvTourName.setText(name != null ? name : "Không rõ tên");
        holder.tvDescription.setText(desc != null ? desc : "");
        holder.tvLocation.setText(location != null ? location : "Chưa xác định");
        holder.tvPrice.setText(price != null
                ? "Giá: " + NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price)
                : "");

        // Hiển thị ảnh
        List<SlideModel> slideModels = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (String url : images) {
                slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
            }
        } else {
            slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
        }

        holder.imageSlider.setImageList(slideModels, ScaleTypes.CENTER_CROP);

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(doc));
        holder.btnView.setOnClickListener(v -> listener.onView(doc));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(doc));
    }

    @Override
    public int getItemCount() {
        return tours.size();
    }

    public static class TourViewHolder extends RecyclerView.ViewHolder {
        ImageSlider imageSlider;
        TextView tvTourName, tvDescription, tvLocation, tvPrice;
        MaterialButton btnEdit, btnView, btnDelete;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            imageSlider = itemView.findViewById(R.id.imageSlider);
            tvTourName = itemView.findViewById(R.id.tvTourName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnView = itemView.findViewById(R.id.btnView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
