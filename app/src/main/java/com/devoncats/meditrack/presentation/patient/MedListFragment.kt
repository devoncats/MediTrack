package com.devoncats.meditrack.presentation.patient

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.logout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MedListFragment : Fragment(R.layout.fragment_med_list) {

    private val viewModel: MedListViewModel by viewModels {
        MedListViewModelFactory(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.medicationRecyclerView)
        val emptyStateTextView = view.findViewById<View>(R.id.emptyStateTextView)
        val addMedicationFab = view.findViewById<FloatingActionButton>(R.id.addMedicationFab)

        val adapter = MedicationListAdapter { item ->
            findNavController().navigate(
                R.id.action_medList_to_medDetail,
                bundleOf("medicationId" to item.medication.id)
            )
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        addMedicationFab.setOnClickListener {
            findNavController().navigate(R.id.action_medList_to_medForm)
        }

        view.findViewById<View>(R.id.logoutButton).setOnClickListener { logout() }

        viewModel.medicationItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            val isEmpty = items.isEmpty()
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }
}
