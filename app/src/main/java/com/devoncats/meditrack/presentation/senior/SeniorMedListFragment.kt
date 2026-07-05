package com.devoncats.meditrack.presentation.senior

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.logout

class SeniorMedListFragment : Fragment(R.layout.fragment_senior_med_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.logoutButton).setOnClickListener { logout() }
    }
}
