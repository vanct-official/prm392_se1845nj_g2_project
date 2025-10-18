package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.entity.User;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.Arrays;

/**
 * Màn hình đăng nhập — hỗ trợ cả Google và Email/Password.
 * Code được tách rõ, dễ mở rộng.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private GoogleSignInClient googleClient;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etEmail, etPassword;
    private MaterialButton btnLoginEmail, btnLoginGoogle;

    // Launcher cho Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            Log.d(TAG, "Google account: " + account.getEmail());
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign-in failed", e);
                        Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initFirebase();
        initUI();
        initGoogleSignIn();
        setListeners();
    }

    /** 🔹 Khởi tạo Firebase */
    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /** 🔹 Ánh xạ các thành phần giao diện */
    private void initUI() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLoginEmail = findViewById(R.id.btnLogin);
        btnLoginGoogle = findViewById(R.id.btnGoogleSignIn);
    }

    /** 🔹 Cấu hình Google Sign-In */
    private void initGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);
    }

    /** 🔹 Gắn sự kiện click */
    private void setListeners() {
        btnLoginGoogle.setOnClickListener(v -> signInWithGoogle());
        btnLoginEmail.setOnClickListener(v -> signInWithEmail());
    }

    /** 🟢 Đăng nhập bằng Google */
    private void signInWithGoogle() {
        Intent signInIntent = googleClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    /** 🟢 Xác thực Google ID Token với Firebase */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d(TAG, "Google login success: " + firebaseUser.getEmail());
                            checkUserInFirestore(firebaseUser);
                        }
                    } else {
                        Log.e(TAG, "Firebase Google login error", task.getException());
                        Toast.makeText(this, "Firebase Authentication thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** 🟢 Đăng nhập bằng Email/Password */
    private void signInWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Nếu bạn lưu password dạng hash trong Firestore, thì hash password trước khi so sánh
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) checkUserInFirestore(firebaseUser);
                    } else {
                        Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** 🧠 Kiểm tra user có tồn tại trong Firestore chưa */
    private void checkUserInFirestore(FirebaseUser firebaseUser) {
        String email = firebaseUser.getEmail();
        if (email == null) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        // 🔹 Lấy user đầu tiên
                        User user = snap.getDocuments().get(0).toObject(User.class);

                        if (user != null) {
                            Log.d(TAG, "User tồn tại: " + user.getEmail());
                            redirectByRole(user); // ✅ truyền cả object user
                        } else {
                            Toast.makeText(this, "Không xác định được thông tin người dùng", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Log.w(TAG, "User chưa tồn tại trong Firestore, tạo mới...");
                        createNewUser(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error: " + e.getMessage());
                    Toast.makeText(this, "Lỗi lấy dữ liệu user", Toast.LENGTH_SHORT).show();
                });
    }


    /** 🆕 Nếu user chưa có trong Firestore thì tạo mới */
    private void createNewUser(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        String displayName = firebaseUser.getDisplayName();
        String firstName = "New";
        String lastName = "User";

        if (displayName != null && !displayName.trim().isEmpty()) {
            String[] parts = displayName.trim().split("\\s+");
            if (parts.length == 1) {
                firstName = parts[0];
                lastName = "";
            } else {
                firstName = parts[parts.length - 1];
                lastName = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
            }
        }

        User newUser = new User();
        newUser.setEmail(firebaseUser.getEmail());
        newUser.setFirstname(firstName);
        newUser.setLastname(lastName);
        newUser.setRole("customer"); // Mặc định là khách hàng
        newUser.setIsActive(true);
        newUser.setPhone("");
        newUser.setGender(true);
        newUser.setDob(null);
        newUser.setUsername(firebaseUser.getEmail().split("@")[0]);
        newUser.setCreatedAt(new Timestamp(new java.util.Date()));
        newUser.setUpdatedAt(new Timestamp(new java.util.Date()));

        db.collection("users")
                .add(newUser)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Tạo mới user thành công: " + firebaseUser.getEmail());
                    redirectByRole(newUser); // ✅ đổi thành truyền user object
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tạo mới user: " + e.getMessage());
                    Toast.makeText(this, "Lỗi tạo mới tài khoản", Toast.LENGTH_SHORT).show();
                });
    }


    /** 🚀 Chuyển sang màn hình tương ứng theo vai trò */
    private void redirectByRole(User user) {
        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Boolean active = user.getIsActive();
        if (active == null || !active) {
            Toast.makeText(this, "Tài khoản của bạn đã bị khóa", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            return;
        }

        String role = user.getRole();
        if (role == null) role = "customer";

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


}
