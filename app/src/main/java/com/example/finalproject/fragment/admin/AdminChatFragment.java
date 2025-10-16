package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;

public class AdminChatFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout đơn giản
        View view = inflater.inflate(R.layout.fragment_admin_chat, container, false);

        // Tìm TextView và set text
        TextView tvMessage = view.findViewById(R.id.tvChart);
        tvMessage.setText("Đây là trang đặt chuyến tham quan hay trang đặt tour, hoặc trang đóe gì đấy");

        return view;
    }
}
