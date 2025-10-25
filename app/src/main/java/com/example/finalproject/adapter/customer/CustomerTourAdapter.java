package com.example.finalproject.adapter.customer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.finalproject.R;
import com.example.finalproject.activity.customer.CustomerTourDetailActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CustomerTourAdapter extends RecyclerView.Adapter<CustomerTourAdapter.TourViewHolder> {

    // ✅ Interface callback để xử lý sự kiện click tour (nếu cần từ Activity)
    public interface OnTourClickListener {
        void onTourClick(DocumentSnapshot tour);
    }

    private Context context;
    private List<DocumentSnapshot> tours;
    private Set<String> wishlistedTourIds;
    private OnTourClickListener listener; // ✅ thêm biến listener
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ✅ Constructor 3 tham số (cũ - vẫn giữ để tương thích)
    public CustomerTourAdapter(Context context, List<DocumentSnapshot> tours, Set<String> wishlistedTourIds) {
        this.context = context;
        this.tours = tours;
        this.wishlistedTourIds = wishlistedTourIds;
    }

    // ✅ Constructor 4 tham số (mới - để dùng với FavoriteToursActivity)
    public CustomerTourAdapter(Context context, List<DocumentSnapshot> tours, Set<String> wishlistedTourIds, OnTourClickListener listener) {
        this.context = context;
        this.tours = tours;
        this.wishlistedTourIds = wishlistedTourIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tour_customer, parent, false);
        return new TourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        DocumentSnapshot doc = tours.get(position);

        String tourId = doc.getId();
        String title = doc.getString("title");
        String description = doc.getString("description");
        String destination = doc.getString("destination");
        String status = doc.getString("status");
        Double price = doc.getDouble("price");
        Timestamp startDate = doc.getTimestamp("start_date");
        List<String> images = (List<String>) doc.get("images");
        List<String> guideIds = (List<String>) doc.get("guideIds");

        // Gán dữ liệu cơ bản
        holder.tvTourTitle.setText(title != null ? title : "Không có tiêu đề");
        holder.tvDescription.setText(description != null ? description : "Không có mô tả");
        holder.tvDestination.setText(destination != null ? destination : "Không có điểm đến");
        holder.tvPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN"))
                .format(price != null ? price : 0));
        holder.tvStartDate.setText(startDate != null ? sdf.format(startDate.toDate()) : "Chưa xác định");

        // Nạp ảnh
        List<SlideModel> slideModels = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (String url : images) {
                slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
            }
        } else {
            slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
        }
        holder.imageSlider.setImageList(slideModels);

        // Hiển thị hướng dẫn viên
        if (guideIds != null && !guideIds.isEmpty()) {
            holder.tvGuides.setText("Đang tải...");
            FirebaseFirestore.getInstance().collection("users")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), guideIds)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<String> guideNames = new ArrayList<>();
                        for (DocumentSnapshot guideDoc : querySnapshot.getDocuments()) {
                            String firstName = guideDoc.getString("firstname");
                            String lastName = guideDoc.getString("lastname");
                            String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                            if (!fullName.isEmpty()) guideNames.add(fullName);
                        }
                        holder.tvGuides.setText(!guideNames.isEmpty() ? String.join(", ", guideNames) : "Chưa có HDV");
                    })
                    .addOnFailureListener(e -> holder.tvGuides.setText("Lỗi tải HDV"));
        } else {
            holder.tvGuides.setText("Chưa có HDV");
        }

        // ✅ Lấy điểm đánh giá trung bình từ bảng reviews
        FirebaseFirestore.getInstance().collection("reviews")
                .whereEqualTo("tourId", tourId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        holder.tvRating.setText("⭐ Chưa có đánh giá");
                    } else {
                        double total = 0;
                        int count = 0;
                        for (DocumentSnapshot reviewDoc : querySnapshot.getDocuments()) {
                            Double rating = reviewDoc.getDouble("rating");
                            if (rating != null) {
                                total += rating;
                                count++;
                            }
                        }
                        double avg = count > 0 ? total / count : 0;
                        holder.tvRating.setText(String.format(Locale.getDefault(),
                                "⭐ %.1f (%d đánh giá)", avg, count));
                    }
                })
                .addOnFailureListener(e -> holder.tvRating.setText("⭐ Lỗi tải đánh giá"));

        // Ẩn nút "Đặt tour"
        if ("completed".equalsIgnoreCase(status)
                || "cancelled".equalsIgnoreCase(status)
                || guideIds == null || guideIds.isEmpty()) {
            holder.btnBook.setVisibility(View.GONE);
        } else {
            holder.btnBook.setVisibility(View.VISIBLE);
        }

        // ✅ Click mở chi tiết (gọi listener nếu có)
        View.OnClickListener openDetail = v -> {
            if (listener != null) {
                listener.onTourClick(doc);
            } else {
                Intent intent = new Intent(context, CustomerTourDetailActivity.class);
                intent.putExtra("tourId", tourId);
                context.startActivity(intent);
            }
        };
        holder.itemView.setOnClickListener(openDetail);
        holder.btnDetails.setOnClickListener(openDetail);

        // ✅ Wishlist
        boolean isWishlisted = wishlistedTourIds.contains(tourId);
        holder.updateWishlistIcon(isWishlisted);
        holder.ivFavorite.setOnClickListener(v -> {
            boolean currentlyWishlisted = wishlistedTourIds.contains(tourId);
            holder.updateWishlistIcon(!currentlyWishlisted);
            if (currentlyWishlisted) {
                removeFromWishlist(tourId, holder);
            } else {
                addToWishlist(tourId, holder);
            }
        });
    }

    private void addToWishlist(String tourId, TourViewHolder holder) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            holder.updateWishlistIcon(false);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> wishlistItem = new HashMap<>();
        wishlistItem.put("tourId", tourId);
        wishlistItem.put("userId", userId);
        db.collection("wishlists").add(wishlistItem)
                .addOnSuccessListener(ref -> {
                    wishlistedTourIds.add(tourId);
                    Toast.makeText(context, "Đã thêm yêu thích", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> holder.updateWishlistIcon(false));
    }

    private void removeFromWishlist(String tourId, TourViewHolder holder) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("wishlists")
                .whereEqualTo("tourId", tourId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        qs.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(a -> wishlistedTourIds.remove(tourId));
                    }
                });
    }

    @Override
    public int getItemCount() {
        return tours.size();
    }

    static class TourViewHolder extends RecyclerView.ViewHolder {
        ImageSlider imageSlider;
        ImageView ivFavorite;
        TextView tvTourTitle, tvDescription, tvPrice, tvDestination, tvStartDate, tvGuides, tvRating;
        Button btnDetails, btnBook;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            imageSlider = itemView.findViewById(R.id.imageSlider);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvTourTitle = itemView.findViewById(R.id.tvTourTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvGuides = itemView.findViewById(R.id.tvGuides);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            btnBook = itemView.findViewById(R.id.btnBook);
        }

        public void updateWishlistIcon(boolean isWishlisted) {
            ivFavorite.setImageResource(isWishlisted ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        }
    }
}
