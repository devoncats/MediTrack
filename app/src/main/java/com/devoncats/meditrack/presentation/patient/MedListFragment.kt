package com.devoncats.meditrack.presentation.patient

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.logout

class MedListFragment : Fragment(R.layout.fragment_med_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.logoutButton).setOnClickListener { logout() }
    }
}
