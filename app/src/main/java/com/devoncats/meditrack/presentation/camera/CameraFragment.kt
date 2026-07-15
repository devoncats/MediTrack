package com.devoncats.meditrack.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.devoncats.meditrack.services.CameraHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class CameraFragment : Fragment(R.layout.fragment_camera) {

    private val cameraHelper by lazy { CameraHelper(requireContext()) }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCameraPreview() else showPermissionDenied()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val permissionDeniedLayout = view.findViewById<View>(R.id.cameraPermissionDeniedLayout)
        val grantButton = view.findViewById<MaterialButton>(R.id.cameraPermissionGrantButton)
        grantButton.setOnClickListener { requestCameraPermission.launch(Manifest.permission.CAMERA) }

        val captureButton = view.findViewById<FloatingActionButton>(R.id.captureButton)
        captureButton.setOnClickListener { capturePhoto() }

        if (CameraHelper.hasCameraPermission(requireContext())) {
            startCameraPreview()
        } else {
            permissionDeniedLayout.visibility = View.VISIBLE
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraPreview() {
        view?.findViewById<View>(R.id.cameraPermissionDeniedLayout)?.visibility = View.GONE
        val previewView = view?.findViewById<PreviewView>(R.id.cameraPreviewView) ?: return
        cameraHelper.startCamera(viewLifecycleOwner, previewView) {
            showPermissionDenied()
        }
    }

    private fun showPermissionDenied() {
        view?.findViewById<View>(R.id.cameraPermissionDeniedLayout)?.visibility = View.VISIBLE
    }

    private fun capturePhoto() {
        cameraHelper.takePhoto(
            onSuccess = { uri ->
                setFragmentResult(RESULT_KEY, bundleOf(RESULT_PHOTO_URI to uri.toString()))
                findNavController().popBackStack()
            },
            onError = {
                view?.let { root -> Snackbar.make(root, R.string.camera_capture_error, Snackbar.LENGTH_LONG).show() }
            }
        )
    }

    companion object {
        const val RESULT_KEY = "camera_result"
        const val RESULT_PHOTO_URI = "photo_uri"
    }
}
