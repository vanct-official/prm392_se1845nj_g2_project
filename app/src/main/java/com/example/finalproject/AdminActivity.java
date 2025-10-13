package com.example.finalproject;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.finalproject.fragment.admin.AdminFeedbackFragment;
import com.example.finalproject.fragment.admin.AdminHomeFragment;
import com.example.finalproject.fragment.admin.AdminManageUsersFragment;
import com.example.finalproject.fragment.admin.AdminSettingFragment;
import com.example.finalproject.fragment.admin.AdminToursFragment;
import com.example.finalproject.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_admin);

        // Mặc định hiển thị HomeFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_admin, new AdminHomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_admin_home) {
                selectedFragment = new AdminHomeFragment();
            } else if (itemId == R.id.nav_admin_tours) {
                selectedFragment = new AdminToursFragment();
            } else if (itemId == R.id.nav_admin_users) {
                selectedFragment = new AdminManageUsersFragment();
            } else if (itemId == R.id.nav_feedback) {
                selectedFragment = new AdminFeedbackFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new AdminSettingFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_admin, selectedFragment)
                        .commit();
            }

            return true;
        });

    }
}