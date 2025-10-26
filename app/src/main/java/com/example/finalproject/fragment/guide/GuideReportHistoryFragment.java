package com.example.finalproject.fragment.guide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuideReportHistoryFragment extends Fragment {

    private RecyclerView rvReports;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private String currentGuideId;
    private ReportAdapter adapter;
    private List<DocumentSnapshot> reportList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_history, container, false);

        rvReports = view.findViewById(R.id.rvReports);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        rvReports.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        currentGuideId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new ReportAdapter(reportList);
        rvReports.setAdapter(adapter);

        loadReportHistory();
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        return view;
    }

    private void loadReportHistory() {
        db.collection("reports")
                .whereEqualTo("userId", currentGuideId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    reportList.clear();
                    reportList.addAll(snapshots.getDocuments());

                    if (reportList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvReports.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvReports.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportVH> {

        private final List<DocumentSnapshot> reportList;

        ReportAdapter(List<DocumentSnapshot> reportList) {
            this.reportList = reportList;
        }

        @NonNull
        @Override
        public ReportVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report_history, parent, false);

            return new ReportVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportVH holder, int position) {
            DocumentSnapshot doc = reportList.get(position);
            String tourName = doc.getString("tourName");
            String summary = doc.getString("summary");
            String status = doc.getString("status");
            String adminComment = doc.getString("adminComment");
            Long participants = doc.getLong("participantsCount");
            Double rating = doc.getDouble("ratingFromGuide");
            Object createdAt = doc.get("createdAt");

            holder.tvTourName.setText(tourName != null ? tourName : "(Không tên)");
            holder.tvSummary.setText(summary);
            holder.tvAdminComment.setText("Phản hồi: " + (adminComment == null ? "Chưa có" : adminComment));

            if (participants != null)
                holder.tvParticipants.setText("👥 " + participants + " khách");
            if (rating != null)
                holder.tvRating.setText("⭐ " + rating);

            if (createdAt != null && createdAt instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) createdAt;
                String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(ts.toDate());
                holder.tvDate.setText("🕓 " + date);
            }


            if ("pending".equals(status)) {
                holder.tvStatus.setText("Đang chờ duyệt");
                holder.tvStatus.setTextColor(0xFFFFA000); // vàng
            } else if ("completed".equals(status)) {
                holder.tvStatus.setText("Đã duyệt");
                holder.tvStatus.setTextColor(0xFF388E3C); // xanh
            } else {
                holder.tvStatus.setText("Bị từ chối");
                holder.tvStatus.setTextColor(0xFFD32F2F); // đỏ
            }
        }

        @Override
        public int getItemCount() {
            return reportList.size();
        }

        static class ReportVH extends RecyclerView.ViewHolder {
            TextView tvTourName, tvSummary, tvStatus, tvAdminComment, tvParticipants, tvRating, tvDate;

            ReportVH(@NonNull View itemView) {
                super(itemView);
                tvTourName = itemView.findViewById(R.id.tvTourName);
                tvSummary = itemView.findViewById(R.id.tvSummary);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvAdminComment = itemView.findViewById(R.id.tvAdminComment);
                tvParticipants = itemView.findViewById(R.id.tvParticipants);
                tvRating = itemView.findViewById(R.id.tvRating);
                tvDate = itemView.findViewById(R.id.tvDate);
            }
        }
    }
}
