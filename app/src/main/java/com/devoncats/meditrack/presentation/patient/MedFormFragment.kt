package com.devoncats.meditrack.presentation.patient

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.devoncats.meditrack.presentation.camera.CameraFragment
import com.devoncats.meditrack.services.FileStorageHelper
import com.devoncats.meditrack.utils.toCode
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.LocalTime

@AndroidEntryPoint
class MedFormFragment : Fragment(R.layout.fragment_med_form) {

    private val viewModel: MedFormViewModel by viewModels()

    private val selectedTimes = sortedSetOf<LocalTime>()

    private var capturedPhotoUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleTextView = view.findViewById<TextView>(R.id.medFormTitle)
        val nameEditText = view.findViewById<TextInputEditText>(R.id.nameEditText)
        val doseEditText = view.findViewById<TextInputEditText>(R.id.doseEditText)
        val frequencyEditText = view.findViewById<TextInputEditText>(R.id.frequencyEditText)
        val instructionsEditText = view.findViewById<TextInputEditText>(R.id.instructionsEditText)
        val photoPreviewImageView = view.findViewById<ImageView>(R.id.photoPreviewImageView)
        val takePhotoButton = view.findViewById<MaterialButton>(R.id.takePhotoButton)
        val daysOfWeekChipGroup = view.findViewById<ChipGroup>(R.id.daysOfWeekChipGroup)
        val timesChipGroup = view.findViewById<ChipGroup>(R.id.timesChipGroup)
        val addTimeButton = view.findViewById<MaterialButton>(R.id.addTimeButton)
        val errorTextView = view.findViewById<View>(R.id.errorTextView)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)

        titleTextView.setText(if (viewModel.isEditMode) R.string.med_form_edit_title else R.string.med_form_title)

        setFragmentResultListener(CameraFragment.RESULT_KEY) { _, bundle ->
            val uriString = bundle.getString(CameraFragment.RESULT_PHOTO_URI) ?: return@setFragmentResultListener
            capturedPhotoUri = Uri.parse(uriString)
            photoPreviewImageView.setImageURI(capturedPhotoUri)
            photoPreviewImageView.visibility = View.VISIBLE
            takePhotoButton.setText(R.string.med_form_retake_photo_button)
        }

        takePhotoButton.setOnClickListener {
            findNavController().navigate(R.id.action_medForm_to_camera)
        }

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

        nameEditText.doAfterTextChanged { updateSaveButtonState() }
        doseEditText.doAfterTextChanged { updateSaveButtonState() }
        frequencyEditText.doAfterTextChanged { updateSaveButtonState() }
        daysOfWeekChipGroup.setOnCheckedStateChangeListener { _, _ -> updateSaveButtonState() }

        @SuppressLint("SetTextI18n")
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
            viewModel.save(
                name = nameEditText.text.toString().trim(),
                dose = doseEditText.text.toString().trim(),
                frequency = frequencyEditText.text.toString().trim(),
                instructions = instructionsEditText.text.toString().trim(),
                selectedDays = selectedDays(),
                selectedTimes = selectedTimes.toList(),
                capturedPhotoUri = capturedPhotoUri
            )
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                MedFormSaveResult.Success -> findNavController().popBackStack()
                MedFormSaveResult.ValidationError -> errorTextView.visibility = View.VISIBLE
            }
        }

        viewModel.prefill.observe(viewLifecycleOwner) { prefill ->
            nameEditText.setText(prefill.name)
            doseEditText.setText(prefill.dose)
            frequencyEditText.setText(prefill.frequency)
            instructionsEditText.setText(prefill.instructions.orEmpty())

            val dayCodes = prefill.days.map { it.toCode() }.toSet()
            for (i in 0 until daysOfWeekChipGroup.childCount) {
                val chip = daysOfWeekChipGroup.getChildAt(i) as? Chip ?: continue
                chip.isChecked = chip.tag as? String in dayCodes
            }

            selectedTimes.clear()
            selectedTimes.addAll(prefill.times)
            renderTimeChips()

            if (capturedPhotoUri == null && !prefill.photoUri.isNullOrBlank()) {
                val bitmap = FileStorageHelper(requireContext()).loadPhoto(prefill.photoUri)
                if (bitmap != null) {
                    photoPreviewImageView.setImageBitmap(bitmap)
                    photoPreviewImageView.visibility = View.VISIBLE
                    takePhotoButton.setText(R.string.med_form_retake_photo_button)
                }
            }

            updateSaveButtonState()
        }
    }
}
