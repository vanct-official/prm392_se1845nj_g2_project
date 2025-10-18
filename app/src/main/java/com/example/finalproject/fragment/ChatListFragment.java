package com.example.finalproject.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.finalproject.R;
import com.example.finalproject.adapter.ChatListAdapter;
import com.example.finalproject.entity.Chat;
import com.example.finalproject.ChatActivity;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerChatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        currentUserId = currentUser.getUid();

        adapter = new ChatListAdapter(chatList, chat -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("chatId", chat.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        loadChats();

        // 👉 Giả sử bạn có nút tìm user mới (ví dụ FloatingActionButton)
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabNewChat);
         fab.setOnClickListener(v -> showSearchUserDialog());

        return view;
    }

    // ===========================================================
    // 🔹 Tải danh sách chat của người dùng hiện tại
    // ===========================================================
    private void loadChats() {
        db.collection("chats")
                .whereArrayContains("members", currentUserId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Error: ", e);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d("Firestore", "No chats found for user: " + currentUserId);
                        chatList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    chatList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.setId(doc.getId());
                            chatList.add(chat);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // ===========================================================
    // 🔎 Tạo 1 phòng chat mới nếu chưa có lịch sử chat với người đó
    // ===========================================================
    private void getOrCreateChatRoom(String otherUserId, OnChatRoomReady callback) {
        String currentUserId = currentUser.getUid();

        // Tạo list userId (2 thành viên)
        List<String> members = Arrays.asList(currentUserId, otherUserId);

        // Kiểm tra phòng chat có tồn tại chưa
        db.collection("chats")
                .whereEqualTo("type", "private")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        List<String> existingMembers = (List<String>) doc.get("members");
                        if (existingMembers != null && existingMembers.contains(otherUserId)) {
                            // Đã có phòng
                            callback.onReady(doc.getId());
                            return;
                        }
                    }

                    // Nếu chưa có phòng → tạo mới
                    if(currentUserId.equals(otherUserId)) {
                        Toast.makeText(getContext(), "Không thể tạo chat với chính mình!", Toast.LENGTH_SHORT).show();
                        return;
                    } else if(otherUserId == null || otherUserId.isEmpty()) {
                        Toast.makeText(getContext(), "Người dùng không hợp lệ!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> newChat = new HashMap<>();
                    newChat.put("members", members);
                    newChat.put("type", "private");
                    newChat.put("lastMessage", "");
                    newChat.put("lastMessageTime", FieldValue.serverTimestamp());

                    db.collection("chats").add(newChat)
                            .addOnSuccessListener(documentReference -> {
                                callback.onReady(documentReference.getId());
                            });
                });
    }

    // ===========================================================
    // 🔍 Dialog tìm user qua email hoặc số điện thoại
    // ===========================================================
    private void showSearchUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_find_user, null);
        builder.setView(dialogView);

        EditText edtSearchInput = dialogView.findViewById(R.id.edtSearchInput);
        Button btnSearch = dialogView.findViewById(R.id.btnSearch);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSearch.setOnClickListener(v -> {
            String keyword = edtSearchInput.getText().toString().trim();
            if (keyword.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 🔍 Tìm theo email trước
            db.collection("users")
                    .whereEqualTo("email", keyword)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                            String userId = userDoc.getId();
                            String name = userDoc.getString("name");
                            Toast.makeText(getContext(), "Tìm thấy: " + name, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();

                            // 👉 Sau khi tìm thấy user, tạo hoặc mở phòng chat
                            getOrCreateChatRoom(userId, chatId -> {
                                Intent intent = new Intent(getContext(), ChatActivity.class);
                                intent.putExtra("chatId", chatId);
                                startActivity(intent);
                            });

                        } else {
                            // 🔍 Nếu không có kết quả theo email thì thử phone
                            db.collection("users")
                                    .whereEqualTo("phone", keyword)
                                    .get()
                                    .addOnSuccessListener(phoneSnapshot -> {
                                        if (!phoneSnapshot.isEmpty()) {
                                            DocumentSnapshot userDoc = phoneSnapshot.getDocuments().get(0);
                                            String userId = userDoc.getId();
                                            String name = userDoc.getString("name");
                                            Toast.makeText(getContext(), "Tìm thấy: " + name, Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();

                                            getOrCreateChatRoom(userId, chatId -> {
                                                Intent intent = new Intent(getContext(), ChatActivity.class);
                                                intent.putExtra("chatId", chatId);
                                                startActivity(intent);
                                            });

                                        } else {
                                            Toast.makeText(getContext(), "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // ===========================================================
    // Callback interface
    // ===========================================================
    public interface OnChatRoomReady {
        void onReady(String chatId);
    }
}
