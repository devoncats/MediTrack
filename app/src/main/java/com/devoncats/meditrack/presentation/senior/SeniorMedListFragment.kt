package com.devoncats.meditrack.presentation.senior

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.logout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SeniorMedListFragment : Fragment(R.layout.fragment_senior_med_list) {

    private val viewModel: SeniorMedListViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refreshTodayRange()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.logoutButton).setOnClickListener { logout() }

        val recyclerView = view.findViewById<RecyclerView>(R.id.seniorMedicationRecyclerView)
        val emptyStateTextView = view.findViewById<View>(R.id.emptyStateTextView)

        val adapter = SeniorMedicationListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.medicationItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            val isEmpty = items.isEmpty()
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }
}
