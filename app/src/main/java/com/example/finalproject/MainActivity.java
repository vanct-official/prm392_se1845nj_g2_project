package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalproject.entity.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Debug: kiểm tra kiểu dữ liệu của dob, tránh crash
        db.collection("users").get().addOnSuccessListener(query -> {
            for (DocumentSnapshot doc : query.getDocuments()) {
                Timestamp dob = doc.getTimestamp("dob");
                if (dob != null) {
                    Log.d("DOB_CHECK", doc.getId() + " -> " + dob.toDate());
                } else {
                    Log.d("DOB_CHECK", doc.getId() + " -> null (chưa có DOB)");
                }
            }
        });

        checkLogin();
    }

    private void checkLogin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Kiểm tra nếu email chưa verify thì không cho vào app
            if (!currentUser.isEmailVerified()) {
                Toast.makeText(this, "Vui lòng xác thực email trước khi đăng nhập!", Toast.LENGTH_LONG).show();
                mAuth.signOut();
                goToLogin();
                return;
            }

            // Nếu email đã xác thực → lấy thông tin user trong Firestore
            fetchUserRole(currentUser.getUid());
        } else {
            goToLogin();
        }
    }

    private void fetchUserRole(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getRole() != null) {
                            Boolean isActive = user.getIsActive();
                            if (isActive != null && isActive) {
                                redirectByRole(user.getRole());
                            } else {
                                Toast.makeText(this, "Tài khoản đã bị khóa", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                                goToLogin();
                            }
                        } else {
                            goToLogin();
                        }
                    } else {
                        goToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Lỗi lấy dữ liệu user: ", e);
                    goToLogin();
                });
    }

    private void redirectByRole(String role) {
        Intent intent;
        switch (role.toLowerCase()) {
            case "admin":
                intent = new Intent(this, AdminActivity.class);
                break;
            case "guide":
                intent = new Intent(this, GuideActivity.class);
                break;
            default:
                intent = new Intent(this, CustomerActivity.class);
                break;
        }
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
