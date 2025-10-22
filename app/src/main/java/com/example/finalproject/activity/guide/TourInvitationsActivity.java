package com.example.finalproject.activity.guide;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.guide.InvitationsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TourInvitationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private InvitationsAdapter adapter;
    private String guideId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_invitations);

        recyclerView = findViewById(R.id.recyclerInvitations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        // ✅ Lấy guideId hiện tại
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            guideId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            // 🔧 ID test nếu chưa đăng nhập
            guideId = "KbQBzkRgGbQNcc0XtRobyqwSlhN2";
        }

        // ✅ Nút quay lại
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // ✅ Tải lời mời
        loadInvitationsRealtime();
    }

    private void loadInvitationsRealtime() {
        db.collection("tour_invitations")
                .whereEqualTo("guideId", guideId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "❌ Lỗi tải lời mời: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snap != null && !snap.isEmpty()) {
                        List<DocumentSnapshot> list = new ArrayList<>(snap.getDocuments());
                        adapter = new InvitationsAdapter(list, this::respondToInvitation);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Toast.makeText(this, "📭 Không có lời mời nào.", Toast.LENGTH_SHORT).show();
                        recyclerView.setAdapter(null);
                    }
                });
    }

    private void respondToInvitation(String invitationId, String tourId, boolean accepted) {
        String newStatus = accepted ? "accepted" : "declined";

        db.collection("tour_invitations").document(invitationId)
                .update("status", newStatus)
                .addOnSuccessListener(v -> {
                    if (accepted) {
                        db.collection("tours").document(tourId)
                                .update("guideIds", FieldValue.arrayUnion(guideId))
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(this, "✅ Đã xác nhận tham gia tour", Toast.LENGTH_SHORT).show();
                                    adapter.removeInvitation(invitationId);
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "⚠️ Lỗi khi thêm hướng dẫn viên", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "🚫 Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                        adapter.removeInvitation(invitationId);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "❌ Lỗi cập nhật lời mời", Toast.LENGTH_SHORT).show());
    }
}
