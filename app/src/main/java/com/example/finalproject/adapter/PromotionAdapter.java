package com.example.finalproject.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromoViewHolder> {

    public interface OnPromotionActionListener {
        void onView(DocumentSnapshot doc);
        void onEdit(DocumentSnapshot doc);
        void onDelete(DocumentSnapshot doc);
    }

    private final Context context;
    private final List<DocumentSnapshot> promotions;
    private final OnPromotionActionListener listener;

    public PromotionAdapter(Context context, List<DocumentSnapshot> promotions, OnPromotionActionListener listener) {
        this.context = context;
        this.promotions = promotions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion_card, parent, false);
        return new PromoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromoViewHolder holder, int position) {
        DocumentSnapshot doc = promotions.get(position);

        String id = doc.getId();
        String name = doc.getString("name");
        String desc = doc.getString("description");
        boolean active = Boolean.TRUE.equals(doc.getBoolean("isActive"));
        long discount = doc.getLong("discountPercent") != null ? doc.getLong("discountPercent") : 0;
        double minValue = doc.getDouble("minimumValue") != null ? doc.getDouble("minimumValue") : 0;

        String condition = "% Giảm " + discount + "% • Tối thiểu " + (int) minValue + "đ";
        String status = active ? "Hoạt động" : "Tạm ngưng";

        holder.tvCode.setText(name != null ? name : id);
        holder.tvStatus.setText(status);
        holder.tvDescription.setText(desc);
        holder.tvCondition.setText(condition);

        // Sự kiện nút
        holder.itemView.findViewById(R.id.btnEdit).setOnClickListener(v -> listener.onEdit(doc));
        holder.itemView.findViewById(R.id.btnView).setOnClickListener(v -> listener.onView(doc));
        holder.itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> listener.onDelete(doc));
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    static class PromoViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvStatus, tvDescription, tvCondition;

        public PromoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCondition = itemView.findViewById(R.id.tvCondition);
        }
    }
}
