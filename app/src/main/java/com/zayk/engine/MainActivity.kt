package com.zayk.engine

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var glView: ZaykGLView
    private lateinit var renderer: ZaykRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val hud = TextView(this).apply {
            setTextColor(0xAAFFFFFF.toInt())
            textSize = 10f
            typeface = Typeface.MONOSPACE
        }
        renderer = ZaykRenderer { fps -> runOnUiThread { hud.text = "ZaykEngine v0.1 | $fps FPS" } }
        glView = ZaykGLView(this, renderer)

        fun btn(label: String, onClick: () -> Unit = {}) = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 12f
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { onClick() }
        }

        val logo = TextView(this).apply {
            text = "ZAYK ENGINE"
            setTextColor(0xFF4F8CFF.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dp(10), 0)
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xCC2B2D31.toInt())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            addView(logo)
            addView(btn("+ Cubo") { glView.queueEvent { renderer.spawn(MeshKind.CUBE) } })
            addView(btn("+ Esfera") { glView.queueEvent { renderer.spawn(MeshKind.SPHERE) } })
            addView(btn("+ Plano") { glView.queueEvent { renderer.spawn(MeshKind.PLANE) } })
        }

        fun holdBtn(label: String, value: Float) = btn(label).apply {
            setOnTouchListener { _, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> renderer.input.vert = value
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> renderer.input.vert = 0f
                }
                true
            }
        }

        val updown = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(holdBtn("▲", 1f))
            addView(holdBtn("▼", -1f))
        }

        val root = FrameLayout(this)
        root.addView(glView)
        root.addView(bar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ))
        root.addView(hud, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START
        ).apply { topMargin = dp(48); leftMargin = dp(8) })
        root.addView(updown, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        ).apply { bottomMargin = dp(12); rightMargin = dp(12) })
        setContentView(root)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        glView.onPause()
        super.onPause()
    }
}