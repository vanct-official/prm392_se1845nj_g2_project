package com.example.finalproject.adapter.customer;

import android.content.Context;
import android.content.Intent;
import android.util.Log; // Import để ghi log lỗi
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
import com.example.finalproject.activity.customer.CustomerTourDetailActivity; // Import Activity chi tiết
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

    // Interface để xử lý sự kiện khi người dùng click vào một tour (để mở chi tiết)
    public interface OnTourClickListener {
        void onTourClick(DocumentSnapshot tour);
    }

    private Context context;
    private List<DocumentSnapshot> tours;
    private OnTourClickListener listener;
    private Set<String> wishlistedTourIds;
    // Đối tượng để format ngày tháng
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public CustomerTourAdapter(Context context, List<DocumentSnapshot> tours, Set<String> wishlistedTourIds, OnTourClickListener listener) {
        this.context = context;
        this.tours = tours;
        this.wishlistedTourIds = wishlistedTourIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item_tour_customer.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_tour_customer, parent, false);
        return new TourViewHolder(view); // Trả về ViewHolder mới được tạo
    }

    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        // Lấy document tour
        DocumentSnapshot doc = tours.get(position);

        // Lấy danh sách hướng dẫn viên
        List<String> guideIds = (List<String>) doc.get("guideIds");
        String status = doc.getString("status");

        // Lọc tour: chỉ hiển thị nếu status = "upcoming" và có ít nhất 1 hướng dẫn viên
        if (!"upcoming".equalsIgnoreCase(status) || guideIds == null || guideIds.isEmpty()) {
            // Ẩn item không hợp lệ
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        } else {
            // Hiển thị item hợp lệ
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        // Lấy ID và dữ liệu tour
        String tourId = doc.getId();
        String title = doc.getString("title");
        String description = doc.getString("description");
        String destination = doc.getString("destination");
        Double price = doc.getDouble("price");
        Timestamp startDate = doc.getTimestamp("start_date");
        List<String> images = (List<String>) doc.get("images");

        // Gán dữ liệu vào ViewHolder
        holder.tvTourTitle.setText(title);
        holder.tvDescription.setText(description);
        holder.tvDestination.setText(destination);
        holder.tvPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price != null ? price : 0));
        if (startDate != null) {
            holder.tvStartDate.setText(sdf.format(startDate.toDate()));
        } else {
            holder.tvStartDate.setText("Chưa xác định");
        }

        // Nạp ảnh vào ImageSlider
        List<SlideModel> slideModels = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (String url : images) {
                slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
            }
        } else {
            slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
        }
        holder.imageSlider.setImageList(slideModels);

        // Hiển thị tên hướng dẫn viên
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
                        if (!guideNames.isEmpty()) {
                            holder.tvGuides.setText(String.join(", ", guideNames));
                        } else {
                            holder.tvGuides.setText("Chưa có");
                        }
                    })
                    .addOnFailureListener(e -> holder.tvGuides.setText("Lỗi tải HDV"));
        } else {
            holder.tvGuides.setText("Chưa có");
        }

        // Xử lý wishlist
        boolean isWishlisted = wishlistedTourIds.contains(tourId);
        holder.updateWishlistIcon(isWishlisted);
        holder.ivFavorite.setOnClickListener(v -> {
            boolean currentlyWishlisted = wishlistedTourIds.contains(tourId);
            holder.updateWishlistIcon(!currentlyWishlisted);
            if (currentlyWishlisted) {
                removeFromWishlist(tourId, holder, position);
            } else {
                addToWishlist(tourId, holder, position);
            }
        });

        // Click mở chi tiết
        holder.btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(context, CustomerTourDetailActivity.class);
            intent.putExtra("tourId", tourId);
            context.startActivity(intent);
        });
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CustomerTourDetailActivity.class);
            intent.putExtra("tourId", tourId);
            context.startActivity(intent);
        });

        // Click đặt tour
        holder.btnBook.setOnClickListener(v ->
                Toast.makeText(context, "Chức năng Đặt tour cho: " + title, Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Hàm thêm một tour vào danh sách yêu thích trên Firebase.
     * @param tourId ID của tour cần thêm.
     * @param holder ViewHolder của item để hoàn tác nếu có lỗi.
     * @param position Vị trí của item trong adapter để cập nhật hiệu quả.
     */
    private void addToWishlist(String tourId, TourViewHolder holder, int position) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
            holder.updateWishlistIcon(false); // Hoàn tác
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> wishlistItem = new HashMap<>();
        wishlistItem.put("tourId", tourId);
        wishlistItem.put("userId", userId);

        db.collection("wishlists").add(wishlistItem)
                .addOnSuccessListener(documentReference -> {
                    wishlistedTourIds.add(tourId);
                    Toast.makeText(context, "Đã thêm vào yêu thích!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Log.e("WishlistError", "Lỗi khi thêm vào wishlist", e); // Đã xóa Log
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    holder.updateWishlistIcon(false); // Hoàn tác
                });
    }

    /**
     * Hàm xóa một tour khỏi danh sách yêu thích trên Firebase.
     * @param tourId ID của tour cần xóa.
     * @param holder ViewHolder của item để hoàn tác nếu có lỗi.
     * @param position Vị trí của item trong adapter để cập nhật hiệu quả.
     */
    private void removeFromWishlist(String tourId, TourViewHolder holder, int position) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) {
            holder.updateWishlistIcon(true); // Hoàn tác
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("wishlists")
                .whereEqualTo("tourId", tourId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    wishlistedTourIds.remove(tourId);
                                    Toast.makeText(context, "Đã bỏ yêu thích!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    // Log.e("WishlistError", "Lỗi khi xóa khỏi wishlist", e); // Đã xóa Log
                                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    holder.updateWishlistIcon(true); // Hoàn tác
                                });
                    } else {
                        wishlistedTourIds.remove(tourId);
                    }
                })
                .addOnFailureListener(e -> {
                    // Log.e("WishlistError", "Lỗi khi tìm mục wishlist", e); // Đã xóa Log
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    holder.updateWishlistIcon(true); // Hoàn tác
                });
    }

    @Override
    public int getItemCount() {
        return tours.size();
    }

    /**
     * Lớp ViewHolder để giữ các tham chiếu đến các View của một item.
     */
    static class TourViewHolder extends RecyclerView.ViewHolder {
        ImageSlider imageSlider;
        ImageView ivFavorite;
        TextView tvTourTitle, tvDescription, tvPrice, tvDestination, tvStartDate, tvGuides;
        // ❌ Đã xóa tvRatingList
        Button btnDetails, btnBook;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            imageSlider = itemView.findViewById(R.id.imageSlider);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvTourTitle = itemView.findViewById(R.id.tvTourTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            // ❌ Đã xóa ánh xạ tvRatingList
            tvGuides = itemView.findViewById(R.id.tvGuides);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            btnBook = itemView.findViewById(R.id.btnBook);
        }

        public void updateWishlistIcon(boolean isWishlisted) {
            if (isWishlisted) {
                ivFavorite.setImageResource(R.drawable.ic_favorite);
            } else {
                ivFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        }
    }
}