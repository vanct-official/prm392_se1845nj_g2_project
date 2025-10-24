package com.example.finalproject.fragment.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.activity.customer.CustomerTourDetailActivity;
import com.example.finalproject.adapter.customer.CustomerTourAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fragment này là màn hình chính cho khách hàng, hiển thị danh sách các tour du lịch.
 * Nó bao gồm các chức năng: tìm kiếm, lọc theo giá/ngày, và xem chi tiết tour.
 */
public class CustomerHomeFragment extends Fragment {

    private RecyclerView recyclerTours;
    private ProgressBar progressBar;
    private EditText etSearchTour;
    private Button btnFilterPrice;
    private Button btnFilterDate;

    private FirebaseFirestore db;
    private CustomerTourAdapter adapter;
    private List<DocumentSnapshot> allTours = new ArrayList<>(); // Chứa tất cả tour từ Firebase
    private List<DocumentSnapshot> displayedTours = new ArrayList<>(); // Chứa các tour đang được hiển thị (sau khi lọc)
    private Set<String> wishlistedTourIds = new HashSet<>(); // Chứa ID các tour đã yêu thích

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp layout cho fragment
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        // Khởi tạo Firestore và ánh xạ các view
        db = FirebaseFirestore.getInstance();
        recyclerTours = view.findViewById(R.id.recyclerToursCustomer);
        progressBar = view.findViewById(R.id.progressBarCustomer);
        etSearchTour = view.findViewById(R.id.etSearchTour);
        btnFilterPrice = view.findViewById(R.id.btnFilterPrice);
        btnFilterDate = view.findViewById(R.id.btnFilterDate);

        // Bắt đầu quá trình tải dữ liệu
        loadWishlistAndTours();
        // Thiết lập các listener cho tìm kiếm và lọc
        setupSearch();
        setupFilter();

