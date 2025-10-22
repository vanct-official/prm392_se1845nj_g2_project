// java
// File: `app/src/main/java/com/example/finalproject/MainActivity.java`
package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Không setContentView vì MainActivity chỉ check login và redirect
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkLogin();
    }

    private void checkLogin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User đã login → lấy thông tin role từ Firestore
            fetchUserRole(currentUser.getUid());
        } else {
            // Chưa login → mở LoginActivity
            goToLogin();
        }
    }

    private void fetchUserRole(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null && user.getRole() != null) {
                    Boolean isActive = user.getIsActive(); // tránh null
                    if (isActive != null && isActive) {
                        redirectByRole(user.getRole());
                    } else {
                        Toast.makeText(this, "Account is locked or inactive", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        goToLogin();
                    }
                } else {
                    goToLogin();
                }

            } else {
                goToLogin();
            }
        }).addOnFailureListener(e -> {
            Log.e("MainActivity", "Error fetching user role", e);
            goToLogin();
        });
    }

    private void redirectByRole(String role) {
        Intent intent;
        switch (role) {
            case "admin":
                intent = new Intent(this, AdminActivity.class);
                break;
            case "guide":
                intent = new Intent(this, GuideActivity.class);
                break;
            default:
                intent = new Intent(this, CustomerActivity.class);
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
