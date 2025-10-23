package com.example.finalproject.adapter.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
        String avatarUrl = review.get("avatarUrl") != null ? review.get("avatarUrl").toString() : "";
        double rating = review.get("rating") != null ? Double.parseDouble(review.get("rating").toString()) : 0.0;

        holder.tvComment.setText(comment);
        holder.tvTourName.setText(tourName);
        holder.tvUserName.setText(userName);
        holder.ratingBar.setRating((float) rating);
        holder.tvRatingValue.setText(String.valueOf(rating));

        // Load avatar với Glide
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(avatarUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void filterList(List<Map<String, Object>> filteredList) {
        reviewList.clear();
        reviewList.addAll(filteredList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTourName, tvUserName, tvComment, tvRatingValue;
        RatingBar ratingBar;
        ImageView imgAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTourName = itemView.findViewById(R.id.tvTourName);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvRatingValue = itemView.findViewById(R.id.tvRatingValue);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}