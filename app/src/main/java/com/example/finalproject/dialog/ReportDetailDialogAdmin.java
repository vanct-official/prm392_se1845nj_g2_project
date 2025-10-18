package com.example.finalproject.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.finalproject.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import android.view.View;

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

        // 🔽 Gắn view SAU setContentView()
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

        tvTourName.setText("Tour: " + report.getOrDefault("tourName", "(Không rõ)"));
        tvGuideId.setText("Hướng dẫn viên ID: " + report.getOrDefault("guideId", "(Không rõ)"));
        tvSummary.setText("Tóm tắt: " + report.getOrDefault("summary", "(Không có)"));
        tvIssues.setText("Sự cố: " + report.getOrDefault("issues", "(Không có)"));
        tvStatus.setText("Trạng thái: " + report.getOrDefault("status", "(Không rõ)"));

        Object ratingObj = report.get("ratingFromGuide");
        if (ratingObj instanceof Number) {
            ratingBar.setRating(((Number) ratingObj).floatValue());
        }

        Object createdAt = report.get("createdAt");
        if (createdAt instanceof com.google.firebase.Timestamp) {
            Date date = ((com.google.firebase.Timestamp) createdAt).toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvCreatedAt.setText("Ngày gửi: " + sdf.format(date));
        }

        // ⚙️ Kiểm tra trạng thái
        String status = (String) report.get("status");
        if ("completed".equalsIgnoreCase(status)) {
            etAdminComment.setEnabled(false);
            etAdminComment.setAlpha(0.7f);
            btnSave.setVisibility(View.GONE);

            // 🌿 Đổi màu nền + hiển thị thông báo hoàn tất
            statusContainer.setBackgroundColor(Color.parseColor("#E6F9EC"));
            tvStatus.setText("✔️ Báo cáo đã hoàn tất");
            tvStatus.setTextColor(Color.parseColor("#16A34A"));
        }

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

            FirebaseFirestore.getInstance().collection("reports").document(id)
                    .update(
                            "adminComment", comment,
                            "status", "completed",
                            "updatedAt", new com.google.firebase.Timestamp(new Date())
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "✅ Báo cáo đã hoàn tất!", Toast.LENGTH_SHORT).show();

                        // 🔄 Cập nhật giao diện ngay lập tức
                        btnSave.setVisibility(View.GONE);
                        etAdminComment.setEnabled(false);
                        etAdminComment.setAlpha(0.7f);
                        tvStatus.setText("✔️ Báo cáo đã hoàn tất");
                        tvStatus.setTextColor(Color.parseColor("#16A34A"));
                        statusContainer.setBackgroundColor(Color.parseColor("#E6F9EC"));
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "❌ Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }

}
