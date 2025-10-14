package com.example.finalproject.adapter;

import android.app.AlertDialog;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> promotions;

    public PromotionAdapter(Context context, List<DocumentSnapshot> promotions) {
        this.context = context;
        this.promotions = promotions;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion_card, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        DocumentSnapshot doc = promotions.get(position);

        String id = doc.getId();
        String name = doc.getString("name");
        String description = doc.getString("description");
        int discount = doc.getLong("discountPercent") != null ? doc.getLong("discountPercent").intValue() : 0;
        boolean isActive = Boolean.TRUE.equals(doc.getBoolean("isActive"));
        double minValue = doc.getDouble("minimumValue") != null ? doc.getDouble("minimumValue") : 0;

        String condition = "% Giảm " + discount + "% • Tối thiểu " + (int) minValue + "đ";
        String status = isActive ? "Hoạt động" : "Tạm ngưng";

        holder.tvCode.setText(name != null ? name : id);
        holder.tvStatus.setText(status);
        holder.tvDescription.setText(description);
        holder.tvCondition.setText(condition);

        // 🔹 Nút Xóa có xác nhận
        holder.itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa \"" + (name != null ? name : id) + "\" không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        FirebaseFirestore.getInstance().collection("promotions")
                                .document(id)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    promotions.remove(position);
                                    notifyItemRemoved(position);
                                    Toast.makeText(context, "Đã xóa " + (name != null ? name : id), Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvStatus, tvDescription, tvCondition;
        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCondition = itemView.findViewById(R.id.tvCondition);
        }
    }
}
