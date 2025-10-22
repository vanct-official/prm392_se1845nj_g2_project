package com.example.finalproject.activity;

import android.content.Intent; // Import Intent nếu chưa có
import android.os.Bundle;
// import android.util.Log; // Log import removed
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger; // Import để đếm tác vụ bất đồng bộ

/**
 * Lớp Activity mới dành riêng cho màn hình chi tiết tour của khách hàng.
 * Hiển thị thông tin chi tiết của một tour và cho phép thêm/xóa khỏi danh sách yêu thích.
 */
public class CustomerTourDetailActivity extends AppCompatActivity {

    // Khai báo các thành phần giao diện (View) khớp với file layout mới
    private Toolbar toolbarCustomerDetail;
    private ImageView ivToolbarFavoriteCustomer;
    private ImageSlider imageSliderCustomerDetail;
    private TextView tvTourTitleCustomerDetail, tvRatingCustomerDetail, tvPriceCustomerDetail, tvDescriptionCustomerDetail;
    private CardView cardViewReviewsCustomer;
    private Button btnBookNowCustomerDetail;
    private TextView tvStartDateDetail, tvDurationDetail, tvGuideNameDetail; // TextViews mới

    // Firebase
    private FirebaseFirestore db;
    private String tourId; // ID của tour đang xem
    private DocumentSnapshot currentTourDoc; // Lưu trữ document tour đã tải về
    private Set<String> wishlistedTourIds = new HashSet<>(); // Set chứa ID các tour đã yêu thích

    // Đối tượng format tiền tệ Việt Nam
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    // Đối tượng format ngày tháng
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sử dụng layout mới đã tạo cho màn hình này
        setContentView(R.layout.activity_customer_tour_detail);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();
        // Lấy tourId được truyền từ màn hình danh sách tour qua Intent
        tourId = getIntent().getStringExtra("tourId");

        // Kiểm tra xem tourId có tồn tại không
        if (tourId == null || tourId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin tour", Toast.LENGTH_SHORT).show();
            finish(); // Đóng Activity nếu không có tourId
            return;
        }

