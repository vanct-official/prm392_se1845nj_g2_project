package com.example.finalproject.fragment.guide;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class GuideSubmitReportFragment extends Fragment {

    private RecyclerView rvCompletedTours;
    private FirebaseFirestore db;
    private String currentGuideId;
    private List<DocumentSnapshot> tourList = new ArrayList<>();
    private CompletedTourAdapter adapter;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submit_tour_report, container, false);

        rvCompletedTours = view.findViewById(R.id.rvCompletedTours);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvCompletedTours.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        currentGuideId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new CompletedTourAdapter(tourList);
        rvCompletedTours.setAdapter(adapter);

        loadCompletedTours();
        return view;
    }

    private void loadCompletedTours() {
        db.collection("tours")
                .whereArrayContains("guideIds", currentGuideId)
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(snapshots -> {
                    tourList.clear();
                    tourList.addAll(snapshots.getDocuments());

                    if (tourList.isEmpty()) {
                        rvCompletedTours.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvCompletedTours.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private class CompletedTourAdapter extends RecyclerView.Adapter<CompletedTourAdapter.TourVH> {
        private List<DocumentSnapshot> tours;

        CompletedTourAdapter(List<DocumentSnapshot> tours) {
            this.tours = tours;
        }

        @NonNull
        @Override
        public TourVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_completed_tour, parent, false);
            return new TourVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TourVH holder, int position) {
            DocumentSnapshot doc = tours.get(position);
            String title = doc.getString("title");
            String destination = doc.getString("destination");
            List<String> images = (List<String>) doc.get("images");

            holder.tvTitle.setText(title);
            holder.tvDestination.setText(destination);
            if (images != null && !images.isEmpty())
                Glide.with(getContext()).load(images.get(0)).into(holder.imgTour);

            holder.btnReport.setOnClickListener(v -> showReportDialog(doc));
        }

        @Override
        public int getItemCount() {
            return tours.size();
        }

        class TourVH extends RecyclerView.ViewHolder {
            ImageView imgTour;
            TextView tvTitle, tvDestination;
            Button btnReport;

            TourVH(@NonNull View itemView) {
                super(itemView);
                imgTour = itemView.findViewById(R.id.imgTour);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDestination = itemView.findViewById(R.id.tvDestination);
                btnReport = itemView.findViewById(R.id.btnReport);
            }
        }
    }

    private void showReportDialog(DocumentSnapshot tourDoc) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_submit_report, null, false);

        EditText edtSummary = dialogView.findViewById(R.id.edtSummary);
        EditText edtIssues = dialogView.findViewById(R.id.edtIssues);
        EditText edtParticipants = dialogView.findViewById(R.id.edtParticipants);
        EditText edtRating = dialogView.findViewById(R.id.edtRating);

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String summary = edtSummary.getText().toString().trim();
                    String issues = edtIssues.getText().toString().trim();
                    int participants = edtParticipants.getText().toString().isEmpty()
                            ? 0 : Integer.parseInt(edtParticipants.getText().toString());
                    double rating = edtRating.getText().toString().isEmpty()
                            ? 0 : Double.parseDouble(edtRating.getText().toString());

                    Map<String, Object> report = new HashMap<>();
                    report.put("tourId", tourDoc.getId());
                    report.put("tourName", tourDoc.getString("title"));
                    report.put("userId", currentGuideId);
                    report.put("summary", summary);
                    report.put("issues", issues);
                    report.put("participantsCount", participants);
                    report.put("ratingFromGuide", rating);
                    report.put("createdAt", new Timestamp(new Date()));
                    report.put("updatedAt", new Timestamp(new Date()));
                    report.put("status", "pending");
                    report.put("adminComment", "");

                    db.collection("reports")
                            .add(report)
                            .addOnSuccessListener(ref ->
                                    Toast.makeText(getContext(),
                                            "Đã gửi báo cáo tour " + tourDoc.getString("title"),
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Lỗi khi gửi báo cáo: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
