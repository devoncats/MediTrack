package com.devoncats.meditrack

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.domain.model.UserRole

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // NavHostFragment retains its back stack across config changes, so only redirect on a
        // genuinely fresh start — otherwise rotating inside a deep screen would reset it back
        // to the role graph's start destination.
        if (savedInstanceState == null) {
            redirectIfSessionActive()
        }
    }

    private fun redirectIfSessionActive() {
        val sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) return

        val destinationId = try {
            when (UserRole.valueOf(sessionManager.getRole())) {
                UserRole.PATIENT -> R.id.patient_graph
                UserRole.CAREGIVER -> R.id.caregiver_graph
                UserRole.SENIOR_PATIENT -> R.id.senior_graph
            }
        } catch (e: IllegalArgumentException) {
            sessionManager.clearSession()
            return
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(
            destinationId,
            null,
            NavOptions.Builder().setPopUpTo(R.id.auth_graph, true).build()
        )
    }
}
