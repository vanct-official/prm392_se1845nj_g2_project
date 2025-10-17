package com.example.finalproject.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.finalproject.R;
import com.example.finalproject.adapter.ChatListAdapter;
import com.example.finalproject.entity.Chat;
import com.example.finalproject.ChatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerChatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new ChatListAdapter(chatList, chat -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("chatId", chat.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        loadChats();

        return view;
    }

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
                        return;
                    }

                    chatList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Log.d("Firestore", "Loaded chat: " + doc.getId());
                        Chat chat = doc.toObject(Chat.class);
                        chat.setId(doc.getId());
                        chatList.add(chat);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

}
