package com.devoncats.meditrack.presentation.caregiver

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.patient.MedListViewModel
import com.devoncats.meditrack.presentation.patient.MedicationListAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SeniorDetailFragment : Fragment(R.layout.fragment_senior_detail) {

    private val seniorUserId: Long
        get() = requireArguments().getLong("seniorUserId")

    private val seniorName: String
        get() = requireArguments().getString("seniorName").orEmpty()

    private val viewModel: MedListViewModel by viewModels {
        SeniorDetailViewModelFactory(requireContext(), seniorUserId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.seniorDetailTitle).text = seniorName

        val recyclerView = view.findViewById<RecyclerView>(R.id.medicationRecyclerView)
        val emptyStateTextView = view.findViewById<View>(R.id.emptyStateTextView)
        val addMedicationFab = view.findViewById<FloatingActionButton>(R.id.addMedicationFab)

        val adapter = MedicationListAdapter(
            onItemClick = { item ->
                findNavController().navigate(
                    R.id.action_seniorDetail_to_seniorMedDetail,
                    bundleOf("medicationId" to item.medication.id, "seniorUserId" to seniorUserId)
                )
            },
            onPendingStatusClick = { }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        addMedicationFab.setOnClickListener {
            findNavController().navigate(
                R.id.action_seniorDetail_to_seniorMedForm,
                bundleOf("seniorUserId" to seniorUserId)
            )
        }

        viewModel.medicationItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            val isEmpty = items.isEmpty()
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }
}