        mapViews(); // Ánh xạ các view từ layout XML vào biến Java
        setupToolbar(); // Cài đặt Toolbar (nút back, tiêu đề, nút yêu thích)
        loadWishlistAndTourDetails(); // Bắt đầu tải dữ liệu
        setupButtonClickListeners(); // Gán sự kiện click cho các nút
    }

    /**
     * Ánh xạ các biến Java tới các thành phần giao diện trong file activity_customer_tour_detail.xml
     */
    private void mapViews() {
        toolbarCustomerDetail = findViewById(R.id.toolbarCustomerDetail);
        ivToolbarFavoriteCustomer = findViewById(R.id.ivToolbarFavoriteCustomer);
        imageSliderCustomerDetail = findViewById(R.id.imageSliderCustomerDetail);
        tvTourTitleCustomerDetail = findViewById(R.id.tvTourTitleCustomerDetail);
        tvRatingCustomerDetail = findViewById(R.id.tvRatingCustomerDetail);
        tvPriceCustomerDetail = findViewById(R.id.tvPriceCustomerDetail);
        tvDescriptionCustomerDetail = findViewById(R.id.tvDescriptionCustomerDetail);
        cardViewReviewsCustomer = findViewById(R.id.cardViewReviewsCustomer);
        btnBookNowCustomerDetail = findViewById(R.id.btnBookNowCustomerDetail);
        tvStartDateDetail = findViewById(R.id.tvStartDateDetail); // Ánh xạ TextViews mới
        tvDurationDetail = findViewById(R.id.tvDurationDetail);
        tvGuideNameDetail = findViewById(R.id.tvGuideNameDetail);
    }

    /**
     * Cài đặt Toolbar: Thiết lập làm ActionBar, hiển thị nút back và gán sự kiện cho nút yêu thích.
     */
    private void setupToolbar() {
        setSupportActionBar(toolbarCustomerDetail); // Đặt toolbar làm ActionBar chính
        // Hiển thị nút Back (mũi tên quay lại) trên ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Gán sự kiện click cho icon yêu thích trên toolbar
        ivToolbarFavoriteCustomer.setOnClickListener(v -> toggleWishlist());
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn vào các item trên toolbar (đặc biệt là nút back).
     * @param item Item được chọn.
     * @return true nếu sự kiện đã được xử lý.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Kiểm tra xem có phải nút back (android.R.id.home) được nhấn không
        if (item.getItemId() == android.R.id.home) {
            finish(); // Đóng activity hiện tại và quay lại màn hình trước đó
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Tải danh sách yêu thích của người dùng trước, sau đó mới tải chi tiết tour.
     * Điều này đảm bảo icon yêu thích hiển thị đúng trạng thái ban đầu.
     */
    private void loadWishlistAndTourDetails() {
        // Lấy ID của người dùng hiện tại, trả về null nếu chưa đăng nhập
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId != null) {
            // Nếu người dùng đã đăng nhập, truy vấn collection "wishlists"
            db.collection("wishlists").whereEqualTo("userId", currentUserId).get()
                    .addOnSuccessListener(wishlistSnapshot -> {
                        wishlistedTourIds.clear(); // Xóa danh sách cũ
                        // Duyệt qua các document kết quả
                        for (DocumentSnapshot doc : wishlistSnapshot.getDocuments()) {
                            String tid = doc.getString("tourId");
                            if (tid != null) {
                                wishlistedTourIds.add(tid); // Thêm ID tour vào Set
                            }
                        }
                        loadTourDetail(); // Sau khi có wishlist, mới tải chi tiết tour
                    })
                    .addOnFailureListener(e -> {
                        // Nếu lỗi khi tải wishlist, vẫn tiếp tục tải tour
                        loadTourDetail();
                    });
        } else {
            // Nếu người dùng chưa đăng nhập, bỏ qua tải wishlist, tải tour trực tiếp
            loadTourDetail();
        }
    }

    /**
     * Tải dữ liệu chi tiết của tour từ Firestore dựa vào tourId.
     */
    private void loadTourDetail() {
        db.collection("tours").document(tourId).get()
                .addOnSuccessListener(doc -> {
                    // Kiểm tra xem document có tồn tại không
                    if (doc != null && doc.exists()) {
                        currentTourDoc = doc; // Lưu lại document để có thể dùng sau
                        bindTourData(doc); // Gán dữ liệu lên giao diện
                        // Cập nhật icon dựa trên trạng thái wishlist đã tải
                        updateToolbarFavoriteIcon(wishlistedTourIds.contains(tourId));
                        loadReviews(); // Gọi hàm tải reviews sau khi tải xong tour
                        // Gọi hàm tải tên HDV sau khi bind data tour
                        List<String> guideIds = (List<String>) doc.get("guideIds"); // Lấy danh sách ID HDV từ tour
                        if (guideIds != null && !guideIds.isEmpty()) {
                            loadGuideNames(guideIds); // Gọi hàm tải tên
                        } else {
                            tvGuideNameDetail.setText("Hướng dẫn viên: Chưa có"); // Hiển thị nếu không có HDV
                        }
                    } else {
                        // Nếu không tìm thấy tour
                        Toast.makeText(this, "Không tìm thấy tour!", Toast.LENGTH_SHORT).show();
                        finish(); // Đóng màn hình
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi khi tải dữ liệu từ Firestore
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Gán dữ liệu từ DocumentSnapshot (dữ liệu tour) lên các thành phần giao diện.
     * @param doc DocumentSnapshot chứa dữ liệu của tour.
     */
    private void bindTourData(DocumentSnapshot doc) {
        try {
            // Lấy dữ liệu từ document
            String title = doc.getString("title");
            String description = doc.getString("description");
            Double price = doc.getDouble("price");
            List<String> images = (List<String>) doc.get("images");
            Timestamp startDate = doc.getTimestamp("start_date");
            String duration = doc.getString("duration");

            // Gán dữ liệu vào các TextView
            tvTourTitleCustomerDetail.setText(title);
            tvDescriptionCustomerDetail.setText(description);
            // Hiển thị giá (đã bỏ icon 💰)
            tvPriceCustomerDetail.setText("Giá: " + currencyFormatter.format(price != null ? price : 0) + " / người");

            // Hiển thị ngày bắt đầu
            if (startDate != null) {
                tvStartDateDetail.setText("Ngày khởi hành: " + sdf.format(startDate.toDate()));
            } else {
                tvStartDateDetail.setText("Ngày khởi hành: Chưa xác định");
            }

            // Hiển thị thời lượng
            tvDurationDetail.setText("Thời lượng: " + (duration != null ? duration : "--"));


            // Cài đặt Image Slider
            List<SlideModel> slideModels = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                for (String url : images) {
                    slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
                }
            } else {
                slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
            }
            imageSliderCustomerDetail.setImageList(slideModels);

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi hiển thị dữ liệu chi tiết.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hàm mới: Tải đánh giá từ collection "reviews" cho tour hiện tại.
     * Tính toán điểm trung bình và số lượng đánh giá, sau đó cập nhật TextView.
     */
    private void loadReviews() {
        if (tourId == null || tourId.isEmpty()) {
            return; // Không tải nếu không có tourId
        }

        db.collection("reviews")
                .whereEqualTo("tourId", tourId) // Lọc các review có tourId khớp
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        // Nếu không có đánh giá nào (đã bỏ icon ⭐)
                        tvRatingCustomerDetail.setText("Chưa có đánh giá");
                    } else {
                        // Nếu có đánh giá, tính toán điểm trung bình
                        double totalRating = 0;
                        int reviewCount = querySnapshot.size(); // Số lượng đánh giá
                        for (DocumentSnapshot reviewDoc : querySnapshot.getDocuments()) {
                            Double rating = reviewDoc.getDouble("rating"); // Lấy điểm rating
                            if (rating != null) {
                                totalRating += rating;
                            }
                        }
                        // Tính điểm trung bình
                        double averageRating = (reviewCount > 0) ? totalRating / reviewCount : 0;
                        // Hiển thị kết quả lên TextView (đã bỏ icon ⭐)
                        tvRatingCustomerDetail.setText(String.format(Locale.getDefault(),
                                "%.1f (%d đánh giá)", averageRating, reviewCount));
                    }
                })
                .addOnFailureListener(e -> {
                    // Nếu có lỗi khi tải reviews (đã bỏ icon ⭐)
                    tvRatingCustomerDetail.setText("Lỗi tải đánh giá");
                });
    }

    /**
     * Hàm mới: Tải tên của các hướng dẫn viên từ collection "users".
     * Dựa vào danh sách ID hướng dẫn viên (`guideIds`) lấy từ tour.
     * @param guideIds Danh sách ID của các hướng dẫn viên.
     */
    private void loadGuideNames(List<String> guideIds) {
        if (guideIds == null || guideIds.isEmpty()) {
            tvGuideNameDetail.setText("Hướng dẫn viên: Chưa có");
            return;
        }

        // Dùng StringBuilder để nối tên các HDV hiệu quả
        StringBuilder guideNamesBuilder = new StringBuilder();
        // Sử dụng AtomicInteger để đếm số lượng tác vụ bất đồng bộ đã hoàn thành
        AtomicInteger counter = new AtomicInteger(0);
        int totalGuides = guideIds.size(); // Tổng số HDV cần tải

        // Lặp qua từng ID trong danh sách guideIds
        for (String guideId : guideIds) {
            // Truy vấn collection "users" bằng ID của hướng dẫn viên
            db.collection("users").document(guideId).get()
                    .addOnSuccessListener(userDoc -> {
                        // Kiểm tra xem document user có tồn tại không
                        if (userDoc != null && userDoc.exists()) {
                            // Lấy firstname và lastname từ document user
                            String firstName = userDoc.getString("firstname");
                            String lastName = userDoc.getString("lastname");
                            String fullName = ""; // Biến tạm để ghép tên

                            // Ghép tên (xử lý trường hợp chỉ có first hoặc last name)
                            if (firstName != null && !firstName.isEmpty()) {
                                fullName += firstName;
                            }
                            if (lastName != null && !lastName.isEmpty()) {
                                if (!fullName.isEmpty()) fullName += " "; // Thêm khoảng trắng nếu có cả first và last
                                fullName += lastName;
                            }

                            // Nếu ghép được tên thành công
                            if (!fullName.isEmpty()) {
                                // Thêm dấu phẩy vào trước tên thứ 2 trở đi
                                if (guideNamesBuilder.length() > 0) {
                                    guideNamesBuilder.append(", ");
                                }
                                guideNamesBuilder.append(fullName); // Thêm tên vào chuỗi kết quả
                            }
                        }
                        // Tăng biến đếm sau mỗi lần truy vấn (thành công hoặc user không tồn tại)
                        if (counter.incrementAndGet() == totalGuides) {
                            // Nếu đã tải xong tất cả HDV, hiển thị kết quả
                            String result = guideNamesBuilder.toString();
                            tvGuideNameDetail.setText("Hướng dẫn viên: " + (result.isEmpty() ? "Không tìm thấy" : result));
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Nếu có lỗi khi tải thông tin của một HDV
                        // Vẫn tăng biến đếm để đảm bảo hàm không bị treo
                        if (counter.incrementAndGet() == totalGuides) {
                            // Nếu đây là lỗi cuối cùng, hiển thị kết quả đã có và thông báo lỗi
                            String result = guideNamesBuilder.toString();
                            tvGuideNameDetail.setText("Hướng dẫn viên: " + (result.isEmpty() ? "Lỗi tải" : result));
                        }
                    });
        }
    }


    /**
     * Cập nhật trạng thái (icon) của nút yêu thích trên thanh Toolbar.
     * @param isFavorite true nếu tour đang được yêu thích, false nếu không.
     */
    private void updateToolbarFavoriteIcon(boolean isFavorite) {
        if (isFavorite) {
            ivToolbarFavoriteCustomer.setImageResource(R.drawable.ic_favorite); // Trái tim đầy
        } else {
            ivToolbarFavoriteCustomer.setImageResource(R.drawable.ic_favorite_border); // Trái tim rỗng
        }
    }


    /**
     * Xử lý sự kiện khi người dùng nhấn vào icon yêu thích trên Toolbar.
     * Thêm hoặc xóa tour khỏi danh sách yêu thích trên Firebase.
     */
    private void toggleWishlist() {
        // Lấy ID người dùng, kiểm tra xem đã đăng nhập chưa
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để yêu thích", Toast.LENGTH_SHORT).show();
            return; // Dừng lại nếu chưa đăng nhập
        }

        boolean currentlyWishlisted = wishlistedTourIds.contains(tourId);

        if (currentlyWishlisted) {
            // --- Xóa khỏi Wishlist ---
            db.collection("wishlists").whereEqualTo("tourId", tourId).whereEqualTo("userId", userId).get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            querySnapshot.getDocuments().get(0).getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        wishlistedTourIds.remove(tourId);
                                        updateToolbarFavoriteIcon(false);
                                        Toast.makeText(this,"Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this,"Lỗi khi bỏ yêu thích: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        updateToolbarFavoriteIcon(true);
                                    });
                        } else {
                            wishlistedTourIds.remove(tourId);
                            updateToolbarFavoriteIcon(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,"Lỗi khi tìm mục yêu thích: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateToolbarFavoriteIcon(true);
                    });
        } else {
            // --- Thêm vào Wishlist ---
            Map<String, Object> wishlistItem = new HashMap<>();
            wishlistItem.put("tourId", tourId);
            wishlistItem.put("userId", userId);
            db.collection("wishlists").add(wishlistItem)
                    .addOnSuccessListener(documentReference -> {
                        wishlistedTourIds.add(tourId);
                        updateToolbarFavoriteIcon(true);
                        Toast.makeText(this,"Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,"Lỗi khi yêu thích: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateToolbarFavoriteIcon(false);
                    });
        }
    }


    /**
     * Gán sự kiện click cho các nút và thẻ trong màn hình.
     */
    private void setupButtonClickListeners() {
        // Nút "Đặt ngay"
        btnBookNowCustomerDetail.setOnClickListener(v -> {
            // Tạm thời hiển thị thông báo
            Toast.makeText(this, "Chức năng Đặt ngay đang được phát triển!", Toast.LENGTH_SHORT).show();
            // TODO: Triển khai logic điều hướng sang màn hình đặt tour
        });

        // Thẻ "Đánh giá từ khách hàng"
        cardViewReviewsCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerReviewsActivity.class); // Mở Activity đánh giá
            intent.putExtra("tourId", tourId); // Truyền ID tour sang
            startActivity(intent);
        });
    }
}