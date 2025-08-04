package com.jstappdev.e6bflightcomputer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat

class Front : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.front)
        val lockButton: AppCompatImageButton = findViewById(R.id.lockButton)
        val frontView: FrontView = findViewById(R.id.tas)

        frontView.setLockButton(lockButton)

        findViewById<SwitchCompat>(R.id.switch1).setOnCheckedChangeListener { _, _ ->
            startActivity(Intent(this, Back::class.java))
        }
    }
}