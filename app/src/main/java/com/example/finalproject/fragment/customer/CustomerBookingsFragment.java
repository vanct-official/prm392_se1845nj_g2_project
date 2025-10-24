package com.example.finalproject.fragment.customer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;

import java.util.ArrayList;
import java.util.List;

public class CustomerBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private EditText edtSearchBooking;
    private BookingAdapter adapter;
    private final List<Booking> bookingList = new ArrayList<>();
    private final List<Booking> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_customer_bookings_demo, container, false);
        rvBookings = view.findViewById(R.id.rvBookingsCustomer);
        edtSearchBooking = view.findViewById(R.id.edtSearchCustomerBooking);

        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingAdapter(filteredList);
        rvBookings.setAdapter(adapter);

        // Fake data
        bookingList.clear();
        bookingList.add(new Booking("Hạ Long - 3 ngày 2 đêm", "26/10/2025 08:00", BookingStatus.UPCOMING));
        bookingList.add(new Booking("Sapa Adventure", "27/10/2025 09:30", BookingStatus.UPCOMING));
        bookingList.add(new Booking("Đà Nẵng - Hội An", "28/10/2025 14:00", BookingStatus.COMPLETED));
        bookingList.add(new Booking("Phú Quốc Beach Tour", "30/10/2025 07:00", BookingStatus.CANCELLED));

        filteredList.clear();
        filteredList.addAll(bookingList);
        adapter.notifyDataSetChanged();

        // Filter tìm kiếm
        edtSearchBooking.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBookings(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void filterBookings(String query) {
        filteredList.clear();
        if(query == null || query.trim().isEmpty()){
            filteredList.addAll(bookingList);
        } else {
            String q = query.toLowerCase();
            for(Booking b : bookingList){
                if(b.tourName.toLowerCase().contains(q)){
                    filteredList.add(b);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- Model + Adapter ---
    private static class Booking {
        String tourName;
        String dateTime;
        BookingStatus status;

        Booking(String tourName, String dateTime, BookingStatus status){
            this.tourName = tourName;
            this.dateTime = dateTime;
            this.status = status;
        }
    }

    private enum BookingStatus { UPCOMING, COMPLETED, CANCELLED }

    private class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {
        List<Booking> items;
        BookingAdapter(List<Booking> items){ this.items = items; }

        @NonNull
        @Override
        public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_booking_demo, parent, false);
            return new BookingViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
            Booking b = items.get(position);
            holder.tvTourName.setText(b.tourName);
            holder.tvDate.setText("🕒 " + b.dateTime);

            switch (b.status){
                case UPCOMING:
                    holder.tvStatus.setText("Trạng thái: Sắp tới");
                    holder.tvStatus.setTextColor(getResources().getColor(R.color.status_pending, null));
                    break;
                case COMPLETED:
                    holder.tvStatus.setText("Trạng thái: Đã hoàn thành");
                    holder.tvStatus.setTextColor(getResources().getColor(R.color.status_confirmed, null));
                    break;
                case CANCELLED:
                    holder.tvStatus.setText("Trạng thái: Đã huỷ");
                    holder.tvStatus.setTextColor(getResources().getColor(R.color.status_cancelled, null));
                    break;
            }

            holder.ivAvatar.setImageResource(R.drawable.ic_account);
        }

        @Override
        public int getItemCount() { return items.size(); }

        class BookingViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvTourName, tvDate, tvStatus;

            BookingViewHolder(@NonNull View itemView){
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatarCustomer);
                tvTourName = itemView.findViewById(R.id.tvTourNameCustomer);
                tvDate = itemView.findViewById(R.id.tvDateCustomer);
                tvStatus = itemView.findViewById(R.id.tvStatusCustomer);
            }
        }
    }
}
