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
import com.example.finalproject.activity.AddPromotionActivity;
import com.example.finalproject.adapter.PromotionAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminPromotionsFragment extends Fragment {

    private RecyclerView recyclerPromotions;
    private EditText etSearch;
    private ProgressBar loadingProgress;
    private TextView tvAddPromotion;

    private FirebaseFirestore db;
    private PromotionAdapter adapter;
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

        adapter = new PromotionAdapter(getContext(), promotions, new PromotionAdapter.OnPromotionActionListener() {
            @Override
            public void onView(DocumentSnapshot doc) {
                Intent intent = new Intent(requireContext(), com.example.finalproject.activity.ViewPromotionActivity.class);
                intent.putExtra("promotionId", doc.getId());
                startActivity(intent);
            }

            @Override
            public void onEdit(DocumentSnapshot doc) {
                if (doc == null || doc.getId() == null) {
                    Toast.makeText(getContext(), "Không thể mở khuyến mãi này!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String promoId = doc.getId();
                android.util.Log.d("PROMO_DEBUG", "Đang mở sửa cho ID: " + promoId);

                try {
                    Intent intent = new Intent(getActivity(), com.example.finalproject.activity.EditPromotionActivity.class);
                    intent.putExtra("promotionId", promoId);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Lỗi khi mở trang sửa!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                confirmDelete(doc);
            }
        });

        recyclerPromotions.setAdapter(adapter);

        // 🔹 Load toàn bộ khuyến mãi ban đầu
        loadPromotions();

        // 🔹 Nút thêm mới
        tvAddPromotion.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddPromotionActivity.class);
            startActivityForResult(intent, 100); // mở trang thêm mới
        });

        // 🔹 Tìm kiếm realtime
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchPromotions(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    // ===========================================================
    // 🔥 LOAD DANH SÁCH KHUYẾN MÃI
    // ===========================================================
//    private void loadPromotions() {
//        loadingProgress.setVisibility(View.VISIBLE);
//
//        db.collection("promotions")
//                .orderBy("name", Query.Direction.ASCENDING)
//                .get()
//                .addOnSuccessListener(querySnapshot -> {
//                    promotions.clear();
//                    promotions.addAll(querySnapshot.getDocuments());
//                    adapter.notifyDataSetChanged();
//                    loadingProgress.setVisibility(View.GONE);
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    loadingProgress.setVisibility(View.GONE);
//                });
//    }

    private void loadPromotions() {
        if (loadingProgress != null)
            loadingProgress.setVisibility(View.VISIBLE);

        db.collection("promotions")
                .orderBy("name", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    promotions.clear();
                    promotions.addAll(querySnapshot.getDocuments());
                    adapter.notifyDataSetChanged();

                    if (loadingProgress != null)
                        loadingProgress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
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
            loadPromotions();
            return;
        }

        db.collection("promotions")
                .orderBy("name")
                .startAt(keyword)
                .endAt(keyword + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    promotions.clear();
                    promotions.addAll(querySnapshot.getDocuments());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tìm kiếm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    // ===========================================================
    // ❌ XÓA KHUYẾN MÃI
    // ===========================================================
    private void confirmDelete(DocumentSnapshot doc) {
        String name = doc.getString("name");

        new AlertDialog.Builder(getContext())
                .setTitle("Xóa khuyến mãi")
                .setMessage("Bạn có chắc muốn xóa \"" + name + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("promotions").document(doc.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                promotions.remove(doc);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(getContext(), "Đã xóa!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
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
