package com.example.konserve

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.konserve.databinding.FragmentWasteAnalysisBinding
import kotlinx.coroutines.launch

class WasteAnalysisFragment : Fragment() {

    private var _binding: FragmentWasteAnalysisBinding? = null
    private val binding get() = _binding!!

    private var imageUriString: String? = null
    private lateinit var visionService: VisionService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUriString = it.getString(ARG_IMAGE_URI)
        }
        visionService = VisionService(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWasteAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the captured image
        imageUriString?.let { uriString ->
            val imageUri = Uri.parse(uriString)
            binding.capturedImageView.setImageURI(imageUri)

            // Start analysis
            analyzeWasteImage(imageUri)
        }

        // Set up back button
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Set up scan again button
        binding.scanAgainButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun analyzeWasteImage(imageUri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.resultCardView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = visionService.analyzeImage(imageUri)

            // Update UI with results
            binding.wasteTypeTextView.text = result.wasteType.replaceFirstChar { it.uppercase() }
            binding.confidenceTextView.text = "Confidence: ${String.format("%.1f", result.confidence)}%"
            binding.disposalInfoTextView.text = result.disposalInfo

            // Set appropriate category icon
            val iconResId = when (result.wasteType) {
                "glass" -> R.drawable.glass
                "metal" -> R.drawable.metal
                    "plastic" -> R.drawable.plastic
                "paper/cardboard" -> R.drawable.paper
                "textiles" -> R.drawable.textile
                "food waste" -> R.drawable.foodwaste
                else -> R.drawable.unknown
            }
            binding.categoryIconImageView.setImageResource(iconResId)

            // Set category color
            val colorResId = when (result.wasteType) {
                "glass" -> R.color.glass_color
                "metal" -> R.color.metal_color
                "plastic" -> R.color.plastic_color
                "paper/cardboard" -> R.color.paper_color
                "textiles" -> R.color.textile_color
                "food waste" -> R.color.food_waste_color
                else -> R.color.unknown_color
            }
            binding.categoryColorView.setBackgroundResource(colorResId)

            // Show detected objects
            binding.detectedObjectsTextView.text = "Detected: ${result.detectedObjects.take(5).joinToString(", ")}"

            // Hide progress and show results
            binding.progressBar.visibility = View.GONE
            binding.resultCardView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URI = "image_uri"

        fun newInstance(imageUriString: String) =
            WasteAnalysisFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URI, imageUriString)
                }
            }
    }
}