package com.example.finalproject.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportDetailDialogAdmin extends Dialog {

    private final Map<String, Object> report;
    private final FirebaseFirestore db;

    public ReportDetailDialogAdmin(@NonNull Context context, Map<String, Object> report) {
        super(context);
        this.report = report;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_report_detail);

        // 🔽 Gắn view
        View statusContainer = findViewById(R.id.statusContainer);
        TextView tvTourName = findViewById(R.id.tvTourName);
        TextView tvGuideId = findViewById(R.id.tvGuideId);
        TextView tvSummary = findViewById(R.id.tvSummary);
        TextView tvIssues = findViewById(R.id.tvIssues);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvCreatedAt = findViewById(R.id.tvCreatedAt);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        EditText etAdminComment = findViewById(R.id.etAdminComment);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnClose = findViewById(R.id.btnClose);

        // ====== Thông tin cơ bản ======
        tvTourName.setText("Tour: " + report.getOrDefault("tourName", "(Không rõ)"));
        tvSummary.setText("Tóm tắt: " + report.getOrDefault("summary", "(Không có)"));
        tvIssues.setText("Sự cố: " + report.getOrDefault("issues", "(Không có)"));
        String statusEn = (String) report.getOrDefault("status", "unknown");
        String statusVi = convertStatusToVietnamese(statusEn);
        tvStatus.setText("Trạng thái: " + statusVi);

        Object ratingObj = report.get("ratingFromGuide");
        if (ratingObj instanceof Number) {
            ratingBar.setRating(((Number) ratingObj).floatValue());
        }

        Object createdAt = report.get("createdAt");
        if (createdAt instanceof Timestamp) {
            Date date = ((Timestamp) createdAt).toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvCreatedAt.setText("Ngày gửi: " + sdf.format(date));
        }

        // ====== Lấy hướng dẫn viên từ tourId ======
        String tourId = (String) report.get("tourId");
        if (tourId == null || tourId.isEmpty()) {
            tvGuideId.setText("Hướng dẫn viên: (Không rõ tour)");
        } else {
            loadGuidesForTour(tourId, tvGuideId);
        }

        // ====== Kiểm tra trạng thái ======
        String status = (String) report.get("status");
        if ("completed".equalsIgnoreCase(status)) {
            etAdminComment.setEnabled(false);
            etAdminComment.setAlpha(0.7f);
            btnSave.setVisibility(View.GONE);
            statusContainer.setBackgroundColor(Color.parseColor("#E6F9EC"));
            tvStatus.setText("✔️ Báo cáo đã hoàn tất");
            tvStatus.setTextColor(Color.parseColor("#16A34A"));
        }

        // ====== Hành động ======
        btnClose.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String comment = etAdminComment.getText().toString().trim();
            String id = (String) report.get("id");

            if (id == null) {
                Toast.makeText(getContext(), "Không tìm thấy ID báo cáo!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (comment.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập nhận xét!", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("reports").document(id)
                    .update(
                            "adminComment", comment,
                            "status", "completed",
                            "updatedAt", new Timestamp(new Date())
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "✅ Báo cáo đã hoàn tất!", Toast.LENGTH_SHORT).show();
                        btnSave.setVisibility(View.GONE);
                        etAdminComment.setEnabled(false);
                        etAdminComment.setAlpha(0.7f);
                        tvStatus.setText("✔️ Báo cáo đã hoàn tất");
                        tvStatus.setTextColor(Color.parseColor("#16A34A"));
                        statusContainer.setBackgroundColor(Color.parseColor("#E6F9EC"));
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }

    /**
     * 🔹 Lấy danh sách hướng dẫn viên thuộc tour
     */
    private void loadGuidesForTour(String tourId, TextView tvGuideId) {
        db.collection("tours").document(tourId)
                .get()
                .addOnSuccessListener(tourDoc -> {
                    if (!tourDoc.exists()) {
                        tvGuideId.setText("Hướng dẫn viên: (Không tìm thấy tour)");
                        return;
                    }

                    List<String> guideIds = (List<String>) tourDoc.get("guideIds");
                    if (guideIds == null || guideIds.isEmpty()) {
                        tvGuideId.setText("Hướng dẫn viên: (Chưa gán)");
                        return;
                    }

                    db.collection("users")
                            .whereIn(FieldPath.documentId(), guideIds)
                            .get()
                            .addOnSuccessListener(userQuery -> {
                                if (userQuery.isEmpty()) {
                                    tvGuideId.setText("Hướng dẫn viên: (Không tìm thấy)");
                                    return;
                                }

                                List<String> guideNames = new ArrayList<>();
                                for (DocumentSnapshot userDoc : userQuery) {
                                    if ("guide".equals(userDoc.getString("role"))) {
                                        String firstName = userDoc.getString("firstname");
                                        String lastName = userDoc.getString("lastname");
                                        String fullName = ((firstName != null ? firstName : "") + " " +
                                                (lastName != null ? lastName : "")).trim();
                                        if (!fullName.isEmpty()) guideNames.add(fullName);
                                    }
                                }

                                if (!guideNames.isEmpty()) {
                                    tvGuideId.setText("Hướng dẫn viên: " + String.join(", ", guideNames));
                                } else {
                                    tvGuideId.setText("Hướng dẫn viên: (Không rõ)");
                                }
                            })
                            .addOnFailureListener(e ->
                                    tvGuideId.setText("Hướng dẫn viên: (Lỗi tải danh sách)")
                            );
                })
                .addOnFailureListener(e ->
                        tvGuideId.setText("Hướng dẫn viên: (Lỗi tải tour)")
                );
    }
    private String convertStatusToVietnamese(String statusEn) {
        if (statusEn == null) return "(Không rõ)";
        switch (statusEn.toLowerCase()) {
            case "pending":
                return "Chờ xử lý";
            case "reviewed":
                return "Đã xem xét";
            case "completed":
                return "Hoàn tất";
            case "cancelled":
                return "Đã hủy";
            default:
                return "(Không rõ)";
        }
    }

}
