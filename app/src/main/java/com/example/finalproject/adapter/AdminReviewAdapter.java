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
import java.util.Date;
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

        // Comment
        String comment = (String) review.get("comment");
        holder.tvComment.setText(comment != null ? comment : "(Không có bình luận)");

        holder.tvTourId.setText("" + review.getOrDefault("tourName", "(Không rõ tour)"));
        holder.tvUserId.setText("" + review.getOrDefault("userName", "(Không rõ user)"));

        // Rating
        Object ratingObj = review.get("rating");
        float rating = 0;
        if (ratingObj instanceof Number) {
            rating = ((Number) ratingObj).floatValue();
        }
        holder.ratingBar.setRating(rating);
        holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", rating));

        // Date - Chỉ xử lý 1 lần
        Object createdAt = review.get("createdAt");
        if (createdAt instanceof com.google.firebase.Timestamp) {
            Date date = ((com.google.firebase.Timestamp) createdAt).toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDate.setText("" + sdf.format(date));
        } else if (createdAt != null) {
            holder.tvDate.setText("" + createdAt.toString());
        } else {
            holder.tvDate.setText("Ngày: (Không rõ)");
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