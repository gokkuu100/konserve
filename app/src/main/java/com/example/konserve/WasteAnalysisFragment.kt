package com.example.konserve

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.konserve.databinding.FragmentWasteAnalysisBinding

class WasteAnalysisFragment : Fragment() {

    private var _binding: FragmentWasteAnalysisBinding? = null
    private val binding get() = _binding!!

    private var imageUriString: String? = null
    private var wasteType: String? = null
    private var confidence: Float = 0.0f
    private var labels: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUriString = it.getString(ARG_IMAGE_URI)
            wasteType = it.getString(ARG_WASTE_TYPE)
            confidence = it.getFloat(ARG_CONFIDENCE)
            labels = it.getString(ARG_LABELS)
        }
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

        // Load the captured image
        imageUriString?.let { uriString ->
            val uri = Uri.parse(uriString)
            Glide.with(this)
                .load(uri)
                .into(binding.capturedImageView)
        }

        // Display waste classification results
        wasteType?.let { type ->
            val formattedType = type.replace("_", " ").lowercase()
                .split(" ")
                .joinToString(" ") { it.capitalize() }
                
            binding.wasteTypeText.text = "Waste Type: $formattedType"
            binding.confidenceText.text = "Confidence: ${(confidence * 100).toInt()}%"
            
            // Set appropriate icon and color based on waste type
            val iconResId = when (type) {
                "PLASTIC" -> R.drawable.plastic
                "PAPER_CARDBOARD" -> R.drawable.paper
                "GLASS" -> R.drawable.glass
                "METAL" -> R.drawable.metal
                "TEXTILE" -> R.drawable.textile
                "ORGANIC" -> R.drawable.foodwaste
                else -> R.drawable.unknown
            }
            
            binding.wasteTypeIcon.setImageResource(iconResId)
            
            // Set recycling instructions based on waste type
            binding.recyclingInstructionsText.text = getRecyclingInstructions(type)
        }
        
        // Set up the back button
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun getRecyclingInstructions(wasteType: String): String {
        return when (wasteType) {
            "PLASTIC" -> "Rinse containers and remove caps. Check local recycling guidelines for accepted plastic types."
            "PAPER_CARDBOARD" -> "Flatten cardboard boxes. Keep paper dry and clean. Remove any plastic or metal attachments."
            "GLASS" -> "Rinse containers. Remove caps and lids. Sort by color if required by your local recycling program."
            "METAL" -> "Rinse cans and containers. Crush if possible to save space."
            "TEXTILE" -> "Clean textiles can be donated. Worn-out items may be accepted at textile recycling points."
            "ORGANIC" -> "Compost in your garden or use municipal organic waste collection if available."
            else -> "Unable to determine specific recycling instructions. Check with your local waste management."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URI = "image_uri"
        private const val ARG_WASTE_TYPE = "waste_type"
        private const val ARG_CONFIDENCE = "confidence"
        private const val ARG_LABELS = "labels"
        
        fun newInstance(
            imageUri: String,
            wasteType: String,
            confidence: Float,
            labels: String
        ) = WasteAnalysisFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_IMAGE_URI, imageUri)
                putString(ARG_WASTE_TYPE, wasteType)
                putFloat(ARG_CONFIDENCE, confidence)
                putString(ARG_LABELS, labels)
            }
        }
    }
}