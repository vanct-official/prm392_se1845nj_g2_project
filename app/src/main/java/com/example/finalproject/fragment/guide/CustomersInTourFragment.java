package com.example.finalproject.fragment.guide;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.guide.CustomersInTourAdapter;
import com.example.finalproject.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
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
    private List<User> customerList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customers_in_tour, container, false);

        rvCustomers = view.findViewById(R.id.rvCustomers);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvCustomers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CustomersInTourAdapter(getContext(), customerList);
        rvCustomers.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentGuideId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadCustomersInMyTours();

        return view;
    }

    private void loadCustomersInMyTours() {
        Log.d("GuideDebug", "Current Guide UID: " + currentGuideId);

        db.collection("tours")
                .whereArrayContains("guideIds", currentGuideId)
                .get()
                .addOnSuccessListener(tourSnapshots -> {
                    List<String> myTourIds = new ArrayList<>();
                    for (DocumentSnapshot tourDoc : tourSnapshots.getDocuments()) {
                        myTourIds.add(tourDoc.getId());
                    }

                    Log.d("GuideDebug", "Tour IDs: " + myTourIds);

                    if (myTourIds.isEmpty()) {
                        showEmptyState("Không tìm thấy tour của bạn.");
                        return;
                    }

                    db.collection("bookings")
                            .whereIn("tourId", myTourIds)
                            .whereEqualTo("status", "confirmed") // ✅ sửa lại chỗ này
                            .get()
                            .addOnSuccessListener(bookingSnapshots -> {
                                List<String> customerIds = new ArrayList<>();
                                for (QueryDocumentSnapshot bookingDoc : bookingSnapshots) {
                                    String userId = bookingDoc.getString("userId");
                                    if (userId != null && !customerIds.contains(userId)) {
                                        customerIds.add(userId);
                                    }
                                }

                                Log.d("GuideDebug", "Customer IDs: " + customerIds);

                                if (customerIds.isEmpty()) {
                                    showEmptyState("Không có khách hàng tham gia tour nào.");
                                    return;
                                }

                                db.collection("users")
                                        .whereIn(FieldPath.documentId(), customerIds)
                                        .get()
                                        .addOnSuccessListener(userSnapshots -> {
                                            customerList.clear();
                                            for (DocumentSnapshot userDoc : userSnapshots) {
                                                String role = userDoc.getString("role");
                                                if ("customer".equals(role)) {
                                                    User user = userDoc.toObject(User.class);
                                                    customerList.add(user);
                                                }
                                            }

                                            Log.d("GuideDebug", "Loaded customers: " + customerList.size());

                                            if (customerList.isEmpty()) {
                                                showEmptyState("Không tìm thấy khách hàng phù hợp.");
                                            } else {
                                                tvEmpty.setVisibility(View.GONE);
                                                rvCustomers.setVisibility(View.VISIBLE);
                                                adapter.notifyDataSetChanged();
                                            }
                                        })
                                        .addOnFailureListener(e -> showEmptyState("Lỗi tải khách hàng: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> showEmptyState("Lỗi tải bookings: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showEmptyState("Lỗi tải tour: " + e.getMessage()));
    }


    private void handleBookingResults(QuerySnapshot bookingSnapshots) {
        List<String> customerIds = new ArrayList<>();
        for (QueryDocumentSnapshot bookingDoc : bookingSnapshots) {
            String userId = bookingDoc.getString("userId");
            if (userId != null && !customerIds.contains(userId)) {
                customerIds.add(userId);
            }
        }

        Log.d("GuideDebug", "Customer IDs: " + customerIds);

        if (customerIds.isEmpty()) {
            showEmptyState("Không có khách hàng tham gia tour nào.");
            return;
        }

        db.collection("users")
                .whereIn(FieldPath.documentId(), customerIds)
                .get()
                .addOnSuccessListener(userSnapshots -> {
                    customerList.clear();
                    for (DocumentSnapshot userDoc : userSnapshots) {
                        String role = userDoc.getString("role");
                        if ("customer".equals(role)) {
                            User user = userDoc.toObject(User.class);
                            customerList.add(user);
                        }
                    }

                    Log.d("GuideDebug", "Loaded customers: " + customerList.size());

                    if (customerList.isEmpty()) {
                        showEmptyState("Không tìm thấy khách hàng phù hợp.");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvCustomers.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> showEmptyState("Lỗi tải khách hàng: " + e.getMessage()));
    }

    private void showEmptyState(String message) {
        rvCustomers.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
        Log.w("GuideDebug", message);
    }
}
