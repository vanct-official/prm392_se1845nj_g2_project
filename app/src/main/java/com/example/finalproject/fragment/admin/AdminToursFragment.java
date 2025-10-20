package com.example.finalproject.fragment.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.activity.admin.AddTourAdminActivity;
import com.example.finalproject.activity.admin.EditTourAdminActivity;
import com.example.finalproject.adapter.admin.TourAdminAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminToursFragment extends Fragment {

    private RecyclerView recyclerTours;
    private TextView tvAddTour;
    private ProgressBar progressBar;
    private SearchView searchView;
    private FirebaseFirestore db;
    private TourAdminAdapter adapter;
    private final List<DocumentSnapshot> tours = new ArrayList<>();
    private final List<DocumentSnapshot> allTours = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_tours, container, false);

        recyclerTours = view.findViewById(R.id.recyclerTours);
        tvAddTour = view.findViewById(R.id.tvAddTour);
        progressBar = view.findViewById(R.id.progressBar);
        searchView = view.findViewById(R.id.searchView);

        db = FirebaseFirestore.getInstance();
        recyclerTours.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TourAdminAdapter(requireContext(), tours, new TourAdminAdapter.OnTourActionListener() {
            @Override
            public void onEdit(DocumentSnapshot doc) {
                Intent intent = new Intent(getContext(), EditTourAdminActivity.class);
                intent.putExtra("tourId", doc.getId());
                startActivity(intent);
            }

            @Override
            public void onView(DocumentSnapshot doc) {
                Toast.makeText(getContext(), "Tour: " + doc.getString("title"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                confirmDelete(doc);
            }
        });

        recyclerTours.setAdapter(adapter);
        tvAddTour.setOnClickListener(v -> startActivity(new Intent(requireActivity(), AddTourAdminActivity.class)));

        setupSearchView();
        loadTours();

        return view;
    }

    // ===========================================================
    // 🔎 Tìm kiếm
    // ===========================================================
    private void setupSearchView() {
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTours(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTours(newText);
                return true;
            }
        });
    }

    // ===========================================================
    // 📦 Load danh sách tour
    // ===========================================================
    private void loadTours() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("tours")
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allTours.clear();
                    allTours.addAll(querySnapshot.getDocuments());

                    tours.clear();
                    tours.addAll(allTours);

                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ===========================================================
    // 🔍 Lọc danh sách theo tên
    // ===========================================================
    private void filterTours(String query) {
        tours.clear();
        if (TextUtils.isEmpty(query)) {
            tours.addAll(allTours);
        } else {
            String lowerQuery = query.toLowerCase();
            for (DocumentSnapshot doc : allTours) {
                String title = doc.getString("title");
                if (title != null && title.toLowerCase().contains(lowerQuery)) {
                    tours.add(doc);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ===========================================================
    // ❌ Kiểm tra trước khi xóa
    // ===========================================================
    private void confirmDelete(DocumentSnapshot doc) {
        String tourTitle = doc.getString("title");
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa tour")
                .setMessage("Bạn có chắc chắn muốn xóa tour \"" + tourTitle + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> checkTourStatusBeforeDelete(doc))
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ===========================================================
    // ⚠️ Kiểm tra status trước khi xóa
    // ===========================================================
    private void checkTourStatusBeforeDelete(DocumentSnapshot doc) {
        String status = doc.getString("status");
        if (status == null) status = "";

        // ❗ Chỉ cho phép xóa khi completed hoặc cancelled
        if (!status.equalsIgnoreCase("completed") && !status.equalsIgnoreCase("cancelled")) {
            Toast.makeText(getContext(), "Chỉ có thể xóa tour đã hoàn thành hoặc bị hủy!", Toast.LENGTH_LONG).show();
            return;
        }

        checkIfTourHasBookings(doc);
    }

    // ===========================================================
    // ⚠️ Kiểm tra tour có bookings không
    // ===========================================================
    private void checkIfTourHasBookings(DocumentSnapshot doc) {
        String tourId = doc.getId();

        db.collection("bookings")
                .whereEqualTo("tourId", tourId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(getContext(), "Không thể xóa! Tour này đã có lượt đặt.", Toast.LENGTH_LONG).show();
                    } else {
                        deleteTour(doc);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi khi kiểm tra bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ===========================================================
    // 🗑️ Xóa tour nếu hợp lệ
    // ===========================================================
    private void deleteTour(DocumentSnapshot doc) {
        db.collection("tours").document(doc.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    tours.remove(doc);
                    allTours.remove(doc);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Đã xóa tour thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTours();
    }
}
