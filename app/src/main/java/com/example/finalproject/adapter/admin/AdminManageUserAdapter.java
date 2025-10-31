package com.example.finalproject.adapter.admin;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.activity.admin.AdminUserDetailActivity;
import com.example.finalproject.entity.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminManageUserAdapter extends RecyclerView.Adapter<AdminManageUserAdapter.UserViewHolder> {

    private final List<User> users;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AdminManageUserAdapter(List<User> users, Context context) {
        this.users = users;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);

        String fullName = (user.getFirstname() != null ? user.getFirstname() : "")
                + " " + (user.getLastname() != null ? user.getLastname() : "");
        holder.tvName.setText(fullName.trim().isEmpty() ? "Không tên" : fullName.trim());
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Không có email");

        // ✅ Nếu là admin → disable nút
        if ("admin".equalsIgnoreCase(user.getRole())) {
            holder.btnToggle.setText("Vô hiệu hóa");
            holder.btnToggle.setEnabled(false);
            holder.btnToggle.setBackgroundColor(context.getResources().getColor(R.color.gray_disabled, null));
        } else {
            boolean isActive = user.getIsActive();
            holder.btnToggle.setText(isActive ? "Vô hiệu hóa" : "Kích hoạt");
            holder.btnToggle.setBackgroundColor(
                    context.getResources().getColor(isActive ? R.color.status_cancelled : R.color.status_confirmed, null)
            );

            // ✅ Nút kích hoạt / vô hiệu hóa
            holder.btnToggle.setOnClickListener(v -> toggleUserActive(user, position));
        }

        // ✅ Click mở chi tiết
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminUserDetailActivity.class);
            intent.putExtra("userId", user.getUserid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /** ✅ Toggle trạng thái hoạt động của user */
    private void toggleUserActive(User user, int position) {
        boolean newStatus = !user.getIsActive();
        DocumentReference ref = db.collection("users").document(user.getUserid());

        ref.update("isActive", newStatus)
                .addOnSuccessListener(unused -> {
                    user.setIsActive(newStatus);
                    notifyItemChanged(position);
                    Toast.makeText(context,
                            "Đã " + (newStatus ? "kích hoạt" : "vô hiệu hóa") +
                                    " tài khoản: " + user.getFirstname() + " " + user.getLastname(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** ViewHolder hiển thị từng user */
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        Button btnToggle;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserNameAdmin);
            tvEmail = itemView.findViewById(R.id.tvUserEmailAdmin);
            btnToggle = itemView.findViewById(R.id.btnToggleUser);
        }
    }
}
