package com.example.finalproject.fragment.guide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvDob;
    private ShapeableImageView imgAvatar;
    private View btnPersonalInfo, btnChangePassword, btnMyTours, btnGoToFavorites, btnSupport;
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Ánh xạ view
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvDob = view.findViewById(R.id.tvDob);
        imgAvatar = view.findViewById(R.id.imgAvatar);

        // các mục là LinearLayout nên dùng View
        btnPersonalInfo = view.findViewById(R.id.btnPersonalInfo);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnMyTours = view.findViewById(R.id.btnMyTours);
        btnGoToFavorites = view.findViewById(R.id.btnGoToFavorites);
        btnSupport = view.findViewById(R.id.btnSupport);

        btnLogout = view.findViewById(R.id.btnLogout);

        // Dữ liệu mẫu
        tvName.setText("Chu Thế Văn");
        tvEmail.setText("chuthevan450@gmail.com");
        tvDob.setText("25/06/2003");

        // Xử lý click
        btnPersonalInfo.setOnClickListener(v ->
                Toast.makeText(getContext(), "Xem thông tin cá nhân", Toast.LENGTH_SHORT).show());

        btnChangePassword.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đổi mật khẩu", Toast.LENGTH_SHORT).show());

        btnMyTours.setOnClickListener(v ->
                Toast.makeText(getContext(), "Tour của tôi", Toast.LENGTH_SHORT).show());

        btnGoToFavorites.setOnClickListener(v ->
                Toast.makeText(getContext(), "Tour yêu thích", Toast.LENGTH_SHORT).show());

        btnSupport.setOnClickListener(v ->
                Toast.makeText(getContext(), "Trợ giúp & Hỗ trợ", Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            // TODO: FirebaseAuth.getInstance().signOut();
            // startActivity(new Intent(getActivity(), LoginActivity.class));
            // getActivity().finish();
        });

        return view;
    }
}
