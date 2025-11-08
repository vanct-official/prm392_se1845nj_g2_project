package com.example.finalproject.fragment.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.activity.customer.BookingDetailActivity;
import com.example.finalproject.adapter.customer.CustomerBookingAdapter;
import com.example.finalproject.entity.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private EditText edtSearchBooking;
    private CustomerBookingAdapter adapter;
    private final List<Booking> bookingList = new ArrayList<>();
    private final List<Booking> filteredList = new ArrayList<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_customer_bookings_demo, container, false);
        rvBookings = view.findViewById(R.id.rvBookingsCustomer);
        edtSearchBooking = view.findViewById(R.id.edtSearchCustomerBooking);

        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CustomerBookingAdapter(filteredList);
        rvBookings.setAdapter(adapter);

        loadBookings();

        // 🔍 Lọc tìm kiếm
        edtSearchBooking.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBookings(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    // ✅ Lấy danh sách booking thật của user hiện tại
    private void loadBookings() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("bookings")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    bookingList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Booking booking = doc.toObject(Booking.class);
                        booking.setId(doc.getId());
                        bookingList.add(booking);
                    }

                    filteredList.clear();
                    filteredList.addAll(bookingList);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải booking: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ✅ Lọc theo tên tour, trạng thái hoặc ngày
    private void filterBookings(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(bookingList);
        } else {
            String q = query.toLowerCase(Locale.getDefault());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            for (Booking b : bookingList) {
                String dateString = "";
                if (b.getCreateAt() != null) {
                    dateString = sdf.format(b.getCreateAt().toDate());
                }

                // So khớp tourId hoặc trạng thái
                if ((b.getTourId() != null && b.getTourId().toLowerCase().contains(q))
                        || (b.getStatus() != null && b.getStatus().toLowerCase().contains(q))
                        || dateString.toLowerCase().contains(q)) {
                    filteredList.add(b);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
