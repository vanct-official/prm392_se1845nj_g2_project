package com.example.finalproject.adapter;

import android.content.Context;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adapter này dành riêng cho việc hiển thị danh sách tour cho khách hàng.
 * Nó xử lý logic cho việc hiển thị thông tin tour và tính năng Yêu thích (Wishlist).
 */
public class CustomerTourAdapter extends RecyclerView.Adapter<CustomerTourAdapter.TourViewHolder> {

    // Interface để xử lý sự kiện khi người dùng click vào một tour (để mở chi tiết)
    public interface OnTourClickListener {
        void onTourClick(DocumentSnapshot tour);
    }

    private Context context; // Context của ứng dụng (để truy cập tài nguyên, hiển thị Toast,...)
    private List<DocumentSnapshot> tours; // Danh sách các document tour từ Firestore
    private OnTourClickListener listener; // Listener để xử lý sự kiện click
    private Set<String> wishlistedTourIds; // Set chứa ID của các tour đã được yêu thích để tra cứu nhanh
    // Đối tượng để format ngày tháng theo định dạng dd/MM/yyyy
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    /**
     * Constructor của Adapter.
     * @param context Context hiện tại.
     * @param tours Danh sách các DocumentSnapshot tour.
     * @param wishlistedTourIds Set chứa ID các tour đã yêu thích.
     * @param listener Listener để xử lý sự kiện click vào tour.
     */
    public CustomerTourAdapter(Context context, List<DocumentSnapshot> tours, Set<String> wishlistedTourIds, OnTourClickListener listener) {
        this.context = context;
        this.tours = tours;
        this.wishlistedTourIds = wishlistedTourIds;
        this.listener = listener;
    }

