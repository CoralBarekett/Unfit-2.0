package com.app.unfit20

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.app.unfit2.R
import com.app.unfit20.databinding.ActivityMainBinding
import com.app.unfit20.ui.auth.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Set up Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up Bottom Navigation
        binding.bottomNavigationView.setupWithNavController(navController)

        // Set up FAB for adding new post
        binding.fabAdd.setOnClickListener {
            navController.navigate(R.id.createPostFragment)
        }

        // Check if user is logged in
        if (authViewModel.isLoggedIn()) {
            navController.navigate(R.id.homeFragment)
        }

        // Hide bottom nav and FAB on auth screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment, R.id.createPostFragment, R.id.editProfileFragment -> {
                    binding.bottomNavigationView.visibility = android.view.View.GONE
                    binding.fabAdd.hide()
                }
                else -> {
                    binding.bottomNavigationView.visibility = android.view.View.VISIBLE
                    binding.fabAdd.show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}