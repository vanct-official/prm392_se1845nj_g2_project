package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminPromotionsFragment extends Fragment {

    private LinearLayout promotionListContainer;

    public AdminPromotionsFragment() {
        // Required empty public constructor
    }

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
                        promotionListContainer.removeAllViews(); // Xóa cũ, load mới
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String name = doc.getString("name"); // ✅ Lấy tên khuyến mãi
                            String description = doc.getString("description");
                            Long discountLong = doc.getLong("discountPercent");
                            int discount = (discountLong != null) ? discountLong.intValue() : 0;
                            boolean isActive = Boolean.TRUE.equals(doc.getBoolean("isActive"));
                            Double minValueDouble = doc.getDouble("minimumValue");
                            double minValue = (minValueDouble != null) ? minValueDouble : 0;

                            Timestamp from = doc.getTimestamp("validFrom");
                            Timestamp to = doc.getTimestamp("validTo");
                            String validFrom = (from != null) ? from.toDate().toString() : "-";
                            String validTo = (to != null) ? to.toDate().toString() : "-";

                            String condition = "% Giảm " + discount + "% • Tối thiểu " + (int) minValue + "đ";
                            String status = isActive ? "Hoạt động" : "Tạm ngưng";

                            // ✅ Gọi hàm hiển thị thẻ
                            addPromotionCard(inflater, name != null ? name : doc.getId(), status, description, condition);
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

    // ✅ Hàm thêm từng thẻ khuyến mãi vào giao diện
    private void addPromotionCard(LayoutInflater inflater, String code, String status, String desc, String condition) {
        View itemView = inflater.inflate(R.layout.item_promotion_card, promotionListContainer, false);

        // Gán dữ liệu vào layout item_promotion_card.xml
        ((TextView) itemView.findViewById(R.id.tvCode)).setText(code);
        ((TextView) itemView.findViewById(R.id.tvStatus)).setText(status);
        ((TextView) itemView.findViewById(R.id.tvDescription)).setText(desc);
        ((TextView) itemView.findViewById(R.id.tvCondition)).setText(condition);

        // Sự kiện các nút
        itemView.findViewById(R.id.btnEdit).setOnClickListener(v ->
                Toast.makeText(getContext(), "Sửa " + code, Toast.LENGTH_SHORT).show());
        itemView.findViewById(R.id.btnView).setOnClickListener(v ->
                Toast.makeText(getContext(), "Xem " + code, Toast.LENGTH_SHORT).show());
        itemView.findViewById(R.id.btnDelete).setOnClickListener(v ->
                Toast.makeText(getContext(), "Xóa " + code, Toast.LENGTH_SHORT).show());

        promotionListContainer.addView(itemView);
    }
}
