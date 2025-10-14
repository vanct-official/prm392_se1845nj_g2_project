package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.PromotionAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminPromotionsFragment extends Fragment {

    private RecyclerView recyclerPromotions;
    private EditText etSearch;
    private PromotionAdapter adapter;
    private List<DocumentSnapshot> allPromotions = new ArrayList<>();

    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible; // 🔹 lưu document cuối của mỗi trang
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_promotions, container, false);

        recyclerPromotions = view.findViewById(R.id.recyclerPromotions);
        etSearch = view.findViewById(R.id.etSearch);
        recyclerPromotions.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        adapter = new PromotionAdapter(getContext(), allPromotions);
        recyclerPromotions.setAdapter(adapter);

        loadPromotions(null); // 🔥 Load trang đầu tiên

        // 🔍 Tìm kiếm realtime
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPromotions(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 🔁 Lắng nghe khi cuộn để load thêm
        recyclerPromotions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1) && !isLoading && !isLastPage) {
                    loadPromotions(lastVisible); // 🔁 Load thêm khi cuộn tới cuối
                }
            }
        });

        return view;
    }

    // 🔹 Load dữ liệu phân trang
    private void loadPromotions(DocumentSnapshot startAfterDoc) {
        isLoading = true;
        Query query = db.collection("promotions")
                .orderBy("name")
                .limit(4);

        if (startAfterDoc != null) query = query.startAfter(startAfterDoc);

        query.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.isEmpty()) {
                if (snapshot.size() < 10) isLastPage = true;
                lastVisible = snapshot.getDocuments().get(snapshot.size() - 1);
                allPromotions.addAll(snapshot.getDocuments());
                adapter.notifyDataSetChanged();
            } else {
                isLastPage = true;
            }
            isLoading = false;
        }).addOnFailureListener(e -> {
            isLoading = false;
            Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // 🔍 Lọc danh sách theo từ khóa (local)
    private void filterPromotions(String keyword) {
        keyword = keyword.toLowerCase();
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allPromotions) {
            String name = doc.getString("name") != null ? doc.getString("name").toLowerCase() : "";
            String desc = doc.getString("description") != null ? doc.getString("description").toLowerCase() : "";
            if (name.contains(keyword) || desc.contains(keyword)) filtered.add(doc);
        }
        adapter = new PromotionAdapter(getContext(), filtered);
        recyclerPromotions.setAdapter(adapter);
    }
}
