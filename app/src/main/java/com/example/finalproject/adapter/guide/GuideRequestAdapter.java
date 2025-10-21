package com.example.finalproject.adapter.guide;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.entity.Booking;
import com.example.finalproject.entity.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter hiển thị danh sách lời mời cho hướng dẫn viên
 */
public class GuideRequestAdapter extends RecyclerView.Adapter<GuideRequestAdapter.RequestViewHolder> {

    private final List<DocumentSnapshot> requests;
    private final OnRequestClickListener listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnRequestClickListener {
        void onRequestClick(DocumentSnapshot doc);
    }

    public GuideRequestAdapter(List<DocumentSnapshot> requests, OnRequestClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guide_request, parent, false);
        return new RequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        DocumentSnapshot doc = requests.get(position);
        holder.bind(doc, listener, db);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvTourTitle, tvDestination, tvDate, tvStatus;
        LinearLayout itemLayout;

        RequestViewHolder(View v) {
            super(v);
            tvTourTitle = v.findViewById(R.id.tvTourTitle);
            tvDestination = v.findViewById(R.id.tvDestination);
            tvDate = v.findViewById(R.id.tvDate);
            tvStatus = v.findViewById(R.id.tvStatus);
            itemLayout = v.findViewById(R.id.itemLayout);
        }

        @SuppressLint("SetTextI18n")
        void bind(DocumentSnapshot doc, OnRequestClickListener listener, FirebaseFirestore db) {
            String tourId = doc.getString("tourId");
            String status = doc.getString("status");
            Timestamp createdAt = doc.getTimestamp("createdAt");

            tvStatus.setText("Trạng thái: " + (status != null ? status : "Không rõ"));
            tvDate.setText("Ngày gửi: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(createdAt != null ? createdAt.toDate() : new java.util.Date()));

            // 🔹 Lấy thông tin tour từ Firestore để hiển thị rõ ràng hơn
            db.collection("tours").document(tourId)
                    .get()
                    .addOnSuccessListener(tourDoc -> {
                        if (tourDoc.exists()) {
                            Map<String, Object> tour = tourDoc.getData();
                            tvTourTitle.setText("📍 " + tour.get("title"));
                            tvDestination.setText("Địa điểm: " + tour.get("destination"));
                        } else {
                            tvTourTitle.setText("Tour ID: " + tourId);
                            tvDestination.setText("Địa điểm: (Không tìm thấy)");
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvTourTitle.setText("Tour ID: " + tourId);
                        tvDestination.setText("Lỗi tải tour");
                    });

            itemLayout.setOnClickListener(v -> listener.onRequestClick(doc));
        }
    }

    public static class CustomersInTourFragment extends Fragment {

        private RecyclerView rv;
        private TextView tvEmpty;
        private CustomersInTourAdapter adapter;
        private final List<User> customers = new ArrayList<>();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_customers_in_tour, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
            rv = v.findViewById(R.id.rvCustomers);
            tvEmpty = v.findViewById(R.id.tvEmpty);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new CustomersInTourAdapter(customers);
            rv.setAdapter(adapter);

            loadCustomers();
        }

        private void loadCustomers() {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1️⃣ Lấy danh sách tourId mà guide phụ trách
            db.collection("tours")
                    .whereArrayContains("guideIds", uid)
                    .get()
                    .addOnSuccessListener(tourSnap -> {
                        List<String> tourIds = new ArrayList<>();
                        for (var doc : tourSnap.getDocuments()) {
                            tourIds.add(doc.getId());
                        }
                        if (tourIds.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }

                        // 2️⃣ Lấy bookings confirmed trong các tour đó
                        db.collection("bookings")
                                .whereIn("tourId", tourIds)
                                .whereEqualTo("status", "confirmed")
                                .get()
                                .addOnSuccessListener(bookingSnap -> {
                                    List<String> userIds = new ArrayList<>();
                                    for (var doc : bookingSnap.getDocuments()) {
                                        Booking b = doc.toObject(Booking.class);
                                        if (b != null && b.getUserId() != null)
                                            userIds.add(b.getUserId());
                                    }
                                    if (userIds.isEmpty()) {
                                        tvEmpty.setVisibility(View.VISIBLE);
                                        return;
                                    }

                                    // 3️⃣ Lấy thông tin user tương ứng
                                    db.collection("users")
                                            .whereIn("id", userIds)
                                            .whereEqualTo("role", "customer")
                                            .get()
                                            .addOnSuccessListener(userSnap -> {
                                                customers.clear();
                                                for (var doc : userSnap.getDocuments()) {
                                                    User u = doc.toObject(User.class);
                                                    if (u != null) customers.add(u);
                                                }
                                                adapter.notifyDataSetChanged();
                                                tvEmpty.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
                                            });
                                });
                    });
        }
    }
}