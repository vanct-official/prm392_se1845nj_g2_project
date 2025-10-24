package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.admin.AdminInvitationsAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminInvitationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private AdminInvitationsAdapter adapter;
    private List<Map<String, Object>> invitationList = new ArrayList<>();

    public AdminInvitationsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_invitations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerInvitationsAdmin);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new AdminInvitationsAdapter(invitationList);
        recyclerView.setAdapter(adapter);

        loadInvitations();
    }

    // Trong AdminInvitationsFragment
    private void loadInvitations() {
        db.collection("tour_invitations")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    invitationList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String guideId = doc.getString("guideId");
                        String tourId = doc.getString("tourId");
                        String status = doc.getString("status");

                        // Map status sang tiếng Việt
                        String statusVN;
                        if ("pending".equalsIgnoreCase(status)) {
                            statusVN = "Chưa đồng ý";
                        } else if ("declined".equalsIgnoreCase(status)) {
                            statusVN = "Đã từ chối";
                        } else {
                            statusVN = "Không xác định";
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put("status", statusVN);  // dùng tiếng Việt để hiển thị
                        data.put("guideName", "Đang tải...");
                        data.put("tourName", "Đang tải...");
                        data.put("avatarUrl", "");
                        invitationList.add(data);

                        int currentIndex = invitationList.size() - 1;

                        // Lấy tên & avatar hướng dẫn viên
                        if (guideId != null) {
                            db.collection("users").document(guideId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String role = userDoc.getString("role");
                                            if ("guide".equalsIgnoreCase(role)) {
                                                String lastName = userDoc.getString("lastname");
                                                String firstName = userDoc.getString("firstname");
                                                String guideName = ((lastName != null ? lastName : "") + " " +
                                                        (firstName != null ? firstName : "")).trim();
                                                String avatarUrl = userDoc.getString("avatarUrl");

                                                Map<String, Object> currentData = invitationList.get(currentIndex);
                                                currentData.put("guideName", guideName);
                                                currentData.put("avatarUrl", avatarUrl != null ? avatarUrl : "");
                                                adapter.notifyItemChanged(currentIndex);
                                            } else {
                                                invitationList.get(currentIndex).put("guideName", "Không phải hướng dẫn viên");
                                                adapter.notifyItemChanged(currentIndex);
                                            }
                                        }
                                    });
                        }

                        // Lấy tên tour
                        if (tourId != null) {
                            db.collection("tours").document(tourId)
                                    .get()
                                    .addOnSuccessListener(tourDoc -> {
                                        if (tourDoc.exists()) {
                                            String tourName = tourDoc.getString("title");
                                            invitationList.get(currentIndex).put("tourName", tourName);
                                            adapter.notifyItemChanged(currentIndex);
                                        }
                                    });
                        }
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "❌ Lỗi tải lời mời: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

}
