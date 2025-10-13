package com.example.finalproject;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.finalproject.fragment.customer.CustomerHomeFragment;
import com.example.finalproject.fragment.ProfileFragment;
import com.example.finalproject.fragment.guide.GuideChatFragment;
import com.example.finalproject.fragment.guide.GuideHomeFragment;
import com.example.finalproject.fragment.guide.GuideToursFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_guide);

        // Mặc định hiển thị HomeFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_guide, new GuideHomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_guide_home) {
                selectedFragment = new GuideHomeFragment();
            } else if (itemId == R.id.nav_guide_tours) {
                selectedFragment = new GuideToursFragment();
            } else if (itemId == R.id.nav_guide_chat) {
                selectedFragment = new GuideChatFragment();
            } else if (itemId == R.id.nav_guide_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_guide, selectedFragment)
                        .commit();
            }

            return true;
        });

    }
}