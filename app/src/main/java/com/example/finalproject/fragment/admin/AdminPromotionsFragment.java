package com.example.finalproject.fragment.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.List;

public class AdminPromotionsFragment extends Fragment {

    private LinearLayout promotionListContainer;
    private EditText etSearch;
    private List<DocumentSnapshot> allPromotions = new ArrayList<>();

    public AdminPromotionsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_promotions, container, false);
        promotionListContainer = view.findViewById(R.id.promotionListContainer);
        etSearch = view.findViewById(R.id.etSearch);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 🔥 Lấy dữ liệu Firestore
        db.collection("promotions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        allPromotions.clear();
                        allPromotions.addAll(queryDocumentSnapshots.getDocuments());
                        showPromotions(inflater, allPromotions);
                    } else {
                        Toast.makeText(getContext(), "Không có khuyến mãi nào!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        // 🔍 Lọc realtime khi người dùng nhập
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPromotions(inflater, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    // 🧩 Hiển thị danh sách
    private void showPromotions(LayoutInflater inflater, List<DocumentSnapshot> list) {
        promotionListContainer.removeAllViews();
        for (DocumentSnapshot doc : list) {
            addPromotionCard(inflater, doc);
        }
    }

    // 🔍 Tìm kiếm realtime
    private void filterPromotions(LayoutInflater inflater, String keyword) {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        keyword = keyword.toLowerCase();

        for (DocumentSnapshot doc : allPromotions) {
            String name = doc.getString("name") != null ? doc.getString("name").toLowerCase() : "";
            String description = doc.getString("description") != null ? doc.getString("description").toLowerCase() : "";

            if (name.contains(keyword) || description.contains(keyword)) {
                filtered.add(doc);
            }
        }

        showPromotions(inflater, filtered);
    }

    // 🧱 Tạo từng thẻ khuyến mãi
    private void addPromotionCard(LayoutInflater inflater, DocumentSnapshot doc) {
        View itemView = inflater.inflate(R.layout.item_promotion_card, promotionListContainer, false);

        String id = doc.getId();
        String name = doc.getString("name");
        String description = doc.getString("description");
        int discount = doc.getLong("discountPercent") != null ? doc.getLong("discountPercent").intValue() : 0;
        boolean isActive = Boolean.TRUE.equals(doc.getBoolean("isActive"));
        double minValue = doc.getDouble("minimumValue") != null ? doc.getDouble("minimumValue") : 0;

        Timestamp from = doc.getTimestamp("validFrom");
        Timestamp to = doc.getTimestamp("validTo");
        String validFrom = (from != null) ? from.toDate().toString() : "-";
        String validTo = (to != null) ? to.toDate().toString() : "-";

        String condition = "% Giảm " + discount + "% • Tối thiểu " + (int) minValue + "đ";
        String status = isActive ? "Hoạt động" : "Tạm ngưng";

        ((TextView) itemView.findViewById(R.id.tvCode)).setText(name != null ? name : id);
        ((TextView) itemView.findViewById(R.id.tvStatus)).setText(status);
        ((TextView) itemView.findViewById(R.id.tvDescription)).setText(description);
        ((TextView) itemView.findViewById(R.id.tvCondition)).setText(condition);

        // 🟣 Nút Sửa
        itemView.findViewById(R.id.btnEdit).setOnClickListener(v ->
                Toast.makeText(getContext(), "Sửa " + (name != null ? name : id), Toast.LENGTH_SHORT).show());

        // 🟣 Nút Xem
        itemView.findViewById(R.id.btnView).setOnClickListener(v ->
                Toast.makeText(getContext(), "Xem " + (name != null ? name : id), Toast.LENGTH_SHORT).show());

        // 🟣 Nút Xóa (có hộp thoại xác nhận)
        itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Xác nhận xóa");
            builder.setMessage("Bạn có chắc muốn xóa khuyến mãi \"" + (name != null ? name : id) + "\" không?");
            builder.setPositiveButton("Xóa", (dialog, which) -> {
                FirebaseFirestore.getInstance().collection("promotions")
                        .document(doc.getId())
                        .delete()
                        .addOnSuccessListener(unused -> {
                            promotionListContainer.removeView(itemView);
                            Toast.makeText(getContext(), "Đã xóa " + (name != null ? name : id), Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
            builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
            builder.show();
        });

        promotionListContainer.addView(itemView);
    }
}
