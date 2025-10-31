package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.admin.AdminManageUserAdapter;
import com.example.finalproject.entity.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminManageUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private EditText edtSearchUser;
    private AdminManageUserAdapter adapter;
    private final List<User> userList = new ArrayList<>();
    private final List<User> filteredList = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_manage_users, container, false);

        rvUsers = view.findViewById(R.id.rvUsersAdmin);
        edtSearchUser = view.findViewById(R.id.edtSearchUser);

        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminManageUserAdapter(filteredList, getContext());
        rvUsers.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadUsers();

        // Tìm kiếm user theo tên hoặc email
        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    /** 🔹 Lấy danh sách user từ Firestore */
    private void loadUsers() {
        db.collection("users").get()
                .addOnSuccessListener(query -> {
                    userList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        User user = doc.toObject(User.class);
                        user.setUserid(doc.getId());
                        userList.add(user);
                    }
                    filteredList.clear();
                    filteredList.addAll(userList);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** 🔍 Lọc user theo tên hoặc email */
    private void filterUsers(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(userList);
        } else {
            String q = query.toLowerCase(Locale.getDefault());
            for (User u : userList) {
                String fullName = (u.getFirstname() + " " + u.getLastname()).toLowerCase(Locale.getDefault());
                if (fullName.contains(q) || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))) {
                    filteredList.add(u);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
