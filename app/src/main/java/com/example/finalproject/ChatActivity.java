package com.example.finalproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.adapter.MessageAdapter;
import com.example.finalproject.adapter.SelectedImagesAdapter;
import com.example.finalproject.entity.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.cloudinary.utils.ObjectUtils;
import com.example.finalproject.utils.CloudinaryManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;


import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;

    private RecyclerView recyclerViewMessages, recyclerViewSelectedImages;
    private EditText inputMessage;
    private ImageButton btnSend, btnSendImage, btnBack;
    private ImageView imgUserAvatar;
    private TextView txtUserName;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String chatId, currentUserId, otherUserId;
    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private List<Uri> selectedImages = new ArrayList<>();
    private SelectedImagesAdapter selectedImagesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        initFirebase();
        setupRecyclerViews();
        loadChatHeader();
        loadMessages();
        setupListeners();
    }

    private void initViews() {
        recyclerViewMessages = findViewById(R.id.recyclerMessages);
        recyclerViewSelectedImages = findViewById(R.id.recyclerSelectedImages);
        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        btnSendImage = findViewById(R.id.btnSendImage);
        btnBack = findViewById(R.id.btnBack);
        imgUserAvatar = findViewById(R.id.imgUserAvatar);
        txtUserName = findViewById(R.id.txtUserName);

        chatId = getIntent().getStringExtra("chatId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void setupRecyclerViews() {
        // Messages
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        // Selected Images Preview
        selectedImagesAdapter = new SelectedImagesAdapter(this, selectedImages, position -> {
            selectedImages.remove(position);
            selectedImagesAdapter.notifyDataSetChanged();
            recyclerViewSelectedImages.setVisibility(selectedImages.isEmpty() ? View.GONE : View.VISIBLE);
        });
        recyclerViewSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewSelectedImages.setAdapter(selectedImagesAdapter);
        recyclerViewSelectedImages.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSendImage.setOnClickListener(v -> openGallery());

        btnSend.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();
            if (!selectedImages.isEmpty()) sendImages();
            if (!text.isEmpty()) sendMessage(text);
        });
    }

    private void loadChatHeader() {
        if (chatId == null) return;

        db.collection("chats").document(chatId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.get("members") != null) {
                        List<String> members = (List<String>) doc.get("members");
                        for (String uid : members) {
                            if (!uid.equals(currentUserId)) {
                                otherUserId = uid;
                                break;
                            }
                        }
                        loadUserInfo();
                    }
                });
    }

    private void loadUserInfo() {
        if (otherUserId == null) return;

        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;

                    String firstName = userDoc.getString("firstname");
                    String lastName = userDoc.getString("lastname");
                    String displayName = ((lastName != null) ? lastName.trim() : "") +
                            ((firstName != null && !firstName.trim().isEmpty()) ? " " + firstName.trim() : "");
                    txtUserName.setText(!displayName.isEmpty() ? displayName : "Người dùng");

                    String avatarUrl = userDoc.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_account)
                                .error(R.drawable.ic_account)
                                .circleCrop()
                                .into(imgUserAvatar);
                    }
                });
    }

    private void loadMessages() {
        if (chatId == null) return;

        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    messageList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) {
                            msg.setId(doc.getId());
                            messageList.add(msg);
                        }
                    }
                    messageAdapter.notifyDataSetChanged();
                    recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                });
    }

    private void sendMessage(String text) {
        if (chatId == null || chatId.isEmpty()) {
            createNewChat(() -> sendMessageToChat(chatId, text, null));
        } else {
            sendMessageToChat(chatId, text, null);
        }
        inputMessage.setText("");
    }

    private void sendImages() {
        if (chatId == null || chatId.isEmpty()) return;

        for (Uri uri : selectedImages) {
            uploadImageAndSend(uri); // ✅ dùng Cloudinary
        }
        selectedImages.clear();
        selectedImagesAdapter.notifyDataSetChanged();
        recyclerViewSelectedImages.setVisibility(View.GONE);
    }


    // Trong file ChatActivity.java

    private void uploadImageAndSend(Uri uri) {
        new Thread(() -> {
            try {
                // 1. Chuyển Uri → Bitmap → byte[]
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] data = baos.toByteArray();

                // 2. Upload lên Cloudinary
                Map uploadResult = CloudinaryManager.getInstance()
                        .uploader()
                        .upload(data, ObjectUtils.emptyMap());

                String imageUrl = (String) uploadResult.get("secure_url");

                // 3. Lưu message vào Firestore
                runOnUiThread(() -> saveMessageToFirestore(chatId, null, imageUrl));

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "❌ Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }



    private void saveMessageToFirestore(String chatId, String text, String imageUrl) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("timestamp", FieldValue.serverTimestamp());
        msg.put("seenBy", Collections.singletonList(currentUserId));
        if (text != null) msg.put("content", text);
        if (imageUrl != null) msg.put("imageUrl", imageUrl);

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(doc -> {
                    String lastMsg = (text != null) ? text : "[Hình ảnh]";
                    db.collection("chats").document(chatId)
                            .update("lastMessage", lastMsg, "lastMessageTime", FieldValue.serverTimestamp());
                });
    }

    private void sendMessageToChat(String chatId, String text, Uri imageUri) {
        if (imageUri != null) {
            uploadImageAndSend(imageUri); // Cloudinary upload
            if (text != null && !text.isEmpty()) {
                saveMessageToFirestore(chatId, text, null); // gửi text riêng
            }
        } else if (text != null && !text.isEmpty()) {
            saveMessageToFirestore(chatId, text, null);
        }
    }


    private void createNewChat(Runnable afterCreate) {
        if (otherUserId == null) return;

        Map<String, Object> chat = new HashMap<>();
        chat.put("members", Arrays.asList(currentUserId, otherUserId));
        chat.put("lastMessage", "[Tin nhắn mới]");
        chat.put("lastMessageTime", FieldValue.serverTimestamp());
        chat.put("type", "private");

        db.collection("chats").add(chat)
                .addOnSuccessListener(doc -> {
                    chatId = doc.getId();
                    afterCreate.run();
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    selectedImages.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedImages.add(data.getData());
            }
            selectedImagesAdapter.notifyDataSetChanged();
            recyclerViewSelectedImages.setVisibility(View.VISIBLE);
        }
    }
}
