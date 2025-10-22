package com.example.finalproject.adapter.guide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.example.finalproject.entity.User;

import java.util.List;

public class CustomersInTourAdapter extends RecyclerView.Adapter<CustomersInTourAdapter.CustomerVH> {

    private final Context context;
    private final List<User> customers;

    // ✅ Constructor mới nhận thêm Context
    public CustomersInTourAdapter(Context context, List<User> customers) {
        this.context = context;
        this.customers = customers;
    }

    @NonNull
    @Override
    public CustomerVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_customer_in_tour, parent, false);
        return new CustomerVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerVH holder, int position) {
        User u = customers.get(position);
        holder.tvName.setText(u.getFirstname() + " " + u.getLastname());
        holder.tvEmail.setText(u.getEmail());
        holder.tvPhone.setText(u.getPhone());

        // ✅ Load avatar (nếu có)
        if (u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(u.getAvatarUrl())
                    .placeholder(R.drawable.ic_user_placeholder) // ảnh mặc định nếu null
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return customers != null ? customers.size() : 0;
    }

    static class CustomerVH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvEmail, tvPhone;

        public CustomerVH(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
        }
    }
}
