package org.tensorflow.lite.examples.classification.playservices.profile

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.tensorflow.lite.examples.classification.playservices.databinding.FragmentProfileBinding


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        setOnTouchListeners()
        binding.cvNotifications.setOnClickListener {
            // Manejar el clic aquí
        }
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListeners() {
        val touchListener = OnTouchListener { v, event ->
            animateButton(v, event)
            true
        }

        binding.cvNotifications.setOnTouchListener(touchListener)
        binding.cvGalery.setOnTouchListener(touchListener)
        binding.cvShare.setOnTouchListener(touchListener)
        binding.cvLogOut.setOnTouchListener(touchListener)
        binding.cvConfiguracion.setOnTouchListener(touchListener)
        binding.cvNotas.setOnTouchListener(touchListener)
    }

    private fun animateButton(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Iniciar la animación para oscurecer
                ObjectAnimator.ofFloat(v, "alpha", 0.5f).setDuration(100).start()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Iniciar la animación para volver a la normalidad
                ObjectAnimator.ofFloat(v, "alpha", 1.0f).setDuration(100).start()
                if (event.action == MotionEvent.ACTION_UP) {
                    // Llamar a performClick para manejar accesibilidad
                    v.performClick()
                }
                return true
            }

            else -> return false
        }
    }


}