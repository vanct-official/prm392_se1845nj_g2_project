package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;

import java.util.ArrayList;
import java.util.List;

public class AdminManageUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private EditText edtSearchUser;
    private UserAdapter adapter;
    private final List<User> userList = new ArrayList<>();
    private final List<User> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_manage_users_demo, container, false);

        rvUsers = view.findViewById(R.id.rvUsersAdmin);
        edtSearchUser = view.findViewById(R.id.edtSearchUser);

        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserAdapter(filteredList);
        rvUsers.setAdapter(adapter);

        // Fake data
        userList.clear();
        userList.add(new User("Nguyễn Văn A", "vana@gmail.com", true));
        userList.add(new User("Trần Thị B", "thib@gmail.com", false));
        userList.add(new User("Lê C", "lec@gmail.com", true));
        userList.add(new User("Phạm D", "phamd@gmail.com", true));
        userList.add(new User("Hoàng E", "hoange@gmail.com", false));

        filteredList.clear();
        filteredList.addAll(userList);
        adapter.notifyDataSetChanged();

        // Tìm kiếm user
        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void filterUsers(String query) {
        filteredList.clear();
        if(query == null || query.trim().isEmpty()){
            filteredList.addAll(userList);
        } else {
            String q = query.toLowerCase();
            for(User u : userList){
                if(u.name.toLowerCase().contains(q) || u.email.toLowerCase().contains(q)){
                    filteredList.add(u);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- Model + Adapter ---
    private static class User {
        String name;
        String email;
        boolean active; // true = active, false = banned

        User(String name, String email, boolean active){
            this.name = name;
            this.email = email;
            this.active = active;
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        List<User> items;
        UserAdapter(List<User> items){ this.items = items; }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_user_demo, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User u = items.get(position);
            holder.tvName.setText(u.name);
            holder.tvEmail.setText(u.email);

            holder.btnToggle.setText(u.active ? "Hủy tài khoản" : "Kích hoạt");
            holder.btnToggle.setBackgroundColor(
                    getResources().getColor(u.active ? R.color.status_cancelled : R.color.status_confirmed, null)
            );

            holder.btnToggle.setOnClickListener(v -> {
                u.active = !u.active;
                notifyItemChanged(position);
                Toast.makeText(getContext(),
                        "Đã " + (u.active ? "kích hoạt" : "hủy") + " tài khoản: " + u.name,
                        Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail;
            Button btnToggle;

            UserViewHolder(@NonNull View itemView){
                super(itemView);
                tvName = itemView.findViewById(R.id.tvUserNameAdmin);
                tvEmail = itemView.findViewById(R.id.tvUserEmailAdmin);
                btnToggle = itemView.findViewById(R.id.btnToggleUser);
            }
        }
    }
}
