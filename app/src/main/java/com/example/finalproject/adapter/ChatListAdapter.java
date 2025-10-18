package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.example.finalproject.entity.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    private final List<Chat> chatList;
    private final OnChatClickListener listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Cache thông tin user để tránh load lại nhiều lần
    private final Map<String, UserInfo> userCache = new HashMap<>();

    // Constructor
    public ChatListAdapter(List<Chat> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    // Tạo ViewHolder cho từng item
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    // Liên kết dữ liệu với ViewHolder
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        // Lấy UID của người còn lại trong chat
        String otherUserId = null;
        if (chat.getMembers() != null) {
            for (String uid : chat.getMembers()) {
                if (!uid.equals(currentUserId)) {
                    otherUserId = uid;
                    break;
                }
            }
        }

        if (otherUserId != null) {
            loadUserInfo(otherUserId, holder);
        } else {
            setDefaultUser(holder);
        }

        // Hiển thị tin nhắn cuối
        if (chat.getLastMessage() != null && !chat.getLastMessage().isEmpty()) {
            holder.txtLastMessage.setText(chat.getLastMessage());
            holder.txtLastMessage.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.black));
        } else {
            holder.txtLastMessage.setText("Chưa có tin nhắn");
            holder.txtLastMessage.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.darker_gray));
        }

        // Hiển thị thời gian
        if (chat.getLastMessageTime() != null) {
            Date date = chat.getLastMessageTime().toDate();
            holder.txtTime.setText(formatTime(date));
        } else {
            holder.txtTime.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(chat);
        });
    }

    // Tải thông tin user từ Firestore hoặc cache
    private void loadUserInfo(String uid, ViewHolder holder) {
        // Kiểm tra cache trước
        if (userCache.containsKey(uid)) {
            bindUserInfo(holder, userCache.get(uid));
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String firstName = snapshot.getString("firstname");
                        String lastName = snapshot.getString("lastname");
                        String fullName = snapshot.getString("fullName");
                        String role = snapshot.getString("role");
                        String avatarUrl = snapshot.getString("avatarUrl");

                        String displayName = getDisplayName(fullName, firstName, lastName);
                        UserInfo userInfo = new UserInfo(displayName, role, avatarUrl);

                        userCache.put(uid, userInfo);
                        bindUserInfo(holder, userInfo);
                    } else {
                        setDefaultUser(holder);
                    }
                })
                .addOnFailureListener(e -> setDefaultUser(holder));
    }

    // Liên kết thông tin user vào ViewHolder
    private void bindUserInfo(ViewHolder holder, UserInfo userInfo) {
        holder.txtName.setText(userInfo.displayName);

        if (userInfo.role != null && !userInfo.role.isEmpty()) {
            holder.txtRole.setText(userInfo.role);
            holder.txtRole.setVisibility(View.VISIBLE);
            holder.txtRole.setBackgroundResource(getRoleBadge(userInfo.role));
        } else {
            holder.txtRole.setVisibility(View.GONE);
        }

        if (userInfo.avatarUrl != null && !userInfo.avatarUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(userInfo.avatarUrl)
                    .placeholder(R.drawable.ic_account)
                    .error(R.drawable.ic_account)
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_account);
        }
    }

    // Thiết lập thông tin mặc định khi không lấy được user
    private void setDefaultUser(ViewHolder holder) {
        holder.txtName.setText("Người dùng");
        holder.txtRole.setVisibility(View.GONE);
        holder.imgAvatar.setImageResource(R.drawable.ic_account);
    }

    // Tạo tên hiển thị từ các trường tên
    private String getDisplayName(String fullName, String firstName, String lastName) {
        if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();
        StringBuilder sb = new StringBuilder();
        if (lastName != null && !lastName.trim().isEmpty()) sb.append(lastName.trim());
        if (firstName != null && !firstName.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(firstName.trim());
        }
        return sb.length() > 0 ? sb.toString() : "Người dùng";
    }

    // Lấy badge tương ứng với role
    private int getRoleBadge(String role) {
        if (role == null) return R.drawable.badge_customer;
        switch (role.toLowerCase(Locale.ROOT)) {
            case "admin":
                return R.drawable.badge_admin;
            case "guide":
                return R.drawable.badge_guide;
            default:
                return R.drawable.badge_customer;
        }
    }

    // Định dạng thời gian hiển thị
    private String formatTime(Date date) {
        long diffInMillis = System.currentTimeMillis() - date.getTime();
        long diffInHours = diffInMillis / (1000 * 60 * 60);
        long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

        if (diffInHours < 24) {
            // Hiển thị giờ và phút
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } else if (diffInDays < 7) {
            // Hiển thị ngày trong tuần
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
        } else {
            // Hiển thị ngày/tháng
            return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(date);
        }
    }

    // Tổng số item trong danh sách
    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    // Cập nhật danh sách chat
    public void updateChatList(List<Chat> newChatList) {
        chatList.clear();
        chatList.addAll(newChatList);
        notifyDataSetChanged();
    }

    // Xóa cache user
    public void clearCache() {
        userCache.clear();
    }

    // Lớp lưu trữ thông tin user
    private static class UserInfo {
        String displayName, role, avatarUrl;
        UserInfo(String name, String role, String avatar) {
            this.displayName = name;
            this.role = role;
            this.avatarUrl = avatar;
        }
    }

    // ViewHolder cho item chat
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtLastMessage, txtTime, txtRole;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtRole = itemView.findViewById(R.id.txtRole);
        }
    }

    // Tìm kiếm chat theo tên người dùng
}
