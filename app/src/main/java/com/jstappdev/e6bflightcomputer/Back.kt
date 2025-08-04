package com.jstappdev.e6bflightcomputer

import android.content.Intent
import android.os.Bundle
import android.widget.Magnifier
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat


class Back : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.back)
        val lockButton: AppCompatImageButton = findViewById(R.id.lockButton)
        val backView: BackView = findViewById(R.id.wind)
        val magnifier = Magnifier.Builder(backView).setCornerRadius(200f).setSize(350, 350)
            .setDefaultSourceToMagnifierOffset(0, -300).build()

        backView.setLockButton(lockButton)
        backView.setMagnifier(magnifier)

        findViewById<SwitchCompat>(R.id.switch2).setOnCheckedChangeListener { _, _ ->
            startActivity(Intent(this, Front::class.java))
        }
    }
}