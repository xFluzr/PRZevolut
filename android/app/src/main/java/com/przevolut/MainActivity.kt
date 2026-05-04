package com.przevolut

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.przevolut.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Główna aktywność aplikacji.
 * Hostuję NavHostFragment z Navigation Component.
 * BottomNavigationView obsługuje nawigację między głównymi ekranami.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Podpięcie BottomNavigationView z NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Ukryj BottomNav na ekranie logowania
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> binding.bottomNavigation.visibility =
                    android.view.View.GONE
                else -> binding.bottomNavigation.visibility =
                    android.view.View.VISIBLE
            }
        }
    }
}
