package com.example.finalproject.fragment.guide;

import android.os.Bundle;
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
import com.example.finalproject.adapter.guide.GuideTourAdapter;
import com.example.finalproject.entity.Tour;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GuideHomeFragment extends Fragment {

    private TextView tvWelcome, tvTotalTours, tvOngoing, tvUpcoming, tvReports;
    private RecyclerView rvUpcomingTours;
    private GuideTourAdapter adapter;
    private FirebaseFirestore db;
    private String guideId;
    private List<Tour> upcomingTours = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide_home, container, false);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvTotalTours = view.findViewById(R.id.tvTotalTours);
        tvOngoing = view.findViewById(R.id.tvOngoing);
        tvUpcoming = view.findViewById(R.id.tvUpcoming);
        tvReports = view.findViewById(R.id.tvReports);
        rvUpcomingTours = view.findViewById(R.id.rvUpcomingTours);

        rvUpcomingTours.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        guideId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadGuideInfo();
        loadDashboardData();
        loadUpcomingTours();

        return view;
    }

    private void loadGuideInfo() {
        db.collection("users").document(guideId).get().addOnSuccessListener(doc -> {
            String name = doc.getString("fullName");
            tvWelcome.setText("Xin chào, " + name + " 👋");
        });
    }

    private void loadDashboardData() {
        db.collection("tours").whereEqualTo("guideId", guideId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int total = snapshot.size();
                    int ongoing = 0;
                    int upcoming = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = doc.getString("status");
                        if ("Ongoing".equalsIgnoreCase(status)) ongoing++;
                        if ("Upcoming".equalsIgnoreCase(status)) upcoming++;
                    }

                    tvTotalTours.setText(String.valueOf(total));
                    tvOngoing.setText(String.valueOf(ongoing));
                    tvUpcoming.setText(String.valueOf(upcoming));
                });

        db.collection("reports").whereEqualTo("guideId", guideId)
                .get().addOnSuccessListener(snapshot -> {
                    tvReports.setText(String.valueOf(snapshot.size()));
                });
    }

    private void loadUpcomingTours() {
        db.collection("tours")
                .whereEqualTo("guideId", guideId)
                .whereEqualTo("status", "Upcoming")
                .limit(3)
                .get()
                .addOnSuccessListener(snapshot -> {
                    upcomingTours.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Tour tour = doc.toObject(Tour.class);
                        upcomingTours.add(tour);
                    }
                    adapter = new GuideTourAdapter(getContext(), upcomingTours);
                    rvUpcomingTours.setAdapter(adapter);
                });
    }
}
