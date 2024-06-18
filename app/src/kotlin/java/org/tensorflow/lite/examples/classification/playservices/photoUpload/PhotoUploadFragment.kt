package org.tensorflow.lite.examples.classification.playservices.photoUpload

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.appRepository.MainViewModel
import org.tensorflow.lite.examples.classification.playservices.databinding.FragmentPhotoUploadBinding
import org.tensorflow.lite.examples.classification.playservices.horseCreation.HorseItem
import org.tensorflow.lite.examples.classification.playservices.horseCreation.HorseCreatorActivity
import org.tensorflow.lite.examples.classification.playservices.horseCreation.HorseItemAdapter


class PhotoUploadFragment : DialogFragment() {

    private var _binding: FragmentPhotoUploadBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: HorseItemAdapter
    private var isDropdownVisible = false
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoUploadBinding.inflate(inflater, container, false)
        setupView(binding.root)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupView(view: View) {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        adapter = HorseItemAdapter(viewModel.data.value!!) { selectedItem: HorseItem ->
            binding.selectedItemImage.setImageBitmap(selectedItem.imageBitmap)
            binding.selectedItemText.text = selectedItem.text
            binding.selectedItemLayout.visibility = LinearLayout.VISIBLE
            toggleDropdown()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        // Observando los datos
        viewModel.data.observe(this) { newData ->
            adapter.updateData(newData)
        }

        // Observing specific events
        viewModel.newItemEvent.observe(this) { newItem ->
            adapter.addItem(newItem)
        }

        binding.toggleButton.setOnClickListener {
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
            binding.recyclerView.visibility = RecyclerView.GONE
            binding.dropdownScroll.visibility = ScrollView.GONE

        } else {
            binding.recyclerView.visibility = RecyclerView.VISIBLE
            binding.dropdownScroll.visibility = ScrollView.VISIBLE
        }
        isDropdownVisible = !isDropdownVisible
    }
}