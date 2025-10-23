package com.example.finalproject.adapter.guide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.example.finalproject.entity.Tour;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TourCardAdapter extends RecyclerView.Adapter<TourCardAdapter.TourViewHolder> {

    private final Context context;
    private final List<Tour> tourList;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public TourCardAdapter(Context context, List<Tour> tourList) {
        this.context = context;
        this.tourList = tourList;
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tour_upcoming, parent, false);
        return new TourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        Tour tour = tourList.get(position);

        // --- Gán dữ liệu cơ bản ---
        holder.tvTitle.setText(tour.getTitle() != null ? tour.getTitle() : "Chưa có tiêu đề");
        holder.tvDestination.setText("Địa điểm: " + (tour.getDestination() != null ? tour.getDestination() : "Chưa cập nhật"));
        holder.tvStatus.setText("Trạng thái: " + (tour.getStatus() != null ? tour.getStatus() : "Chưa rõ"));

        // --- Ngày khởi hành ---
        Timestamp startDate = tour.getStart_date();
        if (startDate != null) {
            try {
                holder.tvDate.setText("Ngày khởi hành: " + sdf.format(startDate.toDate()));
            } catch (Exception e) {
                holder.tvDate.setText("Ngày khởi hành: Chưa có dữ liệu");
            }
        } else {
            holder.tvDate.setText("Ngày khởi hành: Chưa có dữ liệu");
        }

        // --- Ảnh tour ---
        if (tour.getImages() != null && !tour.getImages().isEmpty()) {
            Glide.with(context)
                    .load(tour.getImages().get(0))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.imgTour);
        } else {
            holder.imgTour.setImageResource(R.drawable.ic_image_placeholder);
        }

        // --- Nút tạo báo cáo ---
        if (tour.getStatus() != null && tour.getStatus().equalsIgnoreCase("completed")) {
            holder.btnReport.setVisibility(View.VISIBLE);
            holder.btnReport.setOnClickListener(v ->
                    Toast.makeText(context, "Tạo báo cáo cho: " + tour.getTitle(), Toast.LENGTH_SHORT).show());
        } else {
            holder.btnReport.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return tourList != null ? tourList.size() : 0;
    }

    public static class TourViewHolder extends RecyclerView.ViewHolder {
        ImageView imgTour;
        TextView tvTitle, tvDestination, tvDate, tvStatus;
        MaterialButton btnReport;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            imgTour = itemView.findViewById(R.id.imgTour);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnReport = itemView.findViewById(R.id.btnReport);
        }
    }
}
