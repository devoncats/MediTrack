package com.devoncats.meditrack.presentation

import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.devoncats.meditrack.data.local.SessionManager

fun Fragment.logout() {
    SessionManager(requireContext()).clearSession()
    findNavController().navigate(
        R.id.auth_graph,
        null,
        NavOptions.Builder().setPopUpTo(R.id.auth_graph, true).build()
    )
}
