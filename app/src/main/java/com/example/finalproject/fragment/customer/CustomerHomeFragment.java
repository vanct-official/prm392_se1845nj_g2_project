package com.example.finalproject.fragment.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
// import android.util.Log; // Đã xóa import Log
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
import com.example.finalproject.activity.CustomerTourDetailActivity; // Đảm bảo import đúng Activity
import com.example.finalproject.adapter.CustomerTourAdapter;
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
    private List<DocumentSnapshot> allTours = new ArrayList<>();
    private List<DocumentSnapshot> displayedTours = new ArrayList<>();
    private Set<String> wishlistedTourIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerTours = view.findViewById(R.id.recyclerToursCustomer);
        progressBar = view.findViewById(R.id.progressBarCustomer);
        etSearchTour = view.findViewById(R.id.etSearchTour);
        btnFilterPrice = view.findViewById(R.id.btnFilterPrice);
        btnFilterDate = view.findViewById(R.id.btnFilterDate);

        loadWishlistAndTours();
        setupSearch();
        setupFilter();

        return view;
    }

    private void setupRecyclerView() {
        recyclerTours.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CustomerTourAdapter(getContext(), displayedTours, wishlistedTourIds, doc -> {
            Intent intent = new Intent(getContext(), CustomerTourDetailActivity.class);
            String clickedTourId = doc.getId();
            intent.putExtra("tourId", clickedTourId);
            startActivity(intent);
        });
        recyclerTours.setAdapter(adapter);
    }

    private void loadWishlistAndTours() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            setupRecyclerView();
            loadTours();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        db.collection("wishlists")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(wishlistSnapshot -> {
                    wishlistedTourIds.clear();
                    for (DocumentSnapshot doc : wishlistSnapshot.getDocuments()) {
                        String tourId = doc.getString("tourId");
                        if (tourId != null) {
                            wishlistedTourIds.add(tourId);
                        }
                    }
                    setupRecyclerView();
                    loadTours();
                })
                .addOnFailureListener(e -> {
                    setupRecyclerView();
                    loadTours();
                });
    }

    private void loadTours() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("tours")
                .orderBy("start_date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allTours.clear();
                    allTours.addAll(querySnapshot.getDocuments());
                    filterTours("");
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void setupSearch() {
        etSearchTour.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTours(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterTours(String query) {
        displayedTours.clear();
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
        for (DocumentSnapshot doc : allTours) {
            String tourName = doc.getString("title");
            if (query.isEmpty() || (tourName != null && tourName.toLowerCase(Locale.ROOT).contains(lowerCaseQuery))) {
                displayedTours.add(doc);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupFilter() {
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

    private void sortToursByPrice(boolean ascending) {
        Collections.sort(allTours, (o1, o2) -> {
            Double price1 = o1.getDouble("price");
            Double price2 = o2.getDouble("price");
            if (price1 == null) price1 = 0.0;
            if (price2 == null) price2 = 0.0;
            return ascending ? price1.compareTo(price2) : price2.compareTo(price1);
        });
        filterTours(etSearchTour.getText().toString());
    }

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
        filterTours(etSearchTour.getText().toString());
    }
}