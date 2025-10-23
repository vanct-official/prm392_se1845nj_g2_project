package com.example.finalproject.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.finalproject.LoginActivity;
import com.example.finalproject.R;
import com.example.finalproject.activity.CustomerFavoriteToursActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView imgAvatar;
    private TextView tvUserName, tvUserEmail, tvUserPhone, tvUserDob;
    private Button btnLogout, btnGoToFavorites;
    private Button btnPersonalInfo, btnChangePassword, btnHistory, btnTerms, btnHelp;

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

        // Ánh xạ view trong layout
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        tvUserDob = view.findViewById(R.id.tvUserDob);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnGoToFavorites = view.findViewById(R.id.btnGoToFavorites);
        btnPersonalInfo = view.findViewById(R.id.btnPersonalInfo);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnTerms = view.findViewById(R.id.btnTerms);
        btnHelp = view.findViewById(R.id.btnHelp);

        btnGoToFavorites.setVisibility(View.GONE);

        loadUserData(); // 🔹 Lấy dữ liệu từ Firestore và hiển thị

        // 🔹 Nút đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        // 🔹 Nút danh sách tour yêu thích (chỉ cho khách hàng)
        btnGoToFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomerFavoriteToursActivity.class);
            startActivity(intent);
        });

        // 🔹 Các nút còn lại (ví dụ sau này có thể mở activity khác)
        btnPersonalInfo.setOnClickListener(v ->
                Toast.makeText(getContext(), "Mở trang Thông tin cá nhân", Toast.LENGTH_SHORT).show()
        );

        btnChangePassword.setOnClickListener(v ->
                Toast.makeText(getContext(), "Mở trang Đổi mật khẩu", Toast.LENGTH_SHORT).show()
        );

        btnHistory.setOnClickListener(v ->
                Toast.makeText(getContext(), "Xem lịch sử gia hạn vé tháng", Toast.LENGTH_SHORT).show()
        );

        btnTerms.setOnClickListener(v ->
                Toast.makeText(getContext(), "Xem điều khoản dịch vụ", Toast.LENGTH_SHORT).show()
        );

        btnHelp.setOnClickListener(v ->
                Toast.makeText(getContext(), "Trợ giúp và hỗ trợ", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * 🔹 Lấy dữ liệu người dùng thật từ Firestore
     */
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Không có người dùng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        showUserData(document);
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy người dùng trong Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * 🔹 Hiển thị dữ liệu người dùng lên UI
     */
    private void showUserData(DocumentSnapshot doc) {
        String firstname = doc.getString("firstname");
        String lastname = doc.getString("lastname");
        String email = doc.getString("email");
        String phone = doc.getString("phone");
        Object dobObj = doc.get("dob");
        String dob = "";

        if (dobObj instanceof com.google.firebase.Timestamp) {
            java.util.Date date = ((com.google.firebase.Timestamp) dobObj).toDate();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            dob = sdf.format(date);
        } else if (dobObj instanceof java.util.Date) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            dob = sdf.format((java.util.Date) dobObj);
        } else if (dobObj instanceof String) {
            dob = (String) dobObj;
        } else {
            dob = "Chưa có ngày sinh";
        }

        String role = doc.getString("role");
        String avatarUrl = doc.getString("avatarUrl");

        // Họ tên
        String fullName = (firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "");
        tvUserName.setText(fullName.trim().isEmpty() ? "Chưa cập nhật" : fullName);

        // Email
        tvUserEmail.setText(email != null ? email : "Chưa có email");

        // Số điện thoại
        tvUserPhone.setText(phone != null ? phone : "Chưa có số điện thoại");

        // Ngày sinh
        tvUserDob.setText(dob != null ? dob : "Chưa có ngày sinh");

        // Nếu là customer → hiển thị nút tour yêu thích
        if ("customer".equalsIgnoreCase(role)) {
            btnGoToFavorites.setVisibility(View.VISIBLE);
        } else {
            btnGoToFavorites.setVisibility(View.GONE);
        }

        // Ảnh đại diện
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person);
        }
    }
}
