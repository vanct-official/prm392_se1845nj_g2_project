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
import com.example.finalproject.activity.AddTourAdminActivity;
import com.example.finalproject.activity.EditTourAdminActivity;
import com.example.finalproject.adapter.TourAdminAdapter;
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

        tvAddTour.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddTourAdminActivity.class);
            startActivity(intent);
        });

        setupSearchView();
        loadTours();
        return view;
    }

    // ===========================================================
    // 🔎 Thiết lập thanh tìm kiếm
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
    // 🔍 Lọc danh sách theo tiêu đề
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
    // ❌ Xóa tour
    // ===========================================================
    private void confirmDelete(DocumentSnapshot doc) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa tour")
                .setMessage("Bạn có chắc chắn muốn xóa tour \"" + doc.getString("title") + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
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
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTours();
    }
}
