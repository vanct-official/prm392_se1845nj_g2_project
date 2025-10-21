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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomersInTourFragment extends Fragment {

    private RecyclerView rvCustomers;
    private TextView tvEmpty;
    private CustomersInTourAdapter adapter;
    private FirebaseFirestore db;
    private String currentGuideId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customers_in_tour, container, false);

        rvCustomers = view.findViewById(R.id.rvCustomers);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        rvCustomers.setLayoutManager(new GridLayoutManager(getContext(), 2));
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            showEmpty("Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
            Log.e("DEBUG_GUIDE", "❌ FirebaseAuth currentUser is NULL!");
            return view;
        }
        currentGuideId = auth.getCurrentUser().getUid();
        Log.d("DEBUG_GUIDE", "👉 Current logged UID: " + currentGuideId);
        loadCustomersForGuide(currentGuideId);
        return view;
    }

    private void loadCustomersForGuide(String guideId) {
        CollectionReference toursRef = db.collection("tours");
        CollectionReference bookingsRef = db.collection("bookings");
        CollectionReference usersRef = db.collection("users");

        // 1️⃣ Tìm các tour mà hướng dẫn viên này đang phụ trách
        toursRef.whereArrayContains("guideIds", guideId).get()
                .addOnSuccessListener(tourSnapshots -> {
                    List<String> tourIds = new ArrayList<>();
                    for (QueryDocumentSnapshot tourDoc : tourSnapshots) {
                        tourIds.add(tourDoc.getId());
                    }

                    if (tourIds.isEmpty()) {
                        showEmpty("Bạn chưa có tour nào được giao.");
                        return;
                    }

                    // 2️⃣ Tìm các booking thuộc các tour này có status = confirmed
                    bookingsRef.whereIn("tourId", tourIds)
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
                                    showEmpty("Không có khách hàng nào xác nhận tham gia tour.");
                                    return;
                                }

                                // 3️⃣ Lấy thông tin user theo ID
                                usersRef.get().addOnSuccessListener(userSnapshots -> {
                                    List<User> customerList = new ArrayList<>();
                                    for (DocumentSnapshot userDoc : userSnapshots) {
                                        if (customerIds.contains(userDoc.getId())
                                                && "customer".equals(userDoc.getString("role"))) {
                                            User u = userDoc.toObject(User.class);
                                            customerList.add(u);
                                        }
                                    }

                                    if (customerList.isEmpty()) {
                                        showEmpty("Không có khách hàng hợp lệ.");
                                    } else {
                                        tvEmpty.setVisibility(View.GONE);
                                        rvCustomers.setVisibility(View.VISIBLE);
                                        adapter = new CustomersInTourAdapter(customerList);
                                        rvCustomers.setAdapter(adapter);
                                    }
                                });
                            });
                })
                .addOnFailureListener(e -> showEmpty("Lỗi tải dữ liệu: " + e.getMessage()));
    }

    private void showEmpty(String msg) {
        rvCustomers.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(msg);
    }
}
