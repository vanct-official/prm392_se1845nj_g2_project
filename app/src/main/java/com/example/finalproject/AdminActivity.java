package com.example.finalproject;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.finalproject.fragment.ChatListFragment;
import com.example.finalproject.fragment.ProfileFragment;
import com.example.finalproject.fragment.admin.AdminBookingsFragment;
import com.example.finalproject.fragment.admin.AdminDashboardFragment;
import com.example.finalproject.fragment.admin.AdminInvitationsFragment;
import com.example.finalproject.fragment.admin.AdminManageUsersFragment;
import com.example.finalproject.fragment.admin.AdminPaymentsFragment;
import com.example.finalproject.fragment.admin.AdminPromotionsFragment;
import com.example.finalproject.fragment.admin.AdminReportsFragment;
import com.example.finalproject.fragment.admin.AdminReviewsFragment;
import com.example.finalproject.fragment.admin.AdminToursFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Header view components
    private TextView tvAdminName, tvAdminEmail, tvStatus;
    private ImageView ivAdminAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Toolbar setup
        toolbar = findViewById(R.id.toolbar_admin);
        setSupportActionBar(toolbar);

        // Drawer + Navigation setup
        drawerLayout = findViewById(R.id.drawer_layout_admin);
        navigationView = findViewById(R.id.nav_view_admin);
        navigationView.setNavigationItemSelectedListener(this);

        // Drawer toggle (hamburger icon)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ✅ Firebase init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ✅ Load admin info to Navigation header
        setupAdminHeader();

        // ✅ FloatingActionButton → Quay về Dashboard
        findViewById(R.id.fab_admin).setOnClickListener(v -> {
            replaceFragment(new AdminDashboardFragment());
            navigationView.setCheckedItem(R.id.nav_admin_home);
        });

        // ✅ Sử dụng OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawers();
                } else {
                    setEnabled(false);
                    AdminActivity.super.onBackPressed();
                }
            }
        });

        // ✅ Fragment mặc định
        if (savedInstanceState == null) {
            replaceFragment(new AdminDashboardFragment());
            navigationView.setCheckedItem(R.id.nav_admin_home);
        }
    }

    /**
     * Load thông tin admin từ Firestore và hiển thị lên header
     */
    private void setupAdminHeader() {
        View headerView = navigationView.getHeaderView(0);

        tvAdminName = headerView.findViewById(R.id.tvAdminName);
        tvAdminEmail = headerView.findViewById(R.id.tvAdminEmail);
        ivAdminAvatar = headerView.findViewById(R.id.ivAdminAvatar);
        tvStatus = headerView.findViewById(R.id.tvStatus);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String firstname = document.getString("firstname");
                        String lastname = document.getString("lastname");
                        String email = document.getString("email");
                        String avatarUrl = document.getString("avatarUrl");
                        Boolean isActive = document.getBoolean("isActive");

                        tvAdminName.setText(firstname + " " + lastname);
                        tvAdminEmail.setText(email);

                        // Trạng thái online/offline
                        if (Boolean.TRUE.equals(isActive)) {
                            tvStatus.setText("Online");
                            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                        } else {
                            tvStatus.setText("Offline");
                            tvStatus.setTextColor(Color.parseColor("#9E9E9E"));
                        }

                        // Avatar
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.ic_circle)
                                    .circleCrop()
                                    .into(ivAdminAvatar);
                        } else {
                            ivAdminAvatar.setImageResource(R.drawable.ic_circle);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvAdminName.setText("Admin");
                    tvAdminEmail.setText("Không thể tải thông tin");
                    tvStatus.setText("Offline");
                    tvStatus.setTextColor(Color.GRAY);
                });

        // 📌 Khi bấm avatar → mở ProfileFragment
        ivAdminAvatar.setOnClickListener(v -> {
            replaceFragment(new ProfileFragment());
            drawerLayout.closeDrawers();
        });
    }

    /**
     * Xử lý chọn menu
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int id = item.getItemId();

        if (id == R.id.nav_admin_home) {
            selectedFragment = new AdminDashboardFragment();
        } else if (id == R.id.nav_admin_users) {
            selectedFragment = new AdminManageUsersFragment();
        } else if (id == R.id.nav_admin_tours) {
            selectedFragment = new AdminToursFragment();
        } else if (id == R.id.nav_admin_promotions) {
            selectedFragment = new AdminPromotionsFragment();
        } else if (id == R.id.nav_admin_chat) {
            selectedFragment = new ChatListFragment();
        } else if (id == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
        } else if (id == R.id.nav_admin_reviews) {
            selectedFragment = new AdminReviewsFragment();
        } else if (id == R.id.nav_admin_reports) {
            selectedFragment = new AdminReportsFragment();
        } else if (id == R.id.nav_admin_payments) {
            selectedFragment = new AdminPaymentsFragment();
        } else if (id == R.id.nav_admin_invitation) {
            selectedFragment = new AdminInvitationsFragment();
        } else if (id == R.id.nav_admin_bookings) {
            selectedFragment = new AdminBookingsFragment();
        }

        if (selectedFragment != null) {
            replaceFragment(selectedFragment);
        }

        drawerLayout.closeDrawers();
        return true;
    }

    /**
     * Hàm thay thế Fragment
     */
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container_admin, fragment);
        transaction.commit();
    }
}
