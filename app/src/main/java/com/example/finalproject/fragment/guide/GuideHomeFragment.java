package com.example.finalproject.fragment.guide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuideHomeFragment extends Fragment {

    private TextView tvWelcome, tvTotalTours, tvOngoingTours, tvUpcomingTours, tvReports;
    private Button btnSubmitReport;
    private FirebaseFirestore db;
    private String currentGuideId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide_home, container, false);

        // Ánh xạ view
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvTotalTours = view.findViewById(R.id.tvTotalTours);
        tvOngoingTours = view.findViewById(R.id.tvOngoingTours);
        tvUpcomingTours = view.findViewById(R.id.tvUpcomingTours);
        tvReports = view.findViewById(R.id.tvReports);
        btnSubmitReport = view.findViewById(R.id.btnSubmitReport);

        db = FirebaseFirestore.getInstance();
        currentGuideId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadGuideInfo();
        loadTourStats();

        // ======== Nút GỬI BÁO CÁO TOUR =========
        btnSubmitReport.setOnClickListener(v -> submitTourReport());
        Button btnReportHistory = view.findViewById(R.id.btnReportHistory);
        btnReportHistory.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new GuideReportHistoryFragment())
                    .addToBackStack(null)
                    .commit();
        });


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

                    tvTotalTours.setText(String.valueOf(total));
                    tvOngoingTours.setText(String.valueOf(ongoing));
                    tvUpcomingTours.setText(String.valueOf(upcoming));
                });

        db.collection("reports")
                .whereEqualTo("userId", currentGuideId)
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(snapshots -> tvReports.setText(String.valueOf(snapshots.size())));
    }

    /**
     * Gửi báo cáo tour sau khi hoàn thành
     */
    private void submitTourReport() {
        db.collection("tours")
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasTour = false;

                    for (DocumentSnapshot tourDoc : querySnapshot) {
                        List<String> guideIds = (List<String>) tourDoc.get("guideIds");
                        if (guideIds != null && guideIds.contains(currentGuideId)) {
                            hasTour = true;
                            String tourId = tourDoc.getId();
                            String tourName = tourDoc.getString("description");

                            // ✅ Kiểm tra nếu đã gửi báo cáo rồi
                            db.collection("reports")
                                    .whereEqualTo("userId", currentGuideId)
                                    .whereEqualTo("tourId", tourId)
                                    .get()
                                    .addOnSuccessListener(reportSnapshot -> {
                                        if (reportSnapshot.isEmpty()) {
                                            // Chưa gửi -> Cho phép gửi
                                            showReportDialog(tourId, tourName);
                                        } else {
                                            // Đã gửi -> Thông báo
                                            Toast.makeText(getContext(),
                                                    "Bạn đã gửi báo cáo cho tour này rồi.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    if (!hasTour) {
                        Toast.makeText(getContext(),
                                "Không có tour hoàn thành để gửi báo cáo.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Lỗi khi tải tour: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }


    /**
     * Hiển thị dialog nhập báo cáo tour
     */
    private void showReportDialog(String tourId, String tourName) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_submit_report, null);
        TextView edtSummary = dialogView.findViewById(R.id.edtSummary);
        TextView edtIssues = dialogView.findViewById(R.id.edtIssues);
        TextView edtParticipants = dialogView.findViewById(R.id.edtParticipants);
        TextView edtRating = dialogView.findViewById(R.id.edtRating);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Gửi báo cáo tour")
                .setView(dialogView)
                .setPositiveButton("Gửi", (d, which) -> {
                    String summary = edtSummary.getText().toString().trim();
                    String issues = edtIssues.getText().toString().trim();
                    int participants = 0;
                    double rating = 0;

                    try {
                        participants = Integer.parseInt(edtParticipants.getText().toString().trim());
                    } catch (Exception ignored) {}

                    try {
                        rating = Double.parseDouble(edtRating.getText().toString().trim());
                        if (rating < 1 || rating > 5) {
                            Toast.makeText(getContext(),
                                    "Số sao phải nằm trong khoảng 1 đến 5.",
                                    Toast.LENGTH_SHORT).show();
                            return; // Dừng gửi
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(),
                                "Vui lòng nhập số sao hợp lệ (1 - 5).",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Gửi báo cáo lên Firestore
                    Map<String, Object> report = new HashMap<>();
                    report.put("userId", currentGuideId);
                    report.put("tourId", tourId);
                    report.put("tourName", tourName != null ? tourName : "Không tên");
                    report.put("summary", summary.isEmpty() ? "Không có nội dung" : summary);
                    report.put("issues", issues.isEmpty() ? "Không có sự cố" : issues);
                    report.put("participantsCount", participants);
                    report.put("ratingFromGuide", rating);
                    report.put("adminComment", "");
                    report.put("status", "pending");
                    report.put("createdAt", new Timestamp(new Date()));
                    report.put("updatedAt", new Timestamp(new Date()));

                    db.collection("reports")
                            .add(report)
                            .addOnSuccessListener(ref ->
                                    Toast.makeText(getContext(),
                                            "Đã gửi báo cáo tour: " + tourName,
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Lỗi khi gửi báo cáo: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", (d, which) -> d.dismiss())
                .create();

        dialog.show();
    }


}
