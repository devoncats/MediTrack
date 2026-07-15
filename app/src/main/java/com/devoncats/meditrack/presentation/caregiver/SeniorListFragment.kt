package com.devoncats.meditrack.presentation.caregiver

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.NavArgKeys
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SeniorListFragment : Fragment(R.layout.fragment_senior_list) {

    private val viewModel: SeniorListViewModel by viewModels {
        SeniorListViewModelFactory(requireContext())
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTodayRange()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.seniorRecyclerView)
        val emptyStateTextView = view.findViewById<View>(R.id.emptyStateTextView)
        val addSeniorFab = view.findViewById<FloatingActionButton>(R.id.addSeniorFab)

        val adapter = SeniorListAdapter(
            onItemClick = { item ->
                findNavController().navigate(
                    R.id.action_seniorList_to_seniorDetail,
                    bundleOf(NavArgKeys.SENIOR_USER_ID to item.senior.id, NavArgKeys.SENIOR_NAME to item.senior.name)
                )
            },
            onDeleteClick = { item -> showDeleteConfirmation(item) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        addSeniorFab.setOnClickListener {
            findNavController().navigate(R.id.action_seniorList_to_createSeniorPatient)
        }

        viewModel.seniorItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            val isEmpty = items.isEmpty()
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    private fun showDeleteConfirmation(item: SeniorListItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.senior_delete_confirm_title)
            .setMessage(getString(R.string.senior_delete_confirm_message, item.senior.name))
            .setPositiveButton(R.string.senior_delete_confirm_positive) { _, _ ->
                viewModel.deleteSenior(item.senior)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
