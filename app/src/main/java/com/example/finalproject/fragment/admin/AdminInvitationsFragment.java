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

    private void loadInvitations() {
        db.collection("tour_invitations")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    invitationList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String guideId = doc.getString("guideId");
                        String tourId = doc.getString("tourId");
                        String status = doc.getString("status");

                        // Tạo map dữ liệu tạm
                        Map<String, Object> data = new HashMap<>();
                        data.put("status", status);
                        data.put("guideName", "Đang tải...");
                        data.put("tourName", "Đang tải...");
                        invitationList.add(data);

                        int currentIndex = invitationList.size() - 1;

                        // 🔹 Lấy tên hướng dẫn viên
                        if (guideId != null) {
                            db.collection("users").document(guideId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String role = userDoc.getString("role");
                                            if ("guide".equalsIgnoreCase(role)) {
                                                String lastName = userDoc.getString("lastname");
                                                String firstName = userDoc.getString("firstname");

                                                // Ghép tên theo định dạng: lastName + " " + firstName
                                                String guideName = ((lastName != null ? lastName : "") + " " +
                                                        (firstName != null ? firstName : "")).trim();

                                                invitationList.get(currentIndex).put("guideName", guideName);
                                                adapter.notifyItemChanged(currentIndex);
                                            } else {
                                                // Nếu không phải guide thì để trống hoặc ghi chú
                                                invitationList.get(currentIndex).put("guideName", "Không phải hướng dẫn viên");
                                                adapter.notifyItemChanged(currentIndex);
                                            }
                                        } else {
                                            invitationList.get(currentIndex).put("guideName", "Không tìm thấy hướng dẫn viên");
                                            adapter.notifyItemChanged(currentIndex);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        invitationList.get(currentIndex).put("guideName", "Lỗi tải tên hướng dẫn viên");
                                        adapter.notifyItemChanged(currentIndex);
                                    });
                        }

                        // 🔹 Lấy tên tour
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

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "❌ Lỗi tải lời mời: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
