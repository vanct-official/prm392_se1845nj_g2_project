package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.AdminReviewAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminReviewsFragment extends Fragment {

    private RecyclerView rvReviews;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private AdminReviewAdapter adapter;
    private List<Map<String, Object>> reviewList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reviews, container, false);

        rvReviews = view.findViewById(R.id.rvReviews);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        rvReviews.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminReviewAdapter(getContext(), reviewList);
        rvReviews.setAdapter(adapter);

        loadReviews();

        return view;
    }

    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    reviewList.clear();
                    for (var doc : query) {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        reviewList.add(data);
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
