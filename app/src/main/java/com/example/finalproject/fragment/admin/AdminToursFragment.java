package com.example.finalproject.fragment.admin;

import android.app.AlertDialog;
import android.content.Intent;
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
import com.example.finalproject.activity.AddTourActivity;
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

        db = FirebaseFirestore.getInstance();
        recyclerTours.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TourAdapter(getContext(), tours, new TourAdapter.OnTourActionListener() {
            @Override
            public void onEdit(DocumentSnapshot doc) {
                Intent intent = new Intent(getContext(), com.example.finalproject.activity.EditTourActivity.class);
                intent.putExtra("tourId", doc.getId());
                startActivity(intent);
            }

            @Override
            public void onView(DocumentSnapshot doc) {
                // TODO: Có thể mở chi tiết tour tại đây
                Toast.makeText(getContext(), "Tour: " + doc.getString("title"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                confirmDelete(doc);
            }
        });

        recyclerTours.setAdapter(adapter);

        tvAddTour.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddTourActivity.class);
            startActivity(intent);
        });

        loadTours();
        return view;
    }

    // ===========================================================
    // 📦 Load danh sách tour
    // ===========================================================
    private void loadTours() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("tours")
                .orderBy("start_date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    tours.clear();
                    tours.addAll(querySnapshot.getDocuments());
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