        return view;
    }

    // Thiết lập RecyclerView và Adapter
    private void setupRecyclerView() {
        recyclerTours.setLayoutManager(new LinearLayoutManager(getContext()));
        // Khởi tạo adapter và truyền danh sách ID yêu thích vào
        adapter = new CustomerTourAdapter(getContext(), displayedTours, wishlistedTourIds, doc -> {
            // Xử lý sự kiện khi người dùng click vào một tour -> mở màn hình chi tiết
            Intent intent = new Intent(getContext(), CustomerTourDetailActivity.class); // Mở Activity mới
            String clickedTourId = doc.getId();
            intent.putExtra("tourId", clickedTourId); // Đính kèm ID
            startActivity(intent); // Bắt đầu Activity mới (chuyển màn hình)
        });
        recyclerTours.setAdapter(adapter);
    }

    // Tải danh sách yêu thích trước, sau đó mới tải danh sách tour
    private void loadWishlistAndTours() {
        // Lấy ID của người dùng hiện tại, trả về null nếu chưa đăng nhập
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            // Nếu người dùng chưa đăng nhập, chỉ cần tải tour
            setupRecyclerView();
            loadTours();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        // 1. Tải danh sách yêu thích của người dùng
        db.collection("wishlists")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(wishlistSnapshot -> {
                    wishlistedTourIds.clear(); // Xóa danh sách cũ
                    if (wishlistSnapshot != null) {
                        // Duyệt qua các document kết quả
                        for (DocumentSnapshot doc : wishlistSnapshot.getDocuments()) {
                            String tourId = doc.getString("tourId");
                            if (tourId != null) {
                                wishlistedTourIds.add(tourId); // Thêm ID tour vào Set
                            }
                        }
                    }
                    // 2. Sau khi có danh sách yêu thích, mới thiết lập RecyclerView và tải tour
                    setupRecyclerView();
                    loadTours();
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi khi tải wishlist, vẫn tiếp tục tải tour bình thường
                    setupRecyclerView();
                    loadTours(); // Vẫn tải tour dù wishlist lỗi
                });
    }

    /**
     * Tải danh sách các tour có status là "upcoming" từ Firebase.
     */
    private void loadTours() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("tours")
                .whereEqualTo("status", "upcoming") // Chỉ lấy tour có status là "upcoming"
                .orderBy("start_date", Query.Direction.DESCENDING) // Vẫn sắp xếp theo ngày bắt đầu mới nhất
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allTours.clear(); // Xóa danh sách tour cũ
                    if (querySnapshot != null) {
                        allTours.addAll(querySnapshot.getDocuments()); // Thêm tất cả tour mới tải về
                    }
                    filterTours(""); // Hiển thị toàn bộ danh sách đã lọc ban đầu (áp dụng bộ lọc rỗng)
                    progressBar.setVisibility(View.GONE); // Ẩn ProgressBar
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi khi tải tour, báo lỗi cho người dùng
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE); // Ẩn ProgressBar
                });
    }

    // Thiết lập listener cho ô tìm kiếm để lọc realtime
    private void setupSearch() {
        etSearchTour.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Khi người dùng gõ, gọi hàm lọc danh sách
                filterTours(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Lọc danh sách `allTours` dựa trên từ khóa tìm kiếm (`query`)
     * và cập nhật danh sách `displayedTours` để hiển thị trên RecyclerView.
     * @param query Từ khóa tìm kiếm (nhập từ EditText).
     */
    private void filterTours(String query) {
        displayedTours.clear(); // Xóa danh sách hiển thị hiện tại
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT); // Chuyển từ khóa về chữ thường
        // Duyệt qua tất cả các tour đã tải về (đã lọc theo status=upcoming)
        for (DocumentSnapshot doc : allTours) {
            String tourName = doc.getString("title"); // Lấy tên tour
            // Nếu query rỗng (không tìm kiếm) hoặc tên tour chứa query thì thêm vào danh sách hiển thị
            if (query.isEmpty() || (tourName != null && tourName.toLowerCase(Locale.ROOT).contains(lowerCaseQuery))) {
                displayedTours.add(doc);
            }
        }
        // Thông báo cho adapter biết dữ liệu đã thay đổi để cập nhật RecyclerView
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Thiết lập listener cho các nút lọc (theo giá và theo ngày).
     */
    private void setupFilter() {
        // Xử lý sự kiện click cho nút lọc giá
        btnFilterPrice.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.filter_price_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.sort_price_asc) {
                    sortToursByPrice(true);
                } else if (itemId == R.id.sort_price_desc) {
                    sortToursByPrice(false);
                }
                return true;
            });
            popup.show();
        });

        // Xử lý sự kiện click cho nút lọc ngày
        btnFilterDate.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.filter_date_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.sort_date_asc) {
                    sortToursByDate(true);
                } else if (itemId == R.id.sort_date_desc) {
                    sortToursByDate(false);
                }
                return true;
            });
            popup.show();
        });
    }

    /**
     * Sắp xếp danh sách `allTours` theo giá tour.
     * @param ascending true để sắp xếp tăng dần, false để sắp xếp giảm dần.
     */
    private void sortToursByPrice(boolean ascending) {
        Collections.sort(allTours, (o1, o2) -> {
            Double price1 = o1.getDouble("price");
            Double price2 = o2.getDouble("price");
            if (price1 == null) price1 = 0.0;
            if (price2 == null) price2 = 0.0;
            return ascending ? price1.compareTo(price2) : price2.compareTo(price1);
        });
        // Sau khi sắp xếp lại danh sách gốc (allTours),
        // áp dụng lại bộ lọc tìm kiếm hiện tại để cập nhật danh sách hiển thị (displayedTours)
        filterTours(etSearchTour.getText().toString());
    }

    /**
     * Sắp xếp danh sách `allTours` theo ngày bắt đầu (`start_date`).
     * @param ascending true để sắp xếp tăng dần (ngày cũ nhất trước), false để giảm dần (ngày mới nhất trước).
     */
    private void sortToursByDate(boolean ascending) {
        Collections.sort(allTours, (o1, o2) -> {
            Timestamp date1 = o1.getTimestamp("start_date");
            Timestamp date2 = o2.getTimestamp("start_date");
            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;
            if (ascending) {
                return date1.compareTo(date2);
            } else {
                return date2.compareTo(date1);
            }
        });
        // Sau khi sắp xếp lại, áp dụng lại bộ lọc tìm kiếm hiện tại để cập nhật RecyclerView
        filterTours(etSearchTour.getText().toString());
    }
}