package com.fox.gameoverlay

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        val fpsBar = findViewById<SeekBar>(R.id.fpsBar)
        val fpsLabel = findViewById<TextView>(R.id.fpsLabel)
        val alphaBar = findViewById<SeekBar>(R.id.alphaBar)
        val alphaLabel = findViewById<TextView>(R.id.alphaLabel)
        val thickBar = findViewById<SeekBar>(R.id.thickBar)
        val thickLabel = findViewById<TextView>(R.id.thickLabel)
        val colorHex = findViewById<EditText>(R.id.colorHex)
        val fovEdit = findViewById<EditText>(R.id.fovEdit)
        val speedEdit = findViewById<EditText>(R.id.speedEdit)
        val aimCheck = findViewById<CheckBox>(R.id.aimCheck)
        val saveBtn = findViewById<Button>(R.id.btnSave)

        val curFps = prefs.getInt("fps", 30)
        fpsBar.max = 59
        fpsBar.progress = curFps - 1
        fpsLabel.text = "FPS: $curFps"

        val curAlpha = (prefs.getFloat("alpha", 1f) * 100).toInt()
        alphaBar.progress = curAlpha
        alphaLabel.text = "Прозрачность: ${prefs.getFloat("alpha", 1f)}"

        val curThick = prefs.getFloat("thickness", 3f)
        thickBar.max = 20
        thickBar.progress = curThick.toInt()
        thickLabel.text = "Толщина: $curThick"

        colorHex.setText(String.format("#%06X", 0xFFFFFF and prefs.getInt("color", Color.RED)))
        fovEdit.setText(prefs.getInt("fov", 300).toString())
        speedEdit.setText(prefs.getLong("aimDuration", 40L).toString())
        aimCheck.isChecked = prefs.getBoolean("aimEnabled", false)

        fpsBar.setOnSeekBarChangeListener(simple { p, _ -> fpsLabel.text = "FPS: ${p + 1}" })
        alphaBar.setOnSeekBarChangeListener(simple { p, _ -> alphaLabel.text = "Прозрачность: ${p / 100f}" })
        thickBar.setOnSeekBarChangeListener(simple { p, _ -> thickLabel.text = "Толщина: $p" })

        saveBtn.setOnClickListener {
            val fps = fpsBar.progress + 1
            val alpha = alphaBar.progress / 100f
            val thick = thickBar.progress.toFloat()
            val color = try {
                Color.parseColor(colorHex.text.toString())
            } catch (_: Throwable) {
                Color.RED
            }
            val fov = fovEdit.text.toString().toIntOrNull() ?: 300
            val speed = speedEdit.text.toString().toLongOrNull() ?: 40L

            prefs.edit()
                .putInt("fps", fps)
                .putFloat("alpha", alpha)
                .putFloat("thickness", thick)
                .putInt("color", color)
                .putInt("fov", fov)
                .putLong("aimDuration", speed)
                .putBoolean("aimEnabled", aimCheck.isChecked)
                .apply()

            finish()
        }
    }

    private fun simple(onChange: (Int, Boolean) -> Unit): SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) = onChange(p, fromUser)
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
}
