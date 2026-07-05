package com.devoncats.meditrack.presentation.caregiver

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.logout

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.logoutButton).setOnClickListener { logout() }
    }
}
