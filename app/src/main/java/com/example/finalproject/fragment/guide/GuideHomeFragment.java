package com.example.finalproject.fragment.guide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.finalproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Màn hình chính của hướng dẫn viên
 * Hiển thị nút truy cập danh sách lời mời tham gia tour
 */
public class GuideHomeFragment extends Fragment {

    private MaterialButton btnViewRequests;
    private FirebaseFirestore db;
    private String guideId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guide_home, container, false);

        btnViewRequests = view.findViewById(R.id.btnViewRequests);
        db = FirebaseFirestore.getInstance();
        guideId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        btnViewRequests.setOnClickListener(v -> openGuideRequestsFragment());

        // (Tuỳ chọn) bạn có thể gọi hàm này để hiển thị số lượng lời mời chờ xử lý trên nút
        // loadPendingRequestsCount();

        return view;
    }

    /**
     * Chuyển sang fragment hiển thị danh sách lời mời (GuideRequestsFragment)
     */
    private void openGuideRequestsFragment() {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container_guide, new GuideRequestsFragment());
        transaction.addToBackStack(null); // Cho phép quay lại
        transaction.commit();
    }

    /**
     * (Tuỳ chọn) Hiển thị số lượng lời mời đang chờ trên nút
     */
    private void loadPendingRequestsCount() {
        if (guideId == null) return;

        db.collection("guide_requests")
                .whereEqualTo("guideId", guideId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    if (count > 0) {
                        btnViewRequests.setText("📩 Xem lời mời tham gia tour (" + count + ")");
                    } else {
                        btnViewRequests.setText("📩 Xem lời mời tham gia tour");
                    }
                });
    }
}
