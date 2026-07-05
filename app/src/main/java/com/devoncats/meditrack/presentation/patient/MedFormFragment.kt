package com.devoncats.meditrack.presentation.patient

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.DayOfWeek
import java.time.LocalTime

class MedFormFragment : Fragment(R.layout.fragment_med_form) {

    private val viewModel: MedFormViewModel by viewModels {
        MedFormViewModelFactory(requireContext())
    }

    private val selectedTimes = sortedSetOf<LocalTime>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameEditText = view.findViewById<TextInputEditText>(R.id.nameEditText)
        val doseEditText = view.findViewById<TextInputEditText>(R.id.doseEditText)
        val frequencyEditText = view.findViewById<TextInputEditText>(R.id.frequencyEditText)
        val instructionsEditText = view.findViewById<TextInputEditText>(R.id.instructionsEditText)
        val daysOfWeekChipGroup = view.findViewById<ChipGroup>(R.id.daysOfWeekChipGroup)
        val timesChipGroup = view.findViewById<ChipGroup>(R.id.timesChipGroup)
        val addTimeButton = view.findViewById<MaterialButton>(R.id.addTimeButton)
        val errorTextView = view.findViewById<View>(R.id.errorTextView)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)

        fun selectedDays(): Set<DayOfWeek> = daysOfWeekChipGroup.checkedChipIds
            .mapNotNull { chipId -> daysOfWeekChipGroup.findViewById<Chip>(chipId)?.tag as? String }
            .mapNotNull { code ->
                when (code) {
                    "MON" -> DayOfWeek.MONDAY
                    "TUE" -> DayOfWeek.TUESDAY
                    "WED" -> DayOfWeek.WEDNESDAY
                    "THU" -> DayOfWeek.THURSDAY
                    "FRI" -> DayOfWeek.FRIDAY
                    "SAT" -> DayOfWeek.SATURDAY
                    "SUN" -> DayOfWeek.SUNDAY
                    else -> null
                }
            }
            .toSet()

        fun updateSaveButtonState() {
            saveButton.isEnabled = !nameEditText.text.isNullOrBlank() &&
                !doseEditText.text.isNullOrBlank() &&
                !frequencyEditText.text.isNullOrBlank() &&
                selectedDays().isNotEmpty() &&
                selectedTimes.isNotEmpty()
        }

        val fieldsWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateSaveButtonState()
        }
        nameEditText.addTextChangedListener(fieldsWatcher)
        doseEditText.addTextChangedListener(fieldsWatcher)
        frequencyEditText.addTextChangedListener(fieldsWatcher)
        daysOfWeekChipGroup.setOnCheckedStateChangeListener { _, _ -> updateSaveButtonState() }

        fun renderTimeChips() {
            timesChipGroup.removeAllViews()
            selectedTimes.forEach { time ->
                val chip = Chip(requireContext()).apply {
                    text = "%02d:%02d".format(time.hour, time.minute)
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        selectedTimes.remove(time)
                        renderTimeChips()
                        updateSaveButtonState()
                    }
                }
                timesChipGroup.addView(chip)
            }
        }

        addTimeButton.setOnClickListener {
            val now = LocalTime.now()
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(now.hour)
                .setMinute(now.minute)
                .build()
                .apply {
                    addOnPositiveButtonClickListener {
                        selectedTimes.add(LocalTime.of(hour, minute))
                        renderTimeChips()
                        updateSaveButtonState()
                    }
                }
                .show(childFragmentManager, "time_picker")
        }

        saveButton.setOnClickListener {
            errorTextView.visibility = View.GONE
            viewModel.saveNewMedication(
                name = nameEditText.text.toString().trim(),
                dose = doseEditText.text.toString().trim(),
                frequency = frequencyEditText.text.toString().trim(),
                instructions = instructionsEditText.text.toString().trim(),
                selectedDays = selectedDays(),
                selectedTimes = selectedTimes.toList()
            )
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                MedFormSaveResult.Success -> findNavController().popBackStack()
                MedFormSaveResult.ValidationError -> errorTextView.visibility = View.VISIBLE
            }
        }
    }
}
