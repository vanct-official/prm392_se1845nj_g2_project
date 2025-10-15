package com.example.finalproject.fragment.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.TourAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminToursFragment extends Fragment {

    private RecyclerView recyclerTours;
    private TextView tvAddTour;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private TourAdapter adapter;
    private List<DocumentSnapshot> tours = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_tours, container, false);

        recyclerTours = view.findViewById(R.id.recyclerTours);
        tvAddTour = view.findViewById(R.id.tvAddTour);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerTours.setLayoutManager(new LinearLayoutManager(getContext()));
        db = FirebaseFirestore.getInstance();

        adapter = new TourAdapter(getContext(), tours, new TourAdapter.OnTourActionListener() {
            @Override
            public void onEdit(DocumentSnapshot doc) {
                Toast.makeText(getContext(), "Sửa: " + doc.getId(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onView(DocumentSnapshot doc) {
                Toast.makeText(getContext(), "Xem: " + doc.getId(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                confirmDelete(doc);
            }
        });

        recyclerTours.setAdapter(adapter);

        tvAddTour.setOnClickListener(v -> Toast.makeText(getContext(), "Thêm tour mới!", Toast.LENGTH_SHORT).show());

        loadTours();
        return view;
    }

    private void loadTours() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("tours")
                .orderBy("createAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    tours.clear();
                    tours.addAll(snapshot.getDocuments());
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDelete(DocumentSnapshot doc) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa tour")
                .setMessage("Bạn có chắc muốn xóa tour này không?")
                .setPositiveButton("Xóa", (dialog, which) -> db.collection("tours").document(doc.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            tours.remove(doc);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getContext(), "Đã xóa!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTours();
    }
}