    /**
     * Được gọi khi RecyclerView cần tạo một ViewHolder mới.
     * Nó nạp layout XML cho một item và trả về một ViewHolder mới.
     */
    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item_tour_customer.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_tour_customer, parent, false);
        return new TourViewHolder(view); // Trả về ViewHolder mới được tạo
    }

    /**
     * Được gọi bởi RecyclerView để hiển thị dữ liệu tại một vị trí cụ thể.
     * Phương thức này cập nhật nội dung của ViewHolder để phản ánh dữ liệu tại vị trí đó.
     */
    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        DocumentSnapshot doc = tours.get(position); // Lấy document tour tại vị trí `position`
        String tourId = doc.getId(); // Lấy ID của tour

        // --- Lấy dữ liệu từ Firestore Document ---
        String title = doc.getString("title");
        String description = doc.getString("description");
        String destination = doc.getString("destination");
        String duration = doc.getString("duration");
        Double price = doc.getDouble("price");
        List<String> images = (List<String>) doc.get("images"); // Danh sách URL ảnh
        List<String> guideIds = (List<String>) doc.get("guideIds"); // Danh sách ID hướng dẫn viên
        Timestamp startDate = doc.getTimestamp("start_date"); // Ngày bắt đầu (kiểu Timestamp)

        // --- Gán dữ liệu vào các View trong ViewHolder ---
        holder.tvTourTitle.setText(title);
        holder.tvDescription.setText(description);
        holder.tvDestination.setText(destination);
        holder.tvDuration.setText(duration);
        // Format giá tiền sang dạng tiền tệ Việt Nam (₫)
        holder.tvPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price != null ? price : 0));

        // Hiển thị ngày bắt đầu đã được format
        if (startDate != null) {
            holder.tvStartDate.setText(sdf.format(startDate.toDate())); // Chuyển Timestamp thành Date rồi format
        } else {
            holder.tvStartDate.setText("Chưa xác định"); // Hiển thị nếu không có ngày
        }

        // --- Nạp danh sách ảnh vào ImageSlider ---
        List<SlideModel> slideModels = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            // Nếu có ảnh, tạo SlideModel cho mỗi URL
            for (String url : images) {
                slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP)); // CENTER_CROP để ảnh vừa khung
            }
        } else {
            // Nếu không có ảnh, hiển thị một ảnh mặc định (placeholder)
            slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
        }
        holder.imageSlider.setImageList(slideModels); // Đặt danh sách ảnh cho slider

        // --- Tải và hiển thị tên hướng dẫn viên ---
        if (guideIds != null && !guideIds.isEmpty()) {
            holder.tvGuides.setText("Đang tải..."); // Hiển thị trạng thái chờ
            // Truy vấn collection "guides" để lấy các document có ID nằm trong danh sách guideIds
            FirebaseFirestore.getInstance().collection("guides").whereIn(com.google.firebase.firestore.FieldPath.documentId(), guideIds)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<String> guideNames = new ArrayList<>();
                        // Duyệt qua kết quả trả về
                        for (DocumentSnapshot guideDoc : querySnapshot.getDocuments()) {
                            String guideName = guideDoc.getString("name"); // Lấy tên từ trường "name"
                            if (guideName != null) guideNames.add(guideName);
                        }
                        // Hiển thị danh sách tên, nối với nhau bằng dấu phẩy
                        if (!guideNames.isEmpty()) {
                            holder.tvGuides.setText(String.join(", ", guideNames));
                        } else {
                            holder.tvGuides.setText("Chưa có"); // Hiển thị nếu không tìm thấy tên
                        }
                    })
                    .addOnFailureListener(e -> holder.tvGuides.setText("Lỗi tải HDV")); // Hiển thị lỗi nếu có
        } else {
            holder.tvGuides.setText("Chưa có"); // Hiển thị nếu tour không có guideIds
        }

        // --- Xử lý logic cho Wishlist (Yêu thích) ---
        boolean isWishlisted = wishlistedTourIds.contains(tourId); // Kiểm tra tour này có trong Set yêu thích không
        holder.updateWishlistIcon(isWishlisted); // Cập nhật icon trái tim dựa trên trạng thái

        // Gán sự kiện click cho icon trái tim
        holder.ivFavorite.setOnClickListener(v -> {
            boolean currentlyWishlisted = wishlistedTourIds.contains(tourId); // Kiểm tra lại trạng thái hiện tại
            // Cập nhật giao diện ngay lập tức để người dùng thấy phản hồi
            holder.updateWishlistIcon(!currentlyWishlisted);
            if (currentlyWishlisted) {
                // Nếu đang yêu thích -> Gọi hàm xóa khỏi wishlist
                removeFromWishlist(tourId, holder, position); // Truyền thêm position
            } else {
                // Nếu chưa yêu thích -> Gọi hàm thêm vào wishlist
                addToWishlist(tourId, holder, position); // Truyền thêm position
            }
        });

        // --- Xử lý sự kiện click cho các nút và toàn bộ thẻ ---
        // Khi nhấn nút "Chi tiết" hoặc cả thẻ item -> Gọi listener để mở màn hình chi tiết
        holder.btnDetails.setOnClickListener(v -> listener.onTourClick(doc));
        holder.itemView.setOnClickListener(v -> listener.onTourClick(doc));
        // Khi nhấn nút "Đặt ngay" -> Tạm thời hiển thị Toast
        holder.btnBook.setOnClickListener(v -> Toast.makeText(context, "Chức năng Đặt tour cho: " + title, Toast.LENGTH_SHORT).show());
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
            holder.updateWishlistIcon(false); // Hoàn tác lại icon nếu không có user
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Tạo một Map chứa dữ liệu cho document wishlist mới
        Map<String, Object> wishlistItem = new HashMap<>();
        wishlistItem.put("tourId", tourId);
        wishlistItem.put("userId", userId);

        // Thêm document mới vào collection "wishlists"
        db.collection("wishlists").add(wishlistItem)
                .addOnSuccessListener(documentReference -> {
                    // Thành công: cập nhật bộ nhớ đệm (Set) và thông báo
                    wishlistedTourIds.add(tourId);
                    Toast.makeText(context, "Đã thêm vào yêu thích!", Toast.LENGTH_SHORT).show();
                    // Không cần notifyItemChanged vì icon đã được cập nhật trước đó
                })
                .addOnFailureListener(e -> {
                    // Thất bại: ghi log lỗi, báo lỗi và hoàn tác lại icon trên giao diện
                    Log.e("WishlistError", "Lỗi khi thêm vào wishlist", e);
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    holder.updateWishlistIcon(false); // Hoàn tác icon về trạng thái rỗng
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
            holder.updateWishlistIcon(true); // Hoàn tác icon về trạng thái đầy
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Tìm document wishlist cần xóa dựa trên tourId và userId
        db.collection("wishlists")
                .whereEqualTo("tourId", tourId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Nếu tìm thấy, thực hiện xóa document đầu tiên trong kết quả
                        querySnapshot.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Xóa thành công: cập nhật bộ nhớ đệm (Set) và thông báo
                                    wishlistedTourIds.remove(tourId);
                                    Toast.makeText(context, "Đã bỏ yêu thích!", Toast.LENGTH_SHORT).show();
                                    // Không cần notifyItemChanged vì icon đã được cập nhật trước đó
                                })
                                .addOnFailureListener(e -> { // Lỗi khi xóa document
                                    Log.e("WishlistError", "Lỗi khi xóa khỏi wishlist", e);
                                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    holder.updateWishlistIcon(true); // Hoàn tác icon về trạng thái đầy
                                });
                    } else {
                        // Trường hợp không tìm thấy document (có thể đã bị xóa trước đó)
                        wishlistedTourIds.remove(tourId); // Vẫn cập nhật bộ nhớ đệm
                        // Không cần notifyItemChanged vì icon đã được cập nhật trước đó
                    }
                })
                .addOnFailureListener(e -> { // Lỗi khi tìm kiếm document
                    Log.e("WishlistError", "Lỗi khi tìm mục wishlist", e);
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    holder.updateWishlistIcon(true); // Hoàn tác icon về trạng thái đầy
                });
    }

    /**
     * Trả về tổng số lượng item trong danh sách tours.
     */
    @Override
    public int getItemCount() {
        return tours.size();
    }

    /**
     * Lớp ViewHolder để giữ các tham chiếu đến các View của một item trong RecyclerView.
     * Giúp tăng hiệu suất bằng cách tránh gọi `findViewById` nhiều lần.
     */
    static class TourViewHolder extends RecyclerView.ViewHolder {
        // Khai báo các thành phần View có trong layout item_tour_customer.xml
        ImageSlider imageSlider;
        ImageView ivFavorite;
        TextView tvTourTitle, tvDescription, tvPrice, tvDestination, tvDuration, tvGuides, tvStartDate;
        Button btnDetails, btnBook;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các biến tới ID tương ứng trong layout
            imageSlider = itemView.findViewById(R.id.imageSlider);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvTourTitle = itemView.findViewById(R.id.tvTourTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvGuides = itemView.findViewById(R.id.tvGuides);
            tvStartDate = itemView.findViewById(R.id.tvStartDate); // Ánh xạ TextView ngày bắt đầu
            btnDetails = itemView.findViewById(R.id.btnDetails);
            btnBook = itemView.findViewById(R.id.btnBook);
        }

        /**
         * Hàm tiện ích để thay đổi icon trái tim dựa trên trạng thái yêu thích.
         * @param isWishlisted true nếu đã yêu thích, false nếu chưa.
         */
        public void updateWishlistIcon(boolean isWishlisted) {
            if (isWishlisted) {
                ivFavorite.setImageResource(R.drawable.ic_favorite); // Icon trái tim đầy
            } else {
                ivFavorite.setImageResource(R.drawable.ic_favorite_border); // Icon trái tim rỗng
            }
        }
    }
}