package com.devoncats.meditrack.presentation.caregiver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.model.EmergencyContact
import com.devoncats.meditrack.presentation.patient.MedListViewModel
import com.devoncats.meditrack.presentation.patient.MedicationListAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class SeniorDetailFragment : Fragment(R.layout.fragment_senior_detail) {

    private val seniorUserId: Long
        get() = requireArguments().getLong("seniorUserId")

    private val seniorName: String
        get() = requireArguments().getString("seniorName").orEmpty()

    private val viewModel: MedListViewModel by viewModels {
        SeniorDetailViewModelFactory(requireContext(), seniorUserId)
    }

    private val emergencyContactViewModel: EmergencyContactViewModel by viewModels {
        EmergencyContactViewModelFactory(requireContext(), seniorUserId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.seniorDetailTitle).text = seniorName

        val emergencyContactInfo = view.findViewById<TextView>(R.id.emergencyContactInfo)
        val editEmergencyContactButton = view.findViewById<MaterialButton>(R.id.editEmergencyContactButton)

        var currentContact: EmergencyContact? = null
        emergencyContactViewModel.contact.observe(viewLifecycleOwner) { contact ->
            currentContact = contact
            emergencyContactInfo.text = if (contact != null) {
                getString(R.string.senior_detail_contact_format, contact.name, contact.phone)
            } else {
                getString(R.string.senior_detail_contact_missing)
            }
        }
        editEmergencyContactButton.setOnClickListener { showEditContactDialog(currentContact) }

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

    private fun showEditContactDialog(currentContact: EmergencyContact?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_emergency_contact, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.emergencyContactNameEditText)
        val phoneEditText = dialogView.findViewById<TextInputEditText>(R.id.emergencyContactPhoneEditText)
        nameEditText.setText(currentContact?.name)
        phoneEditText.setText(currentContact?.phone)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.emergency_contact_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.emergency_contact_save_button) { _, _ ->
                val phone = phoneEditText.text.toString().trim()
                if (phone.isBlank()) return@setPositiveButton
                val name = nameEditText.text.toString().trim()
                    .ifBlank { getString(R.string.senior_detail_contact_label) }
                emergencyContactViewModel.saveContact(name, phone)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
