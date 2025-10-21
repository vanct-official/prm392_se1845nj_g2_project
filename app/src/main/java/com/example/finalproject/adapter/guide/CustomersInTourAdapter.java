package com.example.finalproject.adapter.guide;

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

    private final List<User> customers;

    public CustomersInTourAdapter(List<User> customers) {
        this.customers = customers;
    }

    @NonNull
    @Override
    public CustomerVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_in_tour, parent, false);
        return new CustomerVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerVH holder, int position) {
        User u = customers.get(position);
        holder.tvName.setText(u.getFirstname() + " " + u.getLastname());
        holder.tvEmail.setText(u.getEmail());
        holder.tvPhone.setText(u.getPhone());
        if (u.getAvatarUrl() != null)
            Glide.with(holder.itemView.getContext()).load(u.getAvatarUrl()).into(holder.imgAvatar);
    }

    @Override
    public int getItemCount() {
        return customers.size();
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
