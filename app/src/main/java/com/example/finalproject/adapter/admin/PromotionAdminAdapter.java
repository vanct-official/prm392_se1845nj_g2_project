package com.example.finalproject.adapter.admin;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PromotionAdminAdapter extends RecyclerView.Adapter<PromotionAdminAdapter.PromoViewHolder> {

    public interface OnPromotionActionListener {
        void onView(DocumentSnapshot doc);
        void onEdit(DocumentSnapshot doc);
        void onDelete(DocumentSnapshot doc);
    }

    private final Context context;
    private final OnPromotionActionListener listener;
    private List<DocumentSnapshot> promotions = new ArrayList<>();

    public PromotionAdminAdapter(Context context, List<DocumentSnapshot> promotions, OnPromotionActionListener listener) {
        this.context = context;
        this.promotions = new ArrayList<>(promotions);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion_card_admin, parent, false);
        return new PromoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromoViewHolder holder, int position) {
        if (promotions == null || promotions.isEmpty()) return;

        DocumentSnapshot doc = promotions.get(position);
        if (doc == null) return;

        String id = doc.getId();
        String name = doc.getString("name");
        String desc = doc.getString("description");
        boolean active = Boolean.TRUE.equals(doc.getBoolean("isActive"));
        Long discountObj = doc.getLong("discountPercent");
        Double minValueObj = doc.getDouble("minimumValue");
        String endDateStr = doc.getString("endDate");

        int discount = (discountObj != null) ? discountObj.intValue() : 0;
        double minValue = (minValueObj != null) ? minValueObj : 0;

        // ✅ Định dạng giá trị tiền
        String formattedMinValue = formatCurrency(minValue);

        // ✅ Hiển thị điều kiện với tiền tệ được format
        String condition = "Giảm " + discount + "% • Tối thiểu " + formattedMinValue;

        // --- Kiểm tra hết hạn ---
        boolean expired = false;
        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Date endDate = sdf.parse(endDateStr);
                java.util.Date today = new java.util.Date();
                if (endDate.before(today)) {
                    expired = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // --- Cập nhật giao diện ---
        String status;
        if (expired) {
            status = "Đã hết hạn";
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3CD"));
            holder.tvStatus.setTextColor(Color.parseColor("#B45309"));
        } else if (!active) {
            status = "Tạm ngưng";
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
            holder.tvStatus.setTextColor(Color.parseColor("#DC2626"));
        } else {
            status = "Hoạt động";
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvStatus.setTextColor(Color.parseColor("#16A34A"));
        }

        holder.tvCode.setText(name != null ? name : id);
        holder.tvDescription.setText(desc != null ? desc : "Không có mô tả");
        holder.tvCondition.setText(condition);
        holder.tvStatus.setText(status);

        holder.btnView.setOnClickListener(v -> listener.onView(doc));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(doc));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(doc));
    }

    @Override
    public int getItemCount() {
        return promotions != null ? promotions.size() : 0;
    }

    static class PromoViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvStatus, tvDescription, tvCondition;
        MaterialButton btnView, btnEdit, btnDelete;
        MaterialCardView cardView;

        public PromoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            btnView = itemView.findViewById(R.id.btnView);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            cardView = itemView.findViewById(R.id.cardPromotion);
        }
    }

    public void updateData(List<DocumentSnapshot> newList) {
        this.promotions = new ArrayList<>(newList);
        android.util.Log.d("PROMO_DEBUG", "Adapter cập nhật " + promotions.size() + " item");
        notifyDataSetChanged();
    }

    // ✅ Hàm định dạng tiền tệ Việt Nam
    private String formatCurrency(double value) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(value) + " đ";
    }
}
