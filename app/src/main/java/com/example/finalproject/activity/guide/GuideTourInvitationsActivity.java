package com.example.finalproject.activity.guide;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.guide.GuideInvitationsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GuideTourInvitationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private GuideInvitationsAdapter adapter;
    private String guideId;
    private boolean isEmptyNotified = false;
    private boolean isFirstLoad = true; // ✅ Chỉ hiển thị toast "Không có lời mời" lần đầu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_invitations_guide);

        recyclerView = findViewById(R.id.recyclerInvitations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            guideId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            guideId = "KbQBzkRgGbQNcc0XtRobyqwSlhN2";
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

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
                        isEmptyNotified = false;
                        isFirstLoad = false; // ✅ Đã có dữ liệu rồi

                        List<DocumentSnapshot> list = new ArrayList<>(snap.getDocuments());
                        adapter = new GuideInvitationsAdapter(list, this::respondToInvitation);
                        recyclerView.setAdapter(adapter);
                    } else {
                        // ✅ Chỉ hiển thị toast nếu:
                        // - Chưa từng thông báo (isEmptyNotified = false)
                        // - VÀ đây là lần load đầu tiên (isFirstLoad = true)
                        if (!isEmptyNotified && isFirstLoad) {
                            Toast.makeText(this, "📭 Không có lời mời nào.", Toast.LENGTH_SHORT).show();
                            isEmptyNotified = true;
                        }
                        isFirstLoad = false;
                        recyclerView.setAdapter(null);
                    }
                });
    }

    private void respondToInvitation(String invitationId, String tourId, boolean accepted) {
        String newStatus = accepted ? "accepted" : "declined";

        // ✅ Đánh dấu đã xử lý → không hiện toast "Không có lời mời" nữa
        isEmptyNotified = true;

        db.collection("tour_invitations").document(invitationId)
                .update("status", newStatus)
                .addOnSuccessListener(v -> {
                    if (accepted) {
                        db.collection("tours").document(tourId)
                                .update("guideIds", FieldValue.arrayUnion(guideId))
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(this, "✅ Đã xác nhận tham gia tour", Toast.LENGTH_SHORT).show();
                                    if (adapter != null) {
                                        adapter.removeInvitation(invitationId);
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "⚠️ Lỗi khi thêm hướng dẫn viên", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "🚫 Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                        if (adapter != null) {
                            adapter.removeInvitation(invitationId);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "❌ Lỗi cập nhật lời mời", Toast.LENGTH_SHORT).show());
    }
}