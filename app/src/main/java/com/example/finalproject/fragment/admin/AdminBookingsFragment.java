package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private EditText edtSearchBooking;
    private BookingAdapter adapter;
    private final List<Booking> bookingList = new ArrayList<>();
    private final List<Booking> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_bookings_demo, container, false);
        rvBookings = view.findViewById(R.id.rvBookings);
        edtSearchBooking = view.findViewById(R.id.edtSearchBooking);

        // Setup RecyclerView
        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingAdapter(filteredList);
        rvBookings.setAdapter(adapter);

        // Tạo fake data
        createFakeBookings();

        // Ban đầu filtered = full list
        filteredList.clear();
        filteredList.addAll(bookingList);
        adapter.notifyDataSetChanged();

        // Search filter (live)
        edtSearchBooking.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBookings(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    // Tạo dữ liệu mẫu
    private void createFakeBookings() {
        bookingList.clear();
        // thời gian hiện tại + mô tả
        String now = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        bookingList.add(new Booking("Nguyễn Văn A", "Hành trình Hà Nội - Hạ Long", now, BookingStatus.PENDING));
        bookingList.add(new Booking("Trần Thị B", "Sapa 3N2Đ", "26/10/2025 08:00", BookingStatus.CONFIRMED));
        bookingList.add(new Booking("Lê Văn C", "Ninh Bình - Tràng An", "27/10/2025 09:30", BookingStatus.CANCELLED));
        bookingList.add(new Booking("Phạm Thị D", "Phú Quốc 4N3Đ", "28/10/2025 07:00", BookingStatus.PENDING));
        bookingList.add(new Booking("Hoàng Văn E", "Đà Nẵng - Hội An", "30/10/2025 14:00", BookingStatus.CONFIRMED));
    }

    // Lọc theo tên khách hoặc tên tour
    private void filterBookings(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(bookingList);
        } else {
            String q = query.toLowerCase(Locale.getDefault());
            for (Booking b : bookingList) {
                if (b.customerName.toLowerCase(Locale.getDefault()).contains(q)
                        || b.tourName.toLowerCase(Locale.getDefault()).contains(q)) {
                    filteredList.add(b);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- Model + Adapter ---
    private static class Booking {
        String customerName;
        String tourName;
        String dateTime;
        BookingStatus status;

        Booking(String customerName, String tourName, String dateTime, BookingStatus status) {
            this.customerName = customerName;
            this.tourName = tourName;
            this.dateTime = dateTime;
            this.status = status;
        }
    }

    private enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED
    }

    private class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

        private final List<Booking> items;

        BookingAdapter(List<Booking> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_booking_demo, parent, false);
            return new BookingViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
            Booking b = items.get(position);
            holder.tvCustomerName.setText(b.customerName);
            holder.tvTourName.setText(b.tourName);
            holder.tvDate.setText("🕒 " + b.dateTime);

            // Trạng thái hiển thị màu sắc khác nhau
            switch (b.status) {
                case CONFIRMED:
                    holder.tvStatus.setText("Trạng thái: Đã xác nhận");
                    holder.tvStatus.setTextColor(getResources().getColor(R.color.status_confirmed, null));
                    holder.btnConfirm.setVisibility(View.GONE);
                    holder.btnCancel.setVisibility(View.VISIBLE);
                    break;
                case CANCELLED:
                    holder.tvStatus.setText("Trạng thái: Đã huỷ");
                    holder.tvStatus.setTextColor(getResources().getColor(R.color.status_cancelled, null));
                    holder.btnConfirm.setVisibility(View.VISIBLE);
                    holder.btnCancel.setVisibility(View.GONE);
                    break;
                case PENDING:
                default:
                    holder.tvStatus.setText("Trạng thái: Chờ xử lý");
                    holder.tvStatus.setTextColor(getResources().getColor(R.color.status_pending, null));
                    holder.btnConfirm.setVisibility(View.VISIBLE);
                    holder.btnCancel.setVisibility(View.VISIBLE);
                    break;
            }

            // Avatar giả (dùng drawable placeholder nếu có)
            holder.ivAvatar.setImageResource(R.drawable.ic_account);

            // Nút hành động giả
            holder.btnConfirm.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Xác nhận booking của " + b.customerName, Toast.LENGTH_SHORT).show();
                // cập nhật view giả (không thay đổi dữ liệu gốc trừ khi muốn)
                b.status = BookingStatus.CONFIRMED;
                notifyItemChanged(position);
            });

            holder.btnCancel.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Huỷ booking của " + b.customerName, Toast.LENGTH_SHORT).show();
                b.status = BookingStatus.CANCELLED;
                notifyItemChanged(position);
            });

            // Khi bấm item mở chi tiết (demo)
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(getContext(), b.customerName + " — " + b.tourName, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class BookingViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvCustomerName, tvTourName, tvDate, tvStatus;
            Button btnConfirm, btnCancel;

            BookingViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvCustomerName = itemView.findViewById(R.id.tvBookingName);
                tvTourName = itemView.findViewById(R.id.tvBookingTour);
                tvDate = itemView.findViewById(R.id.tvBookingDate);
                tvStatus = itemView.findViewById(R.id.tvBookingStatus);
                btnConfirm = itemView.findViewById(R.id.btnConfirm);
                btnCancel = itemView.findViewById(R.id.btnCancel);
            }
        }
    }
}
