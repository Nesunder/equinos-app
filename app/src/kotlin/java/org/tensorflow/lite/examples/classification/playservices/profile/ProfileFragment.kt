package org.tensorflow.lite.examples.classification.playservices.profile

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.databinding.FragmentProfileBinding
import kotlin.math.abs


@Suppress("SameParameterValue")
class ProfileFragment : Fragment() {
    private inner class SwipeGestureDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            if (e1 != null) {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffY) > abs(diffX)) {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) onSwipeDown()
                        return true
                    }
                }
            }
            return false
        }
    }

    private var showingPhotoCardview: Boolean = false
    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var gestureDetector: GestureDetector

    private var initialY: Float = 0f
    private var touchStartY: Float = 0f

    companion object {
        private const val SWIPE_THRESHOLD = 200
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Create a handler associated with the main thread's Looper
        val handler = Handler(Looper.getMainLooper())
        // Initialize the GestureDetector with a handler
        gestureDetector = GestureDetector(requireContext(), SwipeGestureDetector(), handler)
        setOnTouchListeners()

        binding.profilePhoto.setOnClickListener {
            hideOrShowPhotoCardView()
        }

        binding.overlayView.setOnClickListener {
            hideOrShowPhotoCardView()
        }
        return binding.root
    }

    private fun hideOrShowPhotoCardView() {
        initialY = binding.photoCardView.y

        if (showingPhotoCardview) {
            binding.photoCardView.animate().y(initialY + binding.photoCardView.height.toFloat())
                .setDuration(300).start()
            setTimeout(200) {
                binding.overlayView.visibility = View.GONE
                binding.overlayView.isClickable = false
                binding.overlayView.isFocusable = false
            }
            showingPhotoCardview = false
            return
        }

        binding.photoCardView.animate().y(initialY - binding.photoCardView.height.toFloat())
            .setDuration(300).start()
        binding.overlayView.visibility = View.VISIBLE
        binding.overlayView.isClickable = true
        binding.overlayView.isFocusable = true
        showingPhotoCardview = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSwipeDown() {
        // Handle the swipe down action (e.g., hide a view)

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListeners() {
        binding.photoCardView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            handleTouchEvent(v, event)
            v.performClick()
            true
        }

        val touchListener = OnTouchListener { v, event ->
            animateButton(v, event)
            true
        }

        val onTouchListenerAnimation = OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.card_background
                        )
                    )
                    true
                }

                MotionEvent.ACTION_UP -> {
                    v.postDelayed(
                        {
                            v.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(), R.color.card_alternative
                                )
                            )
                        }, 100
                    )
                    v.performClick()
                }

                else -> {
                    false
                }
            }
        }
        binding.changePhotoLl.setOnTouchListener(onTouchListenerAnimation)
        binding.deletePhotoLl.setOnTouchListener(onTouchListenerAnimation)
        binding.cvNotifications.setOnTouchListener(touchListener)
        binding.cvGalery.setOnTouchListener(touchListener)
        binding.cvShare.setOnTouchListener(touchListener)
        binding.cvLogOut.setOnTouchListener(touchListener)
        binding.cvConfiguracion.setOnTouchListener(touchListener)
        binding.cvNotas.setOnTouchListener(touchListener)
    }

    private fun handleTouchEvent(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Store initial touch coordinates
                initialY = view.y
                touchStartY = event.rawY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Calculate the new Y position of the view
                val offset = event.rawY - touchStartY
                if (initialY + offset < initialY) {
                    view.y = initialY
                } else {
                    view.y = initialY + offset
                }

                showingPhotoCardview = if (view.y == initialY + view.height.toFloat()) {
                    false
                } else {
                    false
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Determine if the view should snap to the top or bottom
                if (view.y > initialY + SWIPE_THRESHOLD) {
                    view.animate().y(initialY + view.height.toFloat()).setDuration(300).start()
                    setTimeout(200) {
                        binding.overlayView.visibility = View.GONE
                        binding.overlayView.isClickable = false
                        binding.overlayView.isFocusable = false
                    }
                    showingPhotoCardview = false
                } else {
                    view.animate().y(initialY).setDuration(300).start()
                    showingPhotoCardview = true
                }
                return true
            }
        }
        return false
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

    private fun setTimeout(delayMillis: Long, callback: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            callback()
        }, delayMillis)
    }
}