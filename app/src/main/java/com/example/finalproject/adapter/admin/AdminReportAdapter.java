package com.example.finalproject.adapter.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.models.SlideModel;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.example.finalproject.R;
import com.example.finalproject.dialog.AdminReportDetailDialog;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> reportList;

    public AdminReportAdapter(Context context, List<Map<String, Object>> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> report = reportList.get(position);

        String tourName = (String) report.getOrDefault("tourName", "(Không rõ tour)");
        String status = (String) report.getOrDefault("status", "pending");
        Number participants = (Number) report.get("participantsCount");
        Number rating = (Number) report.get("ratingFromGuide");

        holder.tvTourName.setText("Tour: " + tourName);
        holder.tvParticipants.setText("👥 " + (participants != null ? participants : 0) + " khách");
        holder.tvRating.setText("⭐ " + (rating != null ? rating : 0));

        // Đổi sang tiếng Việt khi hiển thị
        String statusDisplay;
        switch (status) {
            case "completed":
                statusDisplay = "Hoàn thành";
                break;
            case "pending":
            default:
                statusDisplay = "Chờ xử lý";
                break;
        }
        holder.tvStatus.setText("Trạng thái: " + statusDisplay);

        Object createdAt = report.get("createdAt");
        if (createdAt instanceof Timestamp) {
            Date date = ((Timestamp) createdAt).toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDate.setText("🕒 " + sdf.format(date));
        }

        // 🖼️ Hiển thị nhiều ảnh tour
        List<String> tourImages = (List<String>) report.get("tourImages");
        List<SlideModel> slideModels = new ArrayList<>();
        if (tourImages != null && !tourImages.isEmpty()) {
            for (String imgUrl : tourImages) {
                slideModels.add(new SlideModel(imgUrl, ScaleTypes.CENTER_CROP));
            }
        } else {
            slideModels.add(new SlideModel(R.drawable.bg_image_border, ScaleTypes.CENTER_CROP));
        }

        holder.imageSlider.setImageList(slideModels);

        // Khi admin bấm vào item → mở dialog chi tiết
        holder.itemView.setOnClickListener(v -> new AdminReportDetailDialog(context, report).show());
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTourName, tvParticipants, tvRating, tvDate, tvStatus;
        ImageSlider imageSlider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageSlider = itemView.findViewById(R.id.imageSlider);
            tvTourName = itemView.findViewById(R.id.tvTourName);
            tvParticipants = itemView.findViewById(R.id.tvParticipants);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
