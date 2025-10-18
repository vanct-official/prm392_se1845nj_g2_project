package com.example.finalproject.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.example.finalproject.entity.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.ViewHolder> implements Filterable {

    private Context context;
    private List<Review> reviewList;
    private List<Review> filteredList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public AdminReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
        this.filteredList = new ArrayList<>(reviewList);
    }

    @NonNull
    @Override
    public AdminReviewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminReviewAdapter.ViewHolder holder, int position) {
        Review review = filteredList.get(position);

        // Tên người dùng
        holder.tvUserName.setText(
                !TextUtils.isEmpty(review.getUserName()) ? review.getUserName() : "(Không rõ)"
        );

        // Tên tour
        holder.tvTourName.setText(
                !TextUtils.isEmpty(review.getTourName()) ? review.getTourName() : "(Không rõ tour)"
        );

        // Bình luận
        holder.tvComment.setText(
                !TextUtils.isEmpty(review.getComment()) ? review.getComment() : "(Không có nội dung)"
        );

        // Avatar
        if (!TextUtils.isEmpty(review.getUserAvatar())) {
            Glide.with(context)
                    .load(review.getUserAvatar())
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }

        // Rating — luôn chính xác 4.5, 3.5,...
        if (review.getRating() instanceof Number) {
            float ratingValue = ((Number) review.getRating()).floatValue();
            holder.ratingBar.setRating(ratingValue);
            holder.tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f", ratingValue));
        } else {
            holder.ratingBar.setRating(0);
            holder.tvRatingValue.setText("0.0");
        }

        // Ngày đánh giá
        if (review.getCreatedAt() != null) {
            Date date = review.getCreatedAt().toDate();
            holder.tvDate.setText(dateFormat.format(date));
        } else {
            holder.tvDate.setText("(Không rõ thời gian)");
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // 👉 Hàm filter (dùng cho SearchView)
    public void filter(String text) {
        text = text.toLowerCase().trim();
        filteredList.clear();

        if (text.isEmpty()) {
            filteredList.addAll(reviewList);
        } else {
            for (Review r : reviewList) {
                if ((r.getTourName() != null && r.getTourName().toLowerCase().contains(text)) ||
                        (r.getUserName() != null && r.getUserName().toLowerCase().contains(text))) {
                    filteredList.add(r);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Review> resultList = new ArrayList<>();
                String text = constraint.toString().toLowerCase().trim();

                if (text.isEmpty()) {
                    resultList.addAll(reviewList);
                } else {
                    for (Review r : reviewList) {
                        if ((r.getTourName() != null && r.getTourName().toLowerCase().contains(text)) ||
                                (r.getUserName() != null && r.getUserName().toLowerCase().contains(text))) {
                            resultList.add(r);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = resultList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();
                filteredList.addAll((List<Review>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTourName, tvComment, tvDate, tvRatingValue;
        RatingBar ratingBar;
        ImageView ivUserAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTourName = itemView.findViewById(R.id.tvTourName);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvDate = itemView.findViewById(R.id.tvDate);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvRatingValue = itemView.findViewById(R.id.tvRatingScore);
        }
    }
}
