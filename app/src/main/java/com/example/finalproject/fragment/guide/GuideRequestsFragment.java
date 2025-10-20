package com.example.finalproject.fragment.guide;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment hiển thị danh sách lời mời cho hướng dẫn viên
 */
public class GuideRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String guideId;

    private List<DocumentSnapshot> requestList = new ArrayList<>();
    private com.example.finalproject.fragment.guide.GuideRequestAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide_requests, container, false);

        recyclerView = view.findViewById(R.id.recyclerRequests);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        guideId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new com.example.finalproject.fragment.guide.GuideRequestAdapter(requestList, this::showActionDialog);
        recyclerView.setAdapter(adapter);

        loadRequests();
        return view;
    }

    private void loadRequests() {
        if (guideId == null) return;
        progressBar.setVisibility(View.VISIBLE);

        db.collection("guide_requests")
                .whereEqualTo("guideId", guideId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(query -> {
                    progressBar.setVisibility(View.GONE);
                    requestList.clear();
                    requestList.addAll(query.getDocuments());
                    adapter.notifyDataSetChanged();

                    if (requestList.isEmpty())
                        Toast.makeText(getContext(), "Không có lời mời nào.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showActionDialog(DocumentSnapshot requestDoc) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xử lý lời mời")
                .setMessage("Bạn có muốn chấp nhận tour này không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> acceptRequest(requestDoc))
                .setNegativeButton("Từ chối", (dialog, which) -> rejectRequest(requestDoc))
                .show();
    }

    private void acceptRequest(DocumentSnapshot requestDoc) {
        String requestId = requestDoc.getId();
        String tourId = requestDoc.getString("tourId");

        db.collection("guide_requests").document(requestId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    db.collection("tours").document(tourId)
                            .update("guideIds", FieldValue.arrayUnion(guideId))
                            .addOnSuccessListener(u -> {
                                Toast.makeText(getContext(), "✅ Đã chấp nhận tour!", Toast.LENGTH_SHORT).show();
                                loadRequests();
                            });
                });
    }

    private void rejectRequest(DocumentSnapshot requestDoc) {
        String requestId = requestDoc.getId();
        db.collection("guide_requests").document(requestId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "❌ Đã từ chối tour.", Toast.LENGTH_SHORT).show();
                    loadRequests();
                });
    }
}
