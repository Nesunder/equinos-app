package com.equinos.horseList

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.equinos.appRepository.DataRepository
import com.equinos.databinding.FragmentHorseListBinding
import com.equinos.horseCreation.HorseCreatorActivity
import com.equinos.horseCreation.HorseItemAdapter

class HorseListFragment : Fragment() {

    private var _binding: FragmentHorseListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: HorseItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHorseListBinding.inflate(inflater, container, false)
        setupRecyclerView()

        val horseCreatorActivity = Intent(context, HorseCreatorActivity::class.java)
        binding.addHorseBtn.setOnClickListener {
            startActivity(horseCreatorActivity)
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        lifecycleScope.launch {
            val initialData = context?.let { it1 -> DataRepository.loadInitialData(it1) }
            if (initialData != null) {
                DataRepository.updateData(initialData)
                adapter = HorseItemAdapter(initialData) {
                    //Abrir interfaz del caballo o algo
                }

                binding.recyclerView.layoutManager = LinearLayoutManager(context)
                binding.recyclerView.adapter = adapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}