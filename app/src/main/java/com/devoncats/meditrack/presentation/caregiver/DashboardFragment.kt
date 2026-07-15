package com.devoncats.meditrack.presentation.caregiver

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.NavArgKeys
import com.devoncats.meditrack.presentation.logout
import com.devoncats.meditrack.presentation.patient.MedListViewModel
import com.devoncats.meditrack.presentation.patient.MedListViewModelFactory
import com.devoncats.meditrack.presentation.patient.MedicationListAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val medListViewModel: MedListViewModel by viewModels {
        MedListViewModelFactory(requireContext())
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireContext())
    }

    override fun onResume() {
        super.onResume()
        medListViewModel.refreshTodayRange()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val medicationRecyclerView = view.findViewById<RecyclerView>(R.id.medicationRecyclerView)
        val emptyStateTextView = view.findViewById<View>(R.id.emptyStateTextView)
        val addMedicationFab = view.findViewById<FloatingActionButton>(R.id.addMedicationFab)
        val missedDoseRecyclerView = view.findViewById<RecyclerView>(R.id.missedDoseRecyclerView)
        val missedDosesEmptyTextView = view.findViewById<View>(R.id.missedDosesEmptyTextView)

        val medicationAdapter = MedicationListAdapter(
            onItemClick = { item ->
                findNavController().navigate(
                    R.id.action_dashboard_to_medDetail,
                    bundleOf(NavArgKeys.MEDICATION_ID to item.medication.id)
                )
            },
            onPendingStatusClick = { item ->
                medListViewModel.findScheduleIdForAlert(item.medication.id) { scheduleId ->
                    if (scheduleId != null) {
                        findNavController().navigate(
                            R.id.action_dashboard_to_alert,
                            bundleOf(NavArgKeys.SCHEDULE_ID to scheduleId)
                        )
                    }
                }
            }
        )
        medicationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        medicationRecyclerView.adapter = medicationAdapter

        val missedDoseAdapter = MissedDoseAlertAdapter(
            onItemClick = { alert ->
                findNavController().navigate(
                    R.id.action_dashboard_to_missedDoseAlert,
                    bundleOf(NavArgKeys.LOG_ID to alert.logId)
                )
            }
        )
        missedDoseRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        missedDoseRecyclerView.adapter = missedDoseAdapter

        addMedicationFab.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_medForm)
        }

        view.findViewById<MaterialButton>(R.id.viewSeniorsButton).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_seniorList)
        }

        view.findViewById<View>(R.id.addSeniorPatientButton).setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_createSeniorPatient)
        }

        view.findViewById<View>(R.id.logoutButton).setOnClickListener { logout() }

        medListViewModel.medicationItems.observe(viewLifecycleOwner) { items ->
            medicationAdapter.submitList(items)
            val isEmpty = items.isEmpty()
            medicationRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptyStateTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        dashboardViewModel.missedDoseAlerts.observe(viewLifecycleOwner) { alerts ->
            missedDoseAdapter.submitList(alerts)
            val isEmpty = alerts.isEmpty()
            missedDoseRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            missedDosesEmptyTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }
}
