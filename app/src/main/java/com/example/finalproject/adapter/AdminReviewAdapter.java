package com.example.finalproject.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> reviewList;

    public AdminReviewAdapter(Context context, List<Map<String, Object>> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> review = reviewList.get(position);

        holder.tvComment.setText((String) review.get("comment"));
        holder.tvTourId.setText("Tour ID: " + review.get("tourId"));
        holder.tvUserId.setText("User ID: " + review.get("userId"));

        Object rating = review.get("rating");
        if (rating instanceof Number) {
            holder.ratingBar.setRating(((Number) rating).floatValue());
            holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f ★", ((Number) rating).floatValue()));
        }

        Object createdAt = review.get("createdAt");
        if (createdAt != null) {
            holder.tvDate.setText("Ngày: " + createdAt.toString().replace("UTC+7", ""));
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvComment, tvTourId, tvUserId, tvRating, tvDate;
        RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvTourId = itemView.findViewById(R.id.tvTourId);
            tvUserId = itemView.findViewById(R.id.tvUserId);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDate = itemView.findViewById(R.id.tvDate);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
