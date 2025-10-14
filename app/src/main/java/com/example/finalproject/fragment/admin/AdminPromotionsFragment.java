package com.example.finalproject.fragment.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.finalproject.adapter.PromotionAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminPromotionsFragment extends Fragment {

    private RecyclerView recyclerPromotions;
    private EditText etSearch;
    private ProgressBar loadingProgress;
    private FirebaseFirestore db;
    private PromotionAdapter adapter;
    private List<DocumentSnapshot> promotions = new ArrayList<>();

    public AdminPromotionsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_promotions, container, false);

        recyclerPromotions = view.findViewById(R.id.recyclerPromotions);
        etSearch = view.findViewById(R.id.etSearch);
        loadingProgress = view.findViewById(R.id.loadingMoreProgress);
        TextView tvAddPromotion = view.findViewById(R.id.tvAddPromotion);

        recyclerPromotions.setLayoutManager(new LinearLayoutManager(getContext()));
        db = FirebaseFirestore.getInstance();

        adapter = new PromotionAdapter(getContext(), promotions, new PromotionAdapter.OnPromotionActionListener() {
            @Override
            public void onView(DocumentSnapshot doc) {
                Toast.makeText(getContext(), "Xem " + doc.getString("name"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEdit(DocumentSnapshot doc) {
                Toast.makeText(getContext(), "Sửa " + doc.getString("name"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                confirmDelete(doc);
            }
        });
        recyclerPromotions.setAdapter(adapter);

        // 🔹 Load toàn bộ khuyến mãi khi khởi động
        loadAllPromotions();

        // 🔹 Nút thêm mới
        tvAddPromotion.setOnClickListener(v -> showAddPromotionDialog());

        // 🔹 Tìm kiếm realtime
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchPromotions(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    // ===========================================================
    // 🔥 Load toàn bộ khuyến mãi
    // ===========================================================
    private void loadAllPromotions() {
        loadingProgress.setVisibility(View.VISIBLE);
        db.collection("promotions")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    promotions.clear();
                    promotions.addAll(qs.getDocuments());
                    adapter.notifyDataSetChanged();
                    loadingProgress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingProgress.setVisibility(View.GONE);
                });
    }

    // ===========================================================
    // 🔹 Xóa khuyến mãi
    // ===========================================================
    private void confirmDelete(DocumentSnapshot doc) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa khuyến mãi")
                .setMessage("Bạn có chắc muốn xóa \"" + doc.getString("name") + "\" không?")
                .setPositiveButton("Xóa", (d, w) -> {
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
    // 🔍 Tìm kiếm khuyến mãi
    // ===========================================================
    private void searchPromotions(String keyword) {
        if (keyword.isEmpty()) {
            loadAllPromotions();
            return;
        }

        db.collection("promotions")
                .orderBy("name")
                .startAt(keyword)
                .endAt(keyword + "\uf8ff")
                .get()
                .addOnSuccessListener(qs -> {
                    promotions.clear();
                    promotions.addAll(qs.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    // ===========================================================
    // ➕ Hiển thị form thêm khuyến mãi
    // ===========================================================
    private void showAddPromotionDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_promotion, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        EditText etCode = dialogView.findViewById(R.id.etCode);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        EditText etDiscount = dialogView.findViewById(R.id.etDiscount);
        EditText etMinValue = dialogView.findViewById(R.id.etMinValue);
        EditText etFromDate = dialogView.findViewById(R.id.etFromDate);
        EditText etToDate = dialogView.findViewById(R.id.etToDate);
        CheckBox cbActive = dialogView.findViewById(R.id.cbActive);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);

        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            String name = etCode.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String discountStr = etDiscount.getText().toString().trim();
            String minValueStr = etMinValue.getText().toString().trim();
            String fromStr = etFromDate.getText().toString().trim();
            String toStr = etToDate.getText().toString().trim();
            boolean isActive = cbActive.isChecked();

            // 🛑 Kiểm tra trống
            if (name.isEmpty() || desc.isEmpty() || discountStr.isEmpty() ||
                    minValueStr.isEmpty() || fromStr.isEmpty() || toStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            int discount;
            double minValue;

            try {
                discount = Integer.parseInt(discountStr);
                minValue = Double.parseDouble(minValueStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Giá trị nhập không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🛑 Kiểm tra giá trị hợp lệ
            if (discount < 1 || discount >= 100) {
                Toast.makeText(getContext(), "Phần trăm giảm giá phải từ 1 đến dưới 100!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (minValue <= 0) {
                Toast.makeText(getContext(), "Giá trị tối thiểu phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                Date fromDate = sdf.parse(fromStr);
                Date toDate = sdf.parse(toStr);

                // 🛑 Kiểm tra ngày hợp lệ
                if (fromDate.after(toDate)) {
                    Toast.makeText(getContext(), "Ngày bắt đầu phải nhỏ hơn ngày kết thúc!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 🛑 Kiểm tra trùng name
                db.collection("promotions").document(name).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                Toast.makeText(getContext(), "Tên mã khuyến mãi đã tồn tại!", Toast.LENGTH_SHORT).show();
                            } else {
                                Map<String, Object> promotion = new HashMap<>();
                                promotion.put("name", name);
                                promotion.put("description", desc);
                                promotion.put("discountPercent", discount);
                                promotion.put("minimumValue", minValue);
                                promotion.put("isActive", isActive);
                                promotion.put("validFrom", new Timestamp(fromDate));
                                promotion.put("validTo", new Timestamp(toDate));

                                db.collection("promotions").document(name)
                                        .set(promotion)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Thêm khuyến mãi thành công!", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                            loadAllPromotions();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(getContext(), "Lỗi khi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        });
            } catch (ParseException e) {
                Toast.makeText(getContext(), "Định dạng ngày không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(getContext(),
                (view, year, month, day) -> {
                    String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                    target.setText(dateStr);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)).show();
    }
}
