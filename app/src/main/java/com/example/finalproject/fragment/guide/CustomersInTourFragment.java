package com.example.finalproject.fragment.guide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;

import com.example.finalproject.R;
import com.example.finalproject.adapter.guide.CustomersInTourAdapter;
import com.example.finalproject.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomersInTourFragment extends Fragment {

    private RecyclerView rvCustomers;
    private TextView tvEmpty;
    private CustomersInTourAdapter adapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customers_in_tour, container, false);

        rvCustomers = view.findViewById(R.id.rvCustomers);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvCustomers.setLayoutManager(new GridLayoutManager(getContext(), 2));

        db = FirebaseFirestore.getInstance();

        // 🔹 Nhận tourId được truyền từ adapter
        Bundle args = getArguments();
        String selectedTourId = null;
        if (args != null) {
            selectedTourId = args.getString("tourId");
            Log.d("DEBUG_TOUR", "📦 Nhận tourId: " + selectedTourId);
        }

        if (selectedTourId != null) {
            loadCustomersForTour(selectedTourId);
        } else {
            showEmpty("Không tìm thấy tour được chọn.");
        }

        return view;
    }

    // 🔹 Load khách hàng chỉ cho tour cụ thể
    private void loadCustomersForTour(String tourId) {
        CollectionReference bookingsRef = db.collection("bookings");
        CollectionReference usersRef = db.collection("users");

        bookingsRef.whereEqualTo("tourId", tourId)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(bookingSnapshots -> {
                    List<String> customerIds = new ArrayList<>();
                    for (QueryDocumentSnapshot bookingDoc : bookingSnapshots) {
                        String userId = bookingDoc.getString("userId");
                        if (userId != null && !customerIds.contains(userId)) {
                            customerIds.add(userId);
                        }
                    }

                    if (customerIds.isEmpty()) {
                        showEmpty("Không có khách hàng nào xác nhận tham gia tour này.");
                        return;
                    }

                    // 🔹 Lấy thông tin user
                    usersRef.whereIn(FieldPath.documentId(), customerIds)
                            .whereEqualTo("role", "customer")
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                List<User> customerList = new ArrayList<>();
                                for (DocumentSnapshot userDoc : userSnapshots) {
                                    User u = userDoc.toObject(User.class);
                                    customerList.add(u);
                                }

                                if (customerList.isEmpty()) {
                                    showEmpty("Không có khách hàng hợp lệ.");
                                } else {
                                    tvEmpty.setVisibility(View.GONE);
                                    rvCustomers.setVisibility(View.VISIBLE);
                                    adapter = new CustomersInTourAdapter(customerList);
                                    rvCustomers.setAdapter(adapter);
                                }
                            })
                            .addOnFailureListener(e -> showEmpty("Lỗi tải user: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showEmpty("Lỗi tải bookings: " + e.getMessage()));
    }

    private void showEmpty(String msg) {
        rvCustomers.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(msg);
    }
}
