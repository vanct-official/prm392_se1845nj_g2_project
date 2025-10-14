package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;

public class AdminPromotionsFragment extends Fragment {

    private LinearLayout promotionListContainer;

    public AdminPromotionsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_promotions, container, false);
        promotionListContainer = view.findViewById(R.id.promotionListContainer);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 🔥 Lấy dữ liệu từ Firestore
        db.collection("promotions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (var doc : queryDocumentSnapshots.getDocuments()) {
                            String id = doc.getId();
                            String description = doc.getString("description");
                            int discount = doc.getLong("discountPercent").intValue();
                            boolean isActive = Boolean.TRUE.equals(doc.getBoolean("isActive"));
                            double minValue = doc.getDouble("minimumValue");
                            String validFrom = doc.getTimestamp("validFrom").toDate().toString();
                            String validTo = doc.getTimestamp("validTo").toDate().toString();

                            String condition = "% Giảm " + discount + "% • Tối thiểu " + (int)minValue + "đ";
                            String status = isActive ? "Hoạt động" : "Tạm ngưng";

                            addPromotionCard(inflater, id, status, description, condition);
                        }
                    } else {
                        Toast.makeText(getContext(), "Không có khuyến mãi nào!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        return view;
    }


    private void addPromotionCard(LayoutInflater inflater, String code, String status, String desc, String condition) {
        View itemView = inflater.inflate(R.layout.item_promotion_card, promotionListContainer, false);

        ((TextView) itemView.findViewById(R.id.tvCode)).setText(code);
        ((TextView) itemView.findViewById(R.id.tvStatus)).setText(status);
        ((TextView) itemView.findViewById(R.id.tvDescription)).setText(desc);
        ((TextView) itemView.findViewById(R.id.tvCondition)).setText(condition);

        itemView.findViewById(R.id.btnEdit).setOnClickListener(v ->
                Toast.makeText(getContext(), "Sửa " + code, Toast.LENGTH_SHORT).show());
        itemView.findViewById(R.id.btnView).setOnClickListener(v ->
                Toast.makeText(getContext(), "Xem " + code, Toast.LENGTH_SHORT).show());
        itemView.findViewById(R.id.btnDelete).setOnClickListener(v ->
                Toast.makeText(getContext(), "Xóa " + code, Toast.LENGTH_SHORT).show());

        promotionListContainer.addView(itemView);
    }
}
