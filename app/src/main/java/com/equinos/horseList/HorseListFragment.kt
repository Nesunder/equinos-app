package com.equinos.horseList

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.equinos.appRepository.HorseRepository
import com.equinos.appRepository.HorseViewModel
import com.equinos.databinding.FragmentHorseListBinding
import com.equinos.horseCreation.HorseCreatorActivity
import com.equinos.horseCreation.HorseItem
import com.equinos.horseCreation.HorseItemAdapter
import com.equinos.settings.Network


class HorseListFragment : Fragment() {

    private lateinit var viewModel: HorseViewModel
    private var _binding: FragmentHorseListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: HorseItemAdapter
    private lateinit var horseCreatorLauncher: ActivityResultLauncher<Intent>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHorseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val noConnectionMode = Network.getNoConnectionMode()

        if (!noConnectionMode) {
            // Primero, obtener y actualizar los datos
            updateHorseListData { data ->
                // Después de obtener los datos, configurar el RecyclerView
                setupRecyclerView(data)
            }
        }

        // Configurar el ActivityResultLauncher para la actividad de creación
        horseCreatorLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updateHorseListData { updatedData ->
                adapter.updateData(emptyList())
                adapter.updateData(updatedData)
            }
        }

        binding.addHorseBtn.setOnClickListener {
            if (!noConnectionMode) {
                val horseCreatorActivity = Intent(context, HorseCreatorActivity::class.java)
                horseCreatorLauncher.launch(horseCreatorActivity)
            } else {
                showDialog(requireContext(), "Agregar caballo", "No se puede agregar un caballo porque se está usando el modo sin conexión ")
            }
        }
    }

    private fun showDialog(
        context: Context,
        title: String,
        message: String,
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun setupRecyclerView(initialData: List<HorseItem>) {
        viewModel = ViewModelProvider(this)[HorseViewModel::class.java]
        viewModel.updateData(emptyList())
        viewModel.updateData(initialData)
        adapter = HorseItemAdapter(viewModel.data.value!!) {
            // Lógica para abrir la interfaz del caballo
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        // Observa cambios en los datos para actualizar la UI
        viewModel.data.observe(viewLifecycleOwner) { newData ->
            adapter.updateData(newData)
        }
    }

    private fun updateHorseListData(onDataLoaded: (List<HorseItem>) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = context?.let { it1 ->
                HorseRepository.loadInitialData()
            }
            data?.let {
                onDataLoaded(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val emptyList = emptyList<HorseItem>()
        if(::viewModel.isInitialized && ::adapter.isInitialized)  {
            viewModel.updateData(emptyList)
            adapter.updateData(emptyList)
        }
        _binding = null
    }
}