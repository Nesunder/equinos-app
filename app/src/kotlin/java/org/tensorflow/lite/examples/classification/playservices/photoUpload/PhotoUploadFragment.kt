package org.tensorflow.lite.examples.classification.playservices.photoUpload

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.horseCreation.HorseItem
import org.tensorflow.lite.examples.classification.playservices.horseCreation.HorseCreatorActivity
import org.tensorflow.lite.examples.classification.playservices.horseCreation.HorseItemAdapter


class PhotoUploadFragment : DialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HorseItemAdapter
    private lateinit var selectedItemTextView: TextView
    private lateinit var toggleButton: Button
    private lateinit var selectedItemLayout: LinearLayout
    private lateinit var selectedItemImageView: ImageView
    private lateinit var dropdownScroll: ScrollView
    private var isDropdownVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_upload, container, false)
        setupView(view)
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupView(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
        selectedItemLayout = view.findViewById(R.id.selected_item_layout)
        selectedItemImageView = view.findViewById(R.id.selected_item_image)
        selectedItemTextView = view.findViewById(R.id.selected_item_text)
        toggleButton = view.findViewById(R.id.toggle_button)
        dropdownScroll = view.findViewById(R.id.dropdown_scroll)

        val items = listOf(
            HorseItem("Caballo 1", R.drawable.caballo_inicio),
            HorseItem("Sereno 1", R.drawable.sereno),
            HorseItem("Interesado 1", R.drawable.interesado),
            HorseItem("Disgustado 1", R.drawable.disgustado),
            HorseItem("Caballo 2", R.drawable.caballo_inicio),
            HorseItem("Caballo 3", R.drawable.caballo_inicio),
            HorseItem("Caballo 4", R.drawable.caballo_inicio),
        )

        adapter = HorseItemAdapter(items) { selectedItem: HorseItem ->
            selectedItemImageView.setImageResource(selectedItem.imageResId)
            selectedItemTextView.text = selectedItem.text
            selectedItemLayout.visibility = LinearLayout.VISIBLE
            toggleDropdown()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        toggleButton.setOnClickListener {
            toggleDropdown()
        }

        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val addHorseBtn: Button = view.findViewById(R.id.addHorseBtn)

        cancelButton.setOnClickListener {
            dismiss()
        }

        val horseCreatorActivity = Intent(context, HorseCreatorActivity::class.java)

        addHorseBtn.setOnClickListener {
            startActivity(horseCreatorActivity)
        }
    }

    private fun toggleDropdown() {
        if (isDropdownVisible) {
            recyclerView.visibility = RecyclerView.GONE
            dropdownScroll.visibility = ScrollView.GONE

        } else {
            recyclerView.visibility = RecyclerView.VISIBLE
            dropdownScroll.visibility = ScrollView.VISIBLE
        }
        isDropdownVisible = !isDropdownVisible
    }
}