package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.admin.AdminBookingAdapter;
import com.example.finalproject.entity.Booking;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private EditText edtSearchBooking;
    private AdminBookingAdapter adapter;
    private final List<Booking> bookingList = new ArrayList<>();
    private final List<Booking> filteredList = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_bookings, container, false);

        rvBookings = view.findViewById(R.id.rvBookings);
        edtSearchBooking = view.findViewById(R.id.edtSearchBooking);

        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminBookingAdapter(filteredList, getContext());
        rvBookings.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadBookings();

        edtSearchBooking.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBookings(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    /** 🔹 Lấy danh sách booking từ Firestore + join users & tours */
    private void loadBookings() {
        db.collection("bookings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    bookingList.clear();
                    filteredList.clear();

                    if (querySnapshot.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Booking b = doc.toObject(Booking.class);
                        b.setId(doc.getId());

                        // ✅ Lấy thông tin user để hiển thị
                        db.collection("users").document(b.getUserId()).get()
                                .addOnSuccessListener(userDoc -> {
                                    String customerName;
                                    if (userDoc.exists()) {
                                        String first = userDoc.getString("firstname");
                                        String last = userDoc.getString("lastname");
                                        customerName = (first != null ? first : "") + " " + (last != null ? last : "");
                                    } else {
                                        customerName = "N/A";
                                    }

                                    // ✅ Lấy tên tour từ tourId
                                    db.collection("tours").document(b.getTourId()).get()
                                            .addOnSuccessListener(tourDoc -> {
                                                String tourTitle = "N/A";
                                                if (tourDoc.exists()) {
                                                    tourTitle = tourDoc.getString("title");
                                                }

                                                // ✅ Tạo dòng log / hiển thị kết hợp mà không thay đổi Booking model
                                                Log.d("BookingInfo",
                                                        "BookingId: " + b.getId() +
                                                                " | Customer: " + customerName +
                                                                " | Tour: " + tourTitle);

                                                // ✅ Add vào danh sách hiển thị
                                                bookingList.add(b);
                                                filteredList.clear();
                                                filteredList.addAll(bookingList);
                                                adapter.notifyDataSetChanged();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(getContext(), "Lỗi lấy tour: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Lỗi lấy user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải booking: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** 🔹 Lọc danh sách booking theo từ khóa */
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

                // ✅ Chỉ còn userId, tourId, status, date để lọc
                if ((b.getUserId() != null && b.getUserId().toLowerCase().contains(q))
                        || (b.getTourId() != null && b.getTourId().toLowerCase().contains(q))
                        || (b.getStatus() != null && b.getStatus().toLowerCase().contains(q))
                        || dateString.toLowerCase().contains(q)) {
                    filteredList.add(b);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

}
