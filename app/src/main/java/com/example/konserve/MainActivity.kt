package com.example.konserve

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)


        bottomNavigationView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.navigation_reward -> selectedFragment = RewardFragment()
                R.id.navigation_calendar -> selectedFragment = CalendarFragment()
                R.id.navigation_location -> selectedFragment = LocationFragment()
                R.id.navigation_profile -> selectedFragment = ProfileFragment()
                R.id.navigation_camera -> selectedFragment = AICameraFragment()
            }

            // Load the selected fragment
            selectedFragment?.let { loadFragment(it) }
            true
        }
    }
    // Helper function to load the selected fragment
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }
}