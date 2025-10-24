package com.example.finalproject.adapter.customer; // Đảm bảo đúng package

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Import để lấy màu
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerReviewAdapter extends RecyclerView.Adapter<CustomerReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<DocumentSnapshot> reviews;
    private FirebaseFirestore db;
    // Cache để lưu tên user đã tải, tránh query lại
    private Map<String, String> userNamesCache = new HashMap<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public CustomerReviewAdapter(Context context, List<DocumentSnapshot> reviews) {
        this.context = context;
        this.reviews = reviews;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item_customer_review.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        DocumentSnapshot reviewDoc = reviews.get(position);

        // Lấy dữ liệu từ review document
        String comment = reviewDoc.getString("comment");
        Double rating = reviewDoc.getDouble("rating");
        Timestamp createdAt = reviewDoc.getTimestamp("createdAt"); // Hoặc createdEdit/updatedAt tùy field bạn dùng
        String userId = reviewDoc.getString("userId");

        // Hiển thị comment và ngày
        holder.tvReviewComment.setText(comment != null ? comment : "");
        if (createdAt != null) {
            holder.tvReviewDate.setText(sdf.format(createdAt.toDate()));
            holder.tvReviewDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewDate.setVisibility(View.GONE);
        }

        // Hiển thị các ngôi sao đánh giá
        holder.layoutReviewStars.removeAllViews(); // Xóa các sao cũ (quan trọng khi tái sử dụng ViewHolder)
        if (rating != null && rating > 0) {
            int fullStars = rating.intValue();
            // Lấy màu vàng từ resources
            int starColor = ContextCompat.getColor(context, R.color.star_color); // Đảm bảo bạn có màu này trong colors.xml

            for (int i = 0; i < 5; i++) {
                ImageView star = new ImageView(context);
                // Set kích thước và khoảng cách cho sao (có thể điều chỉnh)
                int starSize = context.getResources().getDimensionPixelSize(R.dimen.star_size_small); // Cần định nghĩa dimens
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(starSize, starSize);
                if (i < 4) { // Không thêm margin cho sao cuối
                    params.setMarginEnd(context.getResources().getDimensionPixelSize(R.dimen.star_margin_small)); // Cần định nghĩa dimens
                }
                star.setLayoutParams(params);

                // Chọn icon sao đầy hoặc rỗng
                if (i < fullStars) {
                    star.setImageResource(R.drawable.ic_star_rate);
                } else {
                    star.setImageResource(R.drawable.ic_star_rate_border);
                }
                // Đặt màu cho sao
                star.setColorFilter(starColor);
                // Thêm sao vào layout
                holder.layoutReviewStars.addView(star);
            }
        }

        // Tải và hiển thị tên người dùng (có cache)
        if (userId != null) {
            if (userNamesCache.containsKey(userId)) {
                holder.tvUserName.setText(userNamesCache.get(userId));
            } else {
                holder.tvUserName.setText("Đang tải...");
                db.collection("users").document(userId).get()
                        .addOnSuccessListener(userDoc -> {
                            if (userDoc != null && userDoc.exists()) {
                                String firstName = userDoc.getString("firstname");
                                String lastName = userDoc.getString("lastname");
                                String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                                if (!fullName.isEmpty()) {
                                    holder.tvUserName.setText(fullName);
                                    userNamesCache.put(userId, fullName); // Lưu vào cache
                                } else {
                                    holder.tvUserName.setText("Người dùng"); // Tên mặc định nếu không có
                                }
                            } else {
                                holder.tvUserName.setText("Người dùng không tồn tại");
                            }
                        })
                        .addOnFailureListener(e -> holder.tvUserName.setText("Lỗi tải tên"));
            }
        } else {
            holder.tvUserName.setText("Người dùng ẩn danh");
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName, tvReviewComment, tvReviewDate;
        LinearLayout layoutReviewStars;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            layoutReviewStars = itemView.findViewById(R.id.layoutReviewStars);
        }
    }
}