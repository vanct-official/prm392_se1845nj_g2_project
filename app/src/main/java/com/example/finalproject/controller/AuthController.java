package com.example.finalproject.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.finalproject.AdminActivity;
import com.example.finalproject.CustomerActivity;
import com.example.finalproject.GuideActivity;
import com.example.finalproject.entity.User;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AuthController {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Context context;

    public AuthController(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Đăng nhập bằng Google account
     * Lấy email từ GoogleSignInAccount, query Firestore User collection
     * Chuyển sang Activity theo role
     */
    public void loginWithGoogle(GoogleSignInAccount account) {
        if (account == null || account.getEmail() == null) return;

        String email = account.getEmail();

        // Query Firestore để lấy user theo email
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                Log.d("AuthController", "User found: " + doc.getData());
                                User user = doc.toObject(User.class);
                                Log.d("AuthController", "Role: " + user.getRole());
                                redirectByRole(user);
                                break;
                            }
                        } else {
                            Log.e("AuthController", "No user found with email: " + email);
                        }
                    } else {
                        Log.e("AuthController", "Firestore error: ", task.getException());
                    }
                });

    }

    /*
    * Đăng nhập bằng email/password Firebase Auth
    * Lấy email từ FirebaseUser, query Firestore User collection
    * Chuyển sang Activity theo role
    * */
    public void loginWithEmail(String email, String passwordHash) {
        auth.signInWithEmailAndPassword(email, passwordHash)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        db.collection("users")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    if (!snapshot.isEmpty()) {
                                        User user = snapshot.getDocuments().get(0).toObject(User.class);
                                        redirectByRole(user);
                                    }
                                });
                    } else {
                        Toast.makeText(context, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Chuyển sang Activity theo role
     */
    private void redirectByRole(User user) {
        Intent intent = null;
        switch (user.getRole()) {
            case "customer":
                intent = new Intent(context, CustomerActivity.class);
                break;
            case "guide":
                intent = new Intent(context, GuideActivity.class);
                break;
            case "admin":
                intent = new Intent(context, AdminActivity.class);
                break;
            default:
                // Role không xác định, có thể show Toast
                return;
        }

        // Bật activity mới
        if (context instanceof Activity) {
            context.startActivity(intent);
            ((Activity) context).finish(); // đóng LoginActivity
        }
    }

}
