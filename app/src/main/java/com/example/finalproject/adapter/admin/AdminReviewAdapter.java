package com.example.finalproject.adapter.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;

import java.util.List;
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

        String comment = review.get("comment") != null ? review.get("comment").toString() : "(Không có bình luận)";
        String tourName = review.get("tourName") != null ? review.get("tourName").toString() : "(Không có tên tour)";
        String userName = review.get("userName") != null ? review.get("userName").toString() : "(Không có tên user)";
        double rating = review.get("rating") != null ? Double.parseDouble(review.get("rating").toString()) : 0.0;

        holder.tvComment.setText(comment);
        holder.tvTourName.setText(tourName);
        holder.tvUserName.setText(userName);
        holder.ratingBar.setRating((float) rating);
        holder.tvRatingValue.setText(String.valueOf(rating));
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    // ✅ Hàm dùng để cập nhật danh sách sau khi lọc
    public void filterList(List<Map<String, Object>> filteredList) {
        reviewList.clear();
        reviewList.addAll(filteredList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTourName, tvUserName, tvComment, tvRatingValue;
        RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTourName = itemView.findViewById(R.id.tvTourName);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvRatingValue = itemView.findViewById(R.id.tvRatingValue);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
