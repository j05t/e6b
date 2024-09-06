package com.jstappdev.e6bflightcomputer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat

class Back : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.back)
        val lockButton: AppCompatImageButton = findViewById(R.id.lockButton)
        val backView: BackView = findViewById(R.id.wind)

        backView.setLockButton(lockButton)

        findViewById<SwitchCompat>(R.id.switch2).setOnCheckedChangeListener { _, _ ->
            startActivity(
                Intent(
                    this, Front::class.java
                )
            )

        }
    }
}