package com.example.finalproject.fragment.guide;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.guide.TourCardAdapter;
import com.example.finalproject.entity.Tour;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GuideHomeFragment extends Fragment {

    private TextView tvWelcome, tvTotal, tvOngoing, tvUpcoming, tvReports, tvEmpty;
    private RecyclerView rvUpcoming;
    private FirebaseFirestore db;
    private String currentGuideId;
    private TourCardAdapter adapter;
    private List<Tour> upcomingTours = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide_home, container, false);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvOngoing = view.findViewById(R.id.tvOngoing);
        tvUpcoming = view.findViewById(R.id.tvUpcoming);
        tvReports = view.findViewById(R.id.tvReports);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvUpcoming = view.findViewById(R.id.rvUpcoming);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TourCardAdapter(getContext(), upcomingTours);
        rvUpcoming.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentGuideId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadGuideInfo();
        loadTourStats();
        loadUpcomingTours();

        return view;
    }

    private void loadGuideInfo() {
        db.collection("users").document(currentGuideId)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("firstname");
                    tvWelcome.setText("Xin chào, " + (name != null ? name : "bạn") + " 👋");
                });
    }

    private void loadTourStats() {
        db.collection("tours")
                .whereArrayContains("guideIds", currentGuideId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int total = snapshots.size();
                    int ongoing = 0, upcoming = 0;

                    for (DocumentSnapshot doc : snapshots) {
                        String status = doc.getString("status");
                        if ("ongoing".equals(status)) ongoing++;
                        if ("upcoming".equals(status)) upcoming++;
                    }

                    tvTotal.setText(String.valueOf(total));
                    tvOngoing.setText(String.valueOf(ongoing));
                    tvUpcoming.setText(String.valueOf(upcoming));
                });

        db.collection("reports")
                .whereEqualTo("guideId", currentGuideId)
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(snapshots ->
                        tvReports.setText(String.valueOf(snapshots.size())));
    }

    private void loadUpcomingTours() {
        db.collection("tours")
                .whereArrayContains("guideIds", currentGuideId)
                .whereEqualTo("status", "upcoming")
                .get()
                .addOnSuccessListener(snapshots -> {
                    upcomingTours.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Tour tour = doc.toObject(Tour.class);
                        upcomingTours.add(tour);
                    }

                    if (upcomingTours.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvUpcoming.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvUpcoming.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e("GuideHome", "Error loading tours", e));
    }
}
