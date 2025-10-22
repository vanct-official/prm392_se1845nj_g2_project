package com.example.finalproject.activity; // Đảm bảo đúng package

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable; // Import TextWatcher
import android.text.TextWatcher; // Import TextWatcher
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText; // Import EditText
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.CustomerTourAdapter; // Tái sử dụng Adapter
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale; // Import Locale
import java.util.Set;

public class CustomerFavoriteToursActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerFavoriteTours;
    private ProgressBar progressBarFavorites;
    private TextView tvNoFavorites;
    private EditText etSearchFavorites; // Khai báo EditText tìm kiếm

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private CustomerTourAdapter adapter;
    private List<DocumentSnapshot> allFavoriteTours = new ArrayList<>(); // Lưu tất cả tour yêu thích đã tải
    private List<DocumentSnapshot> displayedFavoriteTours = new ArrayList<>(); // Lưu tour đang hiển thị (sau khi lọc)
    private Set<String> wishlistedTourIds = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_favorite_tours);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbarFavoriteTours);
        recyclerFavoriteTours = findViewById(R.id.recyclerFavoriteTours);
        progressBarFavorites = findViewById(R.id.progressBarFavorites);
        tvNoFavorites = findViewById(R.id.tvNoFavorites);
        etSearchFavorites = findViewById(R.id.etSearchFavorites); // Ánh xạ EditText

        setupToolbar();
        setupRecyclerView();
        loadFavoriteTourIds();
        setupSearchListener(); // Gọi hàm cài đặt tìm kiếm
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tour Yêu Thích"); // Đặt lại tiêu đề nếu muốn
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

    private void setupRecyclerView() {
        recyclerFavoriteTours.setLayoutManager(new LinearLayoutManager(this));
        // Adapter giờ sẽ dùng displayedFavoriteTours
        adapter = new CustomerTourAdapter(this, displayedFavoriteTours, wishlistedTourIds, doc -> {
            Intent intent = new Intent(this, CustomerTourDetailActivity.class);
            intent.putExtra("tourId", doc.getId());
            startActivity(intent);
        });
        recyclerFavoriteTours.setAdapter(adapter);
    }

    private void loadFavoriteTourIds() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = currentUser.getUid();

        progressBarFavorites.setVisibility(View.VISIBLE);
        tvNoFavorites.setVisibility(View.GONE);
        recyclerFavoriteTours.setVisibility(View.GONE);

        db.collection("wishlists")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> tourIds = new ArrayList<>();
                    wishlistedTourIds.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String tourId = document.getString("tourId");
                        if (tourId != null && !tourId.isEmpty()) {
                            tourIds.add(tourId);
                            wishlistedTourIds.add(tourId);
                        }
                    }

                    if (tourIds.isEmpty()) {
                        progressBarFavorites.setVisibility(View.GONE);
                        tvNoFavorites.setVisibility(View.VISIBLE);
                        recyclerFavoriteTours.setVisibility(View.GONE);
                        allFavoriteTours.clear(); // Xóa list gốc
                        displayedFavoriteTours.clear();
                        adapter.notifyDataSetChanged();
                    } else {
                        loadFavoriteTourDetails(tourIds);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBarFavorites.setVisibility(View.GONE);
                    tvNoFavorites.setText("Lỗi tải danh sách yêu thích");
                    tvNoFavorites.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFavoriteTourDetails(List<String> tourIds) {
        if (tourIds.isEmpty()) return;

        // Chia nhỏ tourIds thành các nhóm 10 (hoặc 30) nếu cần thiết cho whereIn
        // Tạm thời giả sử tourIds.size() <= 30
        db.collection("tours")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), tourIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allFavoriteTours.clear(); // Xóa list gốc cũ
                    allFavoriteTours.addAll(querySnapshot.getDocuments()); // Lưu vào list gốc

                    filterFavoriteTours(""); // Hiển thị tất cả ban đầu

                    progressBarFavorites.setVisibility(View.GONE);
                    if (allFavoriteTours.isEmpty()) { // Kiểm tra list gốc
                        tvNoFavorites.setVisibility(View.VISIBLE);
                        recyclerFavoriteTours.setVisibility(View.GONE);
                    } else {
                        tvNoFavorites.setVisibility(View.GONE);
                        recyclerFavoriteTours.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBarFavorites.setVisibility(View.GONE);
                    tvNoFavorites.setText("Lỗi tải thông tin tour yêu thích");
                    tvNoFavorites.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cài đặt TextWatcher để lắng nghe thay đổi trong ô tìm kiếm.
     */
    private void setupSearchListener() {
        etSearchFavorites.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFavoriteTours(s.toString()); // Gọi hàm lọc khi text thay đổi
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Lọc danh sách allFavoriteTours dựa trên query và cập nhật displayedFavoriteTours.
     * @param query Từ khóa tìm kiếm.
     */
    private void filterFavoriteTours(String query) {
        displayedFavoriteTours.clear(); // Xóa danh sách hiển thị cũ
        String lowerCaseQuery = query.toLowerCase(Locale.getDefault());

        if (query.isEmpty()) {
            displayedFavoriteTours.addAll(allFavoriteTours); // Hiển thị tất cả nếu query rỗng
        } else {
            for (DocumentSnapshot doc : allFavoriteTours) {
                String title = doc.getString("title"); // Lấy tên tour
                if (title != null && title.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    displayedFavoriteTours.add(doc); // Thêm vào list hiển thị nếu khớp
                }
            }
        }
        // Thông báo cho adapter cập nhật RecyclerView
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        // Cập nhật trạng thái "Không có kết quả" nếu cần
        if (displayedFavoriteTours.isEmpty() && !allFavoriteTours.isEmpty()) {
            tvNoFavorites.setText("Không tìm thấy tour phù hợp.");
            tvNoFavorites.setVisibility(View.VISIBLE);
            recyclerFavoriteTours.setVisibility(View.GONE);
        } else if (!displayedFavoriteTours.isEmpty()){
            tvNoFavorites.setVisibility(View.GONE);
            recyclerFavoriteTours.setVisibility(View.VISIBLE);
        } else if (allFavoriteTours.isEmpty()) {
            tvNoFavorites.setText("Bạn chưa có tour yêu thích nào.");
            tvNoFavorites.setVisibility(View.VISIBLE);
            recyclerFavoriteTours.setVisibility(View.GONE);
        }
    }
}