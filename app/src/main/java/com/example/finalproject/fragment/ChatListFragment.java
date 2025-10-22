// java
package com.example.finalproject.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.finalproject.R;
import com.example.finalproject.adapter.ChatListAdapter;
import com.example.finalproject.entity.Chat;
import com.example.finalproject.ChatActivity;
import com.google.android.material.chip.ChipGroup;
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
    private static final String TAG = "ChatListFragment";

    private EditText edtSearchUser;
    private ImageButton btnClearSearch;


    private RecyclerView recyclerView;
    private ChatListAdapter adapter;

    // chatList: all chats returned from Firestore
    private List<Chat> chatList = new ArrayList<>();

    // displayList: filtered list shown in adapter
    private List<Chat> displayList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String currentUserId;
    // cache userId -> name to avoid repeated Firestore hits
    private Map<String, String> userNameCache = new HashMap<>();

    // cache userId -> role to avoid repeated Firestore hits
    private Map<String, String> userRoleCache = new HashMap<>();

    // current role filter (null means "all")
    private String currentRoleFilter = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        // guard: ensure user logged in
        if (currentUser == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            // you may want to redirect to login
        } else {
            currentUserId = currentUser.getUid();
        }

        edtSearchUser = view.findViewById(R.id.edtSearchUser);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        recyclerView = view.findViewById(R.id.recyclerChatList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        // Adapter uses displayList (which we will update when chats or filters change)
        adapter = new ChatListAdapter(displayList, chat -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("chatId", chat.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        loadChats();

        ChipGroup chipGroup = view.findViewById(R.id.chipGroupFilter);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) {
                currentRoleFilter = null;
            } else if (checkedId == R.id.chipAdmin) {
                currentRoleFilter = "admin";
            } else if (checkedId == R.id.chipGuide) {
                currentRoleFilter = "guide";
            } else if (checkedId == R.id.chipCustomer) {
                currentRoleFilter = "customer";
            } else {
                currentRoleFilter = null;
            }
            // rebuild displayList from chatList + cache
            updateDisplayList();
        });

        // Floating action button: mở dialog tìm user
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabNewChat);
        fab.setOnClickListener(v -> showSearchUserDialog());
        setupSearchBar();
        return view;
    }

    // ===========================================================
    // 🔹 Tải danh sách chat của người dùng hiện tại
    // - Khi có snapshot mới: cập nhật chatList, sau đó fetch role người còn lại (nếu cần)
    // ===========================================================
    private void loadChats() {
        if (currentUserId == null) return;

        db.collection("chats")
                .whereArrayContains("members", currentUserId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening chats: ", e);
                        return;
                    }

                    chatList.clear();
                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "No chats found for user: " + currentUserId);
                        updateDisplayList();
                        return;
                    }

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Chat chat = doc.toObject(Chat.class);
                        if (chat != null) {
                            chat.setId(doc.getId());

                            // safely read members as a typed List\<String\>
                            List<String> docMembers = null;
                            Object rawMembers = doc.get("members");
                            if (rawMembers instanceof List) {
                                try {
                                    docMembers = (List<String>) rawMembers;
                                } catch (ClassCastException ex) {
                                    // defensive: cannot cast element types, ignore
                                    docMembers = null;
                                }
                            }

                            chatList.add(chat);

                            // If members available, use them to prefetch role/name
                            if (docMembers != null && docMembers.size() >= 2) {
                                String otherUserId = docMembers.get(0).equals(currentUserId) ? docMembers.get(1) : docMembers.get(0);

                                if (!userRoleCache.containsKey(otherUserId)) {
                                    // fetch role once and cache it
                                    String uid = otherUserId;
                                    db.collection("users").document(uid).get()
                                            .addOnSuccessListener(userDoc -> {
                                                if (userDoc.exists()) {
                                                    String otherRole = userDoc.getString("role");
                                                    userRoleCache.put(uid, otherRole != null ? otherRole.toLowerCase() : null);
                                                } else {
                                                    userRoleCache.put(uid, null);
                                                }
                                                // after updating cache, refresh display list
                                                updateDisplayList();
                                            })
                                            .addOnFailureListener(err -> {
                                                Log.w(TAG, "Failed get user role for " + uid, err);
                                            });
                                }

                                // prefetch partner info (fetchPartnerInfo is guarded by cache)
                                if (!userNameCache.containsKey(otherUserId)) {
                                    fetchPartnerInfo(chat);
                                }
                            }

                            // end for this doc
                        }
                    }

                    // finally update displayed list after initial chatList populated
                    updateDisplayList();
                });
    }

    // ===========================================================
    // 🔎 Tạo 1 phòng chat mới nếu chưa có lịch sử chat với người đó
    // ===========================================================
    private void getOrCreateChatRoom(String otherUserId, OnChatRoomReady callback) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserIdLocal = currentUser.getUid();

        // Tạo list userId (2 thành viên)
        List<String> members = Arrays.asList(currentUserIdLocal, otherUserId);

        // Kiểm tra phòng chat có tồn tại chưa
        db.collection("chats")
                .whereEqualTo("type", "private")
                .whereArrayContains("members", currentUserIdLocal)
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
                    if (currentUserIdLocal.equals(otherUserId)) {
                        Toast.makeText(getContext(), "Không thể tạo chat với chính mình!", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (otherUserId == null || otherUserId.isEmpty()) {
                        Toast.makeText(getContext(), "Người dùng không hợp lệ!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> newChat = new HashMap<>();
                    newChat.put("members", members);
                    newChat.put("type", "private");
                    newChat.put("lastMessage", "");
                    newChat.put("lastMessageTime", FieldValue.serverTimestamp());
                    newChat.put("createdBy", currentUserIdLocal);

                    db.collection("chats").add(newChat)
                            .addOnSuccessListener(documentReference -> {
                                callback.onReady(documentReference.getId());
                            })
                            .addOnFailureListener(err -> {
                                Log.e(TAG, "Failed create chat", err);
                                Toast.makeText(getContext(), "Tạo phòng chat thất bại", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Error checking existing chats", err);
                    Toast.makeText(getContext(), "Lỗi kiểm tra phòng chat", Toast.LENGTH_SHORT).show();
                });
    }

    // ===========================================================
    // 🔍 Dialog tìm user qua email hoặc số điện thoại (giữ nguyên logic)
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

            // disable button to prevent spam
            btnSearch.setEnabled(false);

            // 🔍 Tìm theo email trước
            db.collection("users")
                    .whereEqualTo("email", keyword)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                            handleFoundUserDoc(userDoc, dialog);
                        } else {
                            // Nếu không có kết quả theo email thì thử phone
                            db.collection("users")
                                    .whereEqualTo("phone", keyword)
                                    .get()
                                    .addOnSuccessListener(phoneSnapshot -> {
                                        if (!phoneSnapshot.isEmpty()) {
                                            DocumentSnapshot userDoc = phoneSnapshot.getDocuments().get(0);
                                            handleFoundUserDoc(userDoc, dialog);
                                        } else {
                                            Toast.makeText(getContext(), "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                                        }
                                        btnSearch.setEnabled(true);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        btnSearch.setEnabled(true);
                                    });
                        }
                        btnSearch.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSearch.setEnabled(true);
                    });
        });
    }

    private void handleFoundUserDoc(DocumentSnapshot userDoc, AlertDialog dialog) {
        if (userDoc == null) return;
        String userId = userDoc.getId();
        String firstname = userDoc.getString("firstname");
        String lastname = userDoc.getString("lastname");
        String name = (firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "");
        String role = userDoc.getString("role");
        Toast.makeText(getContext(), "Tìm thấy: " + (name != null ? name : userId), Toast.LENGTH_SHORT).show();
        dialog.dismiss();

        // cache role immediately
        userRoleCache.put(userId, role != null ? role.toLowerCase() : null);

        getOrCreateChatRoom(userId, chatId -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("chatId", chatId);
            startActivity(intent);
        });
    }

    // ===========================================================
    // 🔎 Xử lý thanh tìm kiếm trong Fragment
    // ===========================================================
    private void setupSearchBar() {
        edtSearchUser.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();

                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                }

                filterChats(query);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            edtSearchUser.setText("");
        });
    }

    private void fetchPartnerInfo(Chat chat) {
        List<String> members = chat.getMembers();
        if (members == null || members.size() < 2) return;

        String otherUserId = members.get(0).equals(currentUserId) ? members.get(1) : members.get(0);

        // Nếu đã có cache thì bỏ qua
        if (userNameCache.containsKey(otherUserId)) {
            chat.setPartnerName(userNameCache.get(otherUserId));
            chat.setPartnerRole(userRoleCache.get(otherUserId));
            adapter.notifyDataSetChanged();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // try multiple name fields
                        String fullName = snapshot.getString("fullName");
                        if (fullName == null || fullName.trim().isEmpty()) {
                            String firstname = snapshot.getString("firstname");
                            String lastname = snapshot.getString("lastname");
                            fullName = ((firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "")).trim();
                        }                        String role = snapshot.getString("role");

                        userNameCache.put(otherUserId, fullName != null ? fullName : otherUserId);
                        userRoleCache.put(otherUserId, role != null ? role.toLowerCase() : null);

                        chat.setPartnerName(fullName);
                        chat.setPartnerRole(role);
                        adapter.notifyDataSetChanged();
                    }else {
                        userNameCache.put(otherUserId, "");
                        userRoleCache.put(otherUserId, null);
                    }
                }).addOnFailureListener(err -> {
                    Log.w(TAG, "Failed to fetch partner info for " + otherUserId, err);
                });
    }


    // ===========================================================
    // 🔍 Lọc danh sách chat theo từ khóa + role (nếu có)
    // ===========================================================
    private void filterChats(String query) {
        displayList.clear();
        String q = query == null ? "" : query.toLowerCase().trim();

        for (Chat chat : chatList) {
            List<String> members = chat.getMembers();
            if (members == null || members.size() < 2) continue;

            String otherUserId = members.get(0).equals(currentUserId) ? members.get(1) : members.get(0);
            String otherRole = userRoleCache.get(otherUserId);

            // If filtering by role and roles don't match, skip
            if (currentRoleFilter != null && (otherRole == null || !otherRole.equalsIgnoreCase(currentRoleFilter))) {
                continue;
            }

            // Prefetch partner info if missing (async; will help subsequent searches)
            if (!userNameCache.containsKey(otherUserId)) {
                fetchPartnerInfo(chat);
            }

            // Determine partner name (prefer cache, fallback to chat's partnerName)
            String partnerName = userNameCache.get(otherUserId);
            if (partnerName == null) {
                partnerName = chat.getPartnerName();
            }
            partnerName = partnerName != null ? partnerName.toLowerCase() : "";

            String lastMessage = chat.getLastMessage() != null ? chat.getLastMessage().toLowerCase() : "";

            boolean matchesQuery = q.isEmpty() || partnerName.contains(q) || lastMessage.contains(q);

            if (matchesQuery) {
                displayList.add(chat);
            }
        }

        adapter.notifyDataSetChanged();
    }


    // ===========================================================
    // Update displayList according to currentRoleFilter and notify adapter
    // ===========================================================
    private void updateDisplayList() {
        displayList.clear();
        if (currentRoleFilter == null) {
            // show all
            for (Chat chat : chatList) {
                // prefetch name if missing
                List<String> members = chat.getMembers();
                if (members != null && members.size() >= 2) {
                    String otherUserId = members.get(0).equals(currentUserId) ? members.get(1) : members.get(0);
                    if (!userNameCache.containsKey(otherUserId)) {
                        fetchPartnerInfo(chat);
                    }
                }
                displayList.add(chat);
            }
        } else {
            for (Chat chat : chatList) {
                List<String> members = chat.getMembers();
                if (members == null || members.size() < 2) continue;
                String otherUserId = members.get(0).equals(currentUserId) ? members.get(1) : members.get(0);
                String otherRole = userRoleCache.get(otherUserId); // may be null if not fetched yet

                // prefetch name if missing
                if (!userNameCache.containsKey(otherUserId)) {
                    fetchPartnerInfo(chat);
                }

                if (otherRole != null) {
                    if (otherRole.equalsIgnoreCase(currentRoleFilter)) {
                        displayList.add(chat);
                    }
                } else {
                    // role unknown yet: include and rely on subsequent cache fetch + update
                    displayList.add(chat);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ===========================================================
    // Callback interface
    // ===========================================================
    public interface OnChatRoomReady {
        void onReady(String chatId);
    }
}
