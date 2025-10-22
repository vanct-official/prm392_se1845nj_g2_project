package com.example.finalproject;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.finalproject.fragment.ProfileFragment;
import com.example.finalproject.fragment.guide.GuideHomeFragment;
import com.example.finalproject.fragment.guide.GuideToursFragment;
import com.example.finalproject.fragment.ChatListFragment;
import com.example.finalproject.fragment.guide.CustomersInTourFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class GuideActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        bottomNav = findViewById(R.id.bottom_nav_guide);

        if (savedInstanceState == null) {
            currentFragment = new GuideHomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_guide, currentFragment)
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_guide_home) {
                selectedFragment = new GuideHomeFragment();
            } else if (itemId == R.id.nav_guide_tours) {
                selectedFragment = new GuideToursFragment();
            } else if (itemId == R.id.nav_guide_customers) {
                selectedFragment = new CustomersInTourFragment();
            } else if (itemId == R.id.nav_guide_chat) {
                selectedFragment = new ChatListFragment();
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
