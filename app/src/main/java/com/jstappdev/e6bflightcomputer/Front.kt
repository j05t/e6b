package com.jstappdev.e6bflightcomputer

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

data class TutorialStep(
    val title: String,
    val instructions: String,
    val mainTargetRotation: Float,
    val targetRotationYellow: Float,
    val targetRotationBlue: Float?
)

class Front : AppCompatActivity() {

    private lateinit var tutorialSteps: List<TutorialStep>
    private lateinit var frontView: FrontView
    private lateinit var instructionsTextView: TextView
    private lateinit var animateButton: Button
    private var isTutorialOpen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.front)

        frontView = findViewById(R.id.tas)
        val lockButton: AppCompatImageButton = findViewById(R.id.lockButton)
        frontView.setLockButton(lockButton)

        findViewById<SwitchCompat>(R.id.switch1).setOnCheckedChangeListener { _, _ ->
            startActivity(Intent(this, Back::class.java))
        }

        loadTutorialSteps()
        setupTutorial()
        setupSlideableTutorial()
    }

    private fun loadTutorialSteps() {
        val inputStream = resources.openRawResource(R.raw.questions)
        val reader = InputStreamReader(inputStream)
        val stepType = object : TypeToken<List<TutorialStep>>() {}.type
        tutorialSteps = Gson().fromJson(reader, stepType)
    }

    private fun setupTutorial() {
        val tutorialSpinner: Spinner = findViewById(R.id.tutorialSpinner)
        instructionsTextView = findViewById(R.id.instructionsTextView)
        animateButton = findViewById(R.id.animateButton)

        val adapter =
            ArrayAdapter(this, R.layout.custom_spinner_item, tutorialSteps.map { it.title })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tutorialSpinner.adapter = adapter

        tutorialSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                frontView.clearHighlights()
                val selectedStep = tutorialSteps[position]
                instructionsTextView.text = selectedStep.instructions
                animateButton.setOnClickListener {
                    frontView.animateInnerWheelTo(
                        selectedStep.mainTargetRotation,
                        selectedStep.targetRotationYellow,
                        selectedStep.targetRotationBlue
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                instructionsTextView.text = ""
                animateButton.setOnClickListener(null)
            }
        }
    }

    private fun setupSlideableTutorial() {
        val handleContainer = findViewById<FrameLayout>(R.id.handleContainer)
        handleContainer.setOnClickListener {
            toggleTutorial()
        }
    }

    private fun toggleTutorial() {
        animateTutorial(!isTutorialOpen)
    }

    private fun animateTutorial(open: Boolean) {
        isTutorialOpen = open
        val tutorialLayout = findViewById<View>(R.id.tutorialLayout)
        val parent = tutorialLayout.parent as ViewGroup
        val targetY = if (open) {
            (parent.height - tutorialLayout.height).toFloat()
        } else {
            parent.height - 100f // Hardcoded value to ensure handle is always visible
        }
        ObjectAnimator.ofFloat(tutorialLayout, "y", targetY).apply {
            duration = 300
            start()
        }
    }
}