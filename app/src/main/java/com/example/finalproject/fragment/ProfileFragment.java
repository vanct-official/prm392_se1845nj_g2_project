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

/**
 * Fragment hiển thị thông tin người dùng và cung cấp các tùy chọn như
 * xem danh sách yêu thích và đăng xuất.
 */
public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth; // Firebase Authentication instance
    private Button btnLogout; // Nút đăng xuất
    private Button btnGoToFavorites; // Nút chuyển sang danh sách yêu thích

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Nạp layout cho fragment này
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các nút từ layout
        btnLogout = view.findViewById(R.id.btnLogout);
        btnGoToFavorites = view.findViewById(R.id.btnGoToFavorites);

        // Gán sự kiện click cho nút Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();

            // Tạo Intent để quay về màn hình LoginActivity
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Đóng Activity chứa Fragment này
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        // Gán sự kiện click cho nút Danh sách tour yêu thích
        btnGoToFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomerFavoriteToursActivity.class);
            startActivity(intent);
        });
    }
}
