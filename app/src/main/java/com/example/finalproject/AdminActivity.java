package com.example.finalproject;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.finalproject.fragment.admin.AdminDashboardFragment;
import com.example.finalproject.fragment.admin.AdminReportsFragment;
import com.example.finalproject.fragment.admin.AdminReviewsFragment;
import com.example.finalproject.fragment.admin.AdminToursFragment;
import com.example.finalproject.fragment.admin.AdminManageUsersFragment;
//import com.example.finalproject.fragment.admin.AdminBookingsFragment;
import com.example.finalproject.fragment.admin.AdminPromotionsFragment;
//import com.example.finalproject.fragment.admin.AdminChatFragment;
import com.example.finalproject.fragment.admin.AdminFeedbackFragment;
import com.example.finalproject.fragment.ProfileFragment;

import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

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

        // Nút FloatingActionButton quay về trang Home dashboard
        findViewById(R.id.fab_admin).setOnClickListener(v -> {
            replaceFragment(new AdminDashboardFragment());
            navigationView.setCheckedItem(R.id.nav_admin_home);
        });


        // ✅ Sử dụng OnBackPressedDispatcher thay cho onBackPressed()
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawers();
                } else {
                    // Hành vi back mặc định
                    setEnabled(false);
                    AdminActivity.super.onBackPressed();
                }
            }
        });

        // Fragment mặc định khi khởi động
        if (savedInstanceState == null) {
            replaceFragment(new AdminDashboardFragment());
            navigationView.setCheckedItem(R.id.nav_admin_home);
        }
    }

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
//
//        } else if (id == R.id.nav_admin_bookings) {
//            selectedFragment = new AdminBookingsFragment();
//
        } else if (id == R.id.nav_admin_promotions) {
            selectedFragment = new AdminPromotionsFragment();
//
//        } else if (id == R.id.nav_admin_chat) {
//            selectedFragment = new AdminChatFragment();

//        } else if (id == R.id.nav_admin_feedback) {
//            selectedFragment = new AdminFeedbackFragment();

        } else if (id == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
        }
        else if (id == R.id.nav_admin_reviews) {
            selectedFragment = new AdminReviewsFragment();
        }
        else if (id == R.id.nav_admin_reviews) {
            selectedFragment = new AdminReviewsFragment();
        }
        else if (id == R.id.nav_admin_reports) {
            selectedFragment = new AdminReportsFragment();
        }

        if (selectedFragment != null) {
            replaceFragment(selectedFragment);
        }

        drawerLayout.closeDrawers();
        return true;
    }
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container_admin, fragment);
        transaction.commit();
    }
}
