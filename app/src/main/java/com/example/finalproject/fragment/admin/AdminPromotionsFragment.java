package com.example.finalproject.fragment.admin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.activity.admin.AddPromotionAdminActivity;
import com.example.finalproject.activity.admin.EditPromotionAdminActivity;
import com.example.finalproject.activity.admin.ViewPromotionAdminActivity;
import com.example.finalproject.adapter.admin.PromotionAdminAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminPromotionsFragment extends Fragment {
    private RecyclerView recyclerPromotions;
    private EditText etSearch;
    private ProgressBar loadingProgress;
    private TextView tvAddPromotion;
    private FirebaseFirestore db;
    private PromotionAdminAdapter adapter;
    private List<DocumentSnapshot> allPromotions = new ArrayList<>();
    private List<DocumentSnapshot> promotions = new ArrayList<>();

    public AdminPromotionsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_promotions, container, false);

        recyclerPromotions = view.findViewById(R.id.recyclerPromotions);
        etSearch = view.findViewById(R.id.etSearch);
        loadingProgress = view.findViewById(R.id.loadingMoreProgress);
        tvAddPromotion = view.findViewById(R.id.tvAddPromotion);

        recyclerPromotions.setLayoutManager(new LinearLayoutManager(getContext()));
        db = FirebaseFirestore.getInstance();

        adapter = new PromotionAdminAdapter(getContext(), promotions, new PromotionAdminAdapter.OnPromotionActionListener() {
            @Override
            public void onView(DocumentSnapshot doc) {
                Intent intent = new Intent(requireContext(), ViewPromotionAdminActivity.class);
                intent.putExtra("promotionId", doc.getId());
                startActivity(intent);
            }

            @Override
            public void onEdit(DocumentSnapshot doc) {
                Intent intent = new Intent(requireContext(), EditPromotionAdminActivity.class);
                intent.putExtra("promotionId", doc.getId());
                startActivity(intent);
            }


            @Override
            public void onDelete(DocumentSnapshot doc) {
                confirmDelete(doc);
            }
        });

        recyclerPromotions.setAdapter(adapter);

        // 🔥 Gọi load dữ liệu ngay khi tạo view
        loadPromotions();

        // ✅ Nút "+ Thêm mới"
        tvAddPromotion.setOnClickListener(v -> {
            android.util.Log.d("PROMO_DEBUG", "👉 Nút 'Thêm mới' được bấm!");
            Intent intent = new Intent(getContext(), AddPromotionAdminActivity.class);
            startActivityForResult(intent, 100);
        });

        // 🔹 Tìm kiếm realtime
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchPromotions(s.toString().trim());
            }
        });

        return view;
    }


    // ===========================================================
    // 🔥 LOAD DANH SÁCH KHUYẾN MÃI
    // ===========================================================
    private void loadPromotions() {
        if (loadingProgress != null)
            loadingProgress.setVisibility(View.VISIBLE);

        db.collection("promotions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    android.util.Log.d("PROMO_DEBUG", "🔥 Tổng số khuyến mãi trong Firestore: " + count);

                    allPromotions.clear();
                    promotions.clear();

                    if (count > 0) {
                        allPromotions.addAll(querySnapshot.getDocuments());
                        promotions.addAll(allPromotions);
                        adapter.updateData(promotions);
                    } else {
                        Toast.makeText(getContext(), "Không có dữ liệu khuyến mãi trong Firestore!", Toast.LENGTH_SHORT).show();
                    }

                    if (loadingProgress != null)
                        loadingProgress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PROMO_DEBUG", "❌ Lỗi Firestore: " + e.getMessage());
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    if (loadingProgress != null)
                        loadingProgress.setVisibility(View.GONE);
                });
    }


    // ===========================================================
    // 🔍 TÌM KIẾM KHUYẾN MÃI
    // ===========================================================
    private void searchPromotions(String keyword) {
        if (keyword.isEmpty()) {
            promotions.clear();
            promotions.addAll(allPromotions);
            adapter.updateData(promotions);
            return;
        }

        String lowerKeyword = keyword.toLowerCase();
        List<DocumentSnapshot> filteredList = new ArrayList<>();

        for (DocumentSnapshot doc : allPromotions) {
            String name = doc.getString("name");
            String desc = doc.getString("description");

            if ((name != null && name.toLowerCase().contains(lowerKeyword)) ||
                    (desc != null && desc.toLowerCase().contains(lowerKeyword))) {
                filteredList.add(doc);
            }
        }

        promotions.clear();
        promotions.addAll(filteredList);
        adapter.updateData(promotions);
    }


    // ===========================================================
    // ❌ XÓA KHUYẾN MÃI
    // ===========================================================
//    private void confirmDelete(DocumentSnapshot doc) {
//        String name = doc.getString("name");
//
//        new AlertDialog.Builder(getContext())
//                .setTitle("Xóa khuyến mãi")
//                .setMessage("Bạn có chắc muốn xóa \"" + name + "\" không?")
//                .setPositiveButton("Xóa", (dialog, which) -> {
//                    db.collection("promotions").document(doc.getId())
//                            .delete()
//                            .addOnSuccessListener(aVoid -> {
//                                // ✅ Xóa đúng cách dựa vào id
//                                String deletedId = doc.getId();
//                                promotions.removeIf(p -> p.getId().equals(deletedId));
//                                allPromotions.removeIf(p -> p.getId().equals(deletedId));
//
//                                adapter.updateData(promotions);
//
//                                Toast.makeText(getContext(), "Đã xóa!", Toast.LENGTH_SHORT).show();
//                            })
//                            .addOnFailureListener(e ->
//                                    Toast.makeText(getContext(), "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//                })
//                .setNegativeButton("Hủy", null)
//                .show();
//    }

    // ===========================================================
    // ❌ XÓA KHUYẾN MÃI — CÓ KIỂM TRA RÀNG BUỘC VỚI BOOKINGS
    // ===========================================================
    private void confirmDelete(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String promoId = doc.getId();

        // Bước 1: kiểm tra xem có booking nào đang dùng promotionId này không
        db.collection("bookings")
                .whereEqualTo("promotionId", promoId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Có ít nhất 1 booking đang dùng -> không cho xóa
                        new AlertDialog.Builder(getContext())
                                .setTitle("Không thể xóa")
                                .setMessage("Khuyến mãi \"" + name + "\" đang được áp dụng. Không thể xóa")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        // Không có booking nào dùng -> cho phép xóa
                        new AlertDialog.Builder(getContext())
                                .setTitle("Xóa khuyến mãi")
                                .setMessage("Bạn có chắc muốn xóa \"" + name + "\" không?")
                                .setPositiveButton("Xóa", (dialog, which) -> {
                                    db.collection("promotions").document(promoId)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                // ✅ Xóa thành công
                                                promotions.removeIf(p -> p.getId().equals(promoId));
                                                allPromotions.removeIf(p -> p.getId().equals(promoId));
                                                adapter.updateData(promotions);

                                                Toast.makeText(getContext(), "Đã xóa!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(getContext(), "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi kiểm tra bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ===========================================================
    // ⏪ NHẬN KẾT QUẢ SAU KHI THÊM MỚI
    // ===========================================================
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            loadPromotions(); // reload dữ liệu

            View rootView = getView();
            if (rootView != null) {
                Snackbar.make(rootView, "Thêm khuyến mãi mới thành công!", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.purple_500))
                        .setTextColor(getResources().getColor(android.R.color.white))
                        .show();
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        loadPromotions(); // 🔥 Tự động reload mỗi khi quay lại màn hình
    }

}
