package com.example.finalproject.activity.customer;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalproject.R;
import com.example.finalproject.adapter.customer.CustomerReviewAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.*;

public class CustomerReviewsActivity extends AppCompatActivity {

    // Views
    private Toolbar toolbarReviews;
    private TextView tvOverallRatingSummary, tvRatingDistribution;
    private LinearLayout layoutRatingStars;
    private EditText etReviewComment;
    private Button btnSubmitReview;
    private RecyclerView recyclerReviews;
    private ProgressBar progressBarReviews;

    // Firebase & Data
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String tourId;
    private CustomerReviewAdapter reviewAdapter; // Sử dụng Adapter mới
    private List<DocumentSnapshot> reviewList = new ArrayList<>();
    private int selectedRating = 0; // Số sao người dùng chọn (0 = chưa chọn)

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sử dụng layout activity_customer_reviews.xml
        setContentView(R.layout.activity_customer_reviews);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        tourId = getIntent().getStringExtra("tourId");

        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy Tour ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mapViews();
        setupToolbar();
        setupRatingStars();
        setupRecyclerView();
        setupSubmitButton();
        loadReviewsData();
    }

    private void mapViews() {
        toolbarReviews = findViewById(R.id.toolbarReviews);
        tvOverallRatingSummary = findViewById(R.id.tvOverallRatingSummary);
        tvRatingDistribution = findViewById(R.id.tvRatingDistribution);
        layoutRatingStars = findViewById(R.id.layoutRatingStars);
        etReviewComment = findViewById(R.id.etReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        recyclerReviews = findViewById(R.id.recyclerReviews);
        progressBarReviews = findViewById(R.id.progressBarReviews);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbarReviews);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Cài đặt sự kiện click cho các ngôi sao đánh giá
    private void setupRatingStars() {
        for (int i = 0; i < layoutRatingStars.getChildCount(); i++) {
            ImageView star = (ImageView) layoutRatingStars.getChildAt(i);
            final int rating = i + 1; // Số sao tương ứng (1 đến 5)
            star.setOnClickListener(v -> {
                selectedRating = rating; // Lưu lại số sao đã chọn
                updateRatingStarsUI(rating); // Cập nhật giao diện sao
            });
        }
    }

    // Cập nhật giao diện các ngôi sao khi người dùng chọn
    private void updateRatingStarsUI(int rating) {
        for (int i = 0; i < layoutRatingStars.getChildCount(); i++) {
            ImageView star = (ImageView) layoutRatingStars.getChildAt(i);
            if (i < rating) {
                star.setImageResource(R.drawable.ic_star_rate); // Sao đầy
            } else {
                star.setImageResource(R.drawable.ic_star_rate_border); // Sao rỗng
            }
            // Cập nhật màu sao (có thể bỏ nếu dùng tint trong XML)
            star.setColorFilter(ContextCompat.getColor(this, R.color.star_color));
        }
    }

    private void setupRecyclerView() {
        // Sử dụng CustomerReviewAdapter
        reviewAdapter = new CustomerReviewAdapter(this, reviewList);
        recyclerReviews.setLayoutManager(new LinearLayoutManager(this)); // Cần LayoutManager
        recyclerReviews.setAdapter(reviewAdapter);
    }

    // Cài đặt sự kiện click cho nút Gửi
    private void setupSubmitButton() {
        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    /**
     * Logic chính khi nhấn nút gửi đánh giá.
     * Kiểm tra input -> Kiểm tra đã đánh giá chưa -> Kiểm tra đã đặt tour chưa -> Gửi lên Firebase.
     */
    private void submitReview() {
        String comment = etReviewComment.getText().toString().trim();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // --- Bước 1: Kiểm tra đầu vào cơ bản ---
        if (selectedRating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập cảm nhận của bạn", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Vô hiệu hóa nút và hiển thị progress ---
        btnSubmitReview.setEnabled(false);
        progressBarReviews.setVisibility(View.VISIBLE);

        // --- Bước 2: Kiểm tra xem người dùng đã đánh giá tour này chưa ---
        db.collection("reviews")
                .whereEqualTo("tourId", tourId)
                .whereEqualTo("userId", userId)
                .limit(1) // Chỉ cần tìm 1 cái là đủ
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Nếu tìm thấy bất kỳ review nào khớp (size > 0) -> Đã đánh giá
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            Toast.makeText(this, "Bạn đã đánh giá tour này rồi!", Toast.LENGTH_LONG).show();
                            btnSubmitReview.setEnabled(true);
                            progressBarReviews.setVisibility(View.GONE);
                        } else {
                            // Nếu chưa đánh giá -> Tiếp tục kiểm tra xem đã đặt tour chưa
                            checkBookingAndSubmit(userId, comment);
                        }
                    } else {
                        // Lỗi khi kiểm tra review cũ
                        Toast.makeText(this, "Lỗi kiểm tra đánh giá cũ: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnSubmitReview.setEnabled(true);
                        progressBarReviews.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Hàm phụ: Kiểm tra xem người dùng đã đặt tour chưa, nếu rồi thì mới gửi đánh giá.
     * @param userId ID người dùng.
     * @param comment Nội dung bình luận.
     */
    private void checkBookingAndSubmit(String userId, String comment) {
        // --- Bước 3: Kiểm tra xem người dùng đã đặt tour này chưa ---
        db.collection("bookings")
                .whereEqualTo("tourId", tourId)
                .whereEqualTo("userId", userId)
                // Optional: Thêm điều kiện kiểm tra trạng thái booking (ví dụ: đã xác nhận)
                .whereEqualTo("status", "confirmed") // Dựa theo ảnh db bookings
                .limit(1) // Chỉ cần tìm 1 booking là đủ
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Nếu tìm thấy booking khớp (size > 0) -> Cho phép đánh giá
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            // --- Bước 4: Gửi đánh giá lên Firestore ---
                            Map<String, Object> newReview = new HashMap<>();
                            newReview.put("userId", userId);
                            newReview.put("tourId", tourId);
                            newReview.put("rating", (double) selectedRating); // Lưu dạng double
                            newReview.put("comment", comment);
                            newReview.put("createdAt", Timestamp.now()); // Thời gian tạo

                            db.collection("reviews").add(newReview)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "Gửi đánh giá thành công!", Toast.LENGTH_SHORT).show();
                                        // Reset trạng thái nhập liệu
                                        etReviewComment.setText("");
                                        selectedRating = 0;
                                        updateRatingStarsUI(0);
                                        // Tải lại danh sách review để hiển thị đánh giá mới
                                        loadReviewsData();
                                        // TODO: Cập nhật lại rating trung bình trong collection "tours" (nên dùng Cloud Function)
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Lỗi khi gửi đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnCompleteListener(submitTask -> {
                                        // Dù thành công hay thất bại khi gửi, bật lại nút và ẩn progress
                                        btnSubmitReview.setEnabled(true);
                                        progressBarReviews.setVisibility(View.GONE);
                                    });
                        } else {
                            // Nếu không tìm thấy booking -> Không cho phép đánh giá
                            Toast.makeText(this, "Bạn cần đặt và hoàn thành tour này để đánh giá!", Toast.LENGTH_LONG).show();
                            btnSubmitReview.setEnabled(true);
                            progressBarReviews.setVisibility(View.GONE);
                        }
                    } else {
                        // Lỗi khi kiểm tra booking
                        Toast.makeText(this, "Lỗi kiểm tra thông tin đặt tour: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnSubmitReview.setEnabled(true);
                        progressBarReviews.setVisibility(View.GONE);
                    }
                });
    }


    // Tải tất cả review của tour này và tính toán tổng quan
    private void loadReviewsData() {
        progressBarReviews.setVisibility(View.VISIBLE);

        db.collection("reviews")
                .whereEqualTo("tourId", tourId)
                .orderBy("createdAt", Query.Direction.DESCENDING) // Sắp xếp mới nhất lên đầu
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reviewList.clear(); // Xóa list cũ trước khi thêm mới
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        reviewList.addAll(querySnapshot.getDocuments());
                        calculateAndDisplayOverallRating(querySnapshot); // Tính toán tổng quan
                    } else {
                        // Xử lý trường hợp không có review nào
                        tvOverallRatingSummary.setText("Chưa có đánh giá");
                        tvRatingDistribution.setText("");
                    }
                    reviewAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView
                    progressBarReviews.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvOverallRatingSummary.setText("Lỗi tải đánh giá");
                    tvRatingDistribution.setText("");
                    progressBarReviews.setVisibility(View.GONE);
                });
    }

    // Tính toán và hiển thị điểm trung bình, phân bố sao
    private void calculateAndDisplayOverallRating(QuerySnapshot querySnapshot) {
        if (querySnapshot == null || querySnapshot.isEmpty()) return; // Thoát nếu không có dữ liệu

        double totalRating = 0;
        int reviewCount = querySnapshot.size();
        // Mảng đếm số lượng cho từng mức sao (index 0 bỏ qua, 1->5)
        int[] starCounts = new int[6];

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            Double rating = doc.getDouble("rating");
            if (rating != null) {
                totalRating += rating;
                int star = (int) Math.round(rating); // Làm tròn đến sao gần nhất
                if (star >= 1 && star <= 5) {
                    starCounts[star]++; // Tăng biến đếm cho mức sao đó
                }
            }
        }

        double averageRating = (reviewCount > 0) ? totalRating / reviewCount : 0;

        // Hiển thị điểm trung bình và tổng số
        tvOverallRatingSummary.setText(String.format(Locale.getDefault(),
                "%.1f/5 (%d đánh giá)", averageRating, reviewCount));

        // Tính và hiển thị phần trăm phân bố (chỉ hiển thị 5, 4, 3 sao)
        int percent5 = (reviewCount > 0) ? (int) Math.round((double) starCounts[5] * 100 / reviewCount) : 0;
        int percent4 = (reviewCount > 0) ? (int) Math.round((double) starCounts[4] * 100 / reviewCount) : 0;
        int percent3 = (reviewCount > 0) ? (int) Math.round((double) starCounts[3] * 100 / reviewCount) : 0;
        tvRatingDistribution.setText(String.format(Locale.getDefault(),
                "%d%% 5★ | %d%% 4★ | %d%% 3★", percent5, percent4, percent3));
    }
}