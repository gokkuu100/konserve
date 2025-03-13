package com.example.konserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class AboutMenuFragment : Fragment() {
    private lateinit var backButton: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about_menu, container, false)
        
        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        return view
    }
} 