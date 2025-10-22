package com.example.finalproject.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.example.finalproject.LoginActivity;
import com.example.finalproject.activity.CustomerFavoriteToursActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fragment hiển thị thông tin người dùng và cung cấp các tùy chọn như
 * xem danh sách yêu thích và đăng xuất.
 */
public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button btnLogout;
    private Button btnGoToFavorites;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogout = view.findViewById(R.id.btnLogout);
        btnGoToFavorites = view.findViewById(R.id.btnGoToFavorites);

        // Ẩn nút yêu thích mặc định
        btnGoToFavorites.setVisibility(View.GONE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // 🔹 Lấy role của user từ Firestore
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String role = document.getString("role");
                            if ("customer".equalsIgnoreCase(role)) {
                                btnGoToFavorites.setVisibility(View.VISIBLE);
                            } else {
                                btnGoToFavorites.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Nếu lỗi Firestore thì vẫn ẩn nút
                        btnGoToFavorites.setVisibility(View.GONE);
                    });
        }

        // Nút đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        // Nút danh sách tour yêu thích
        btnGoToFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomerFavoriteToursActivity.class);
            startActivity(intent);
        });
    }
}
