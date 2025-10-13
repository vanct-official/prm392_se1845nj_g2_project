package com.example.finalproject;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.finalproject.fragment.customer.CustomerBookingsFragment;
import com.example.finalproject.fragment.customer.CustomerHomeFragment;
import com.example.finalproject.fragment.ProfileFragment;
import com.example.finalproject.fragment.customer.CustomerChatFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_customer);

        // Mặc định hiển thị HomeFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_customer, new CustomerHomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_customer_home) {
                selectedFragment = new CustomerHomeFragment();
            } else if (itemId == R.id.nav_customer_bookings) {
                selectedFragment = new CustomerBookingsFragment();
            } else if (itemId == R.id.nav_customer_chat) {
                selectedFragment = new CustomerChatFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_customer, selectedFragment)
                        .commit();
            }

            return true;
        });

    }
}