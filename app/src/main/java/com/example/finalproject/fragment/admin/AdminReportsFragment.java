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
import com.example.finalproject.adapter.AdminReportAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminReportsFragment extends Fragment {

    private RecyclerView rvReports;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private AdminReportAdapter adapter;
    private final List<Map<String, Object>> reportList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        rvReports = view.findViewById(R.id.rvReports);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();

        rvReports.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminReportAdapter(getContext(), reportList);
        rvReports.setAdapter(adapter);

        loadReports();

        return view;
    }

    private void loadReports() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reportList.clear();
                    for (var doc : querySnapshot) {
                        Map<String, Object> report = doc.getData();
                        report.put("id", doc.getId());
                        reportList.add(report);
                    }
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
