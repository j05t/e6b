package com.jstappdev.e6bflightcomputer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class FrontView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isRotating = false
    private var initialScaleFactor = 1.0f
    private val matrix = Matrix()
    private var outerDrawable: Drawable? = null
    private var innerDrawable: Drawable? = null
    private val innerMatrix = Matrix()
    private val gestureDetector = GestureDetector(context, GestureListener())
    private var scaleFactor = 1f
    private var isZoomedIn = false
    private var currentRotation = 290.5f
    private var lastAngle = 0f
    private var isPanning = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private lateinit var lockButton: AppCompatImageButton
    private var isLocked = false
    private var scaleGestureDetector: ScaleGestureDetector

    init {
        outerDrawable = ContextCompat.getDrawable(context, R.drawable.tasbase)
        outerDrawable?.setBounds(
            0, 0, outerDrawable!!.intrinsicWidth, outerDrawable!!.intrinsicHeight
        )
        innerDrawable = ContextCompat.getDrawable(context, R.drawable.tasdial)
        innerDrawable?.setBounds(
            0, 0, innerDrawable!!.intrinsicWidth, innerDrawable!!.intrinsicHeight
        )
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            isRotating = false
            isPanning = false

            // Adjust the scale factor based on the scale gesture
            scaleFactor *= detector.scaleFactor

            // Restrict the scale factor to a certain range to prevent excessive zooming
            scaleFactor = scaleFactor.coerceIn(0.1f, 5.0f)

            // Apply the scale transformation
            matrix.postScale(
                detector.scaleFactor, detector.scaleFactor, detector.focusX, detector.focusY
            )

            // Update the slider matrix to match the new transformation
            invalidate()

            return true
        }
    }

    fun setLockButton(button: AppCompatImageButton) {
        lockButton = button
        lockButton.setBackgroundResource(android.R.drawable.ic_menu_rotate)

        lockButton.setOnClickListener { v: View? ->
            isLocked = !isLocked
            if (isLocked) {
                v!!.setBackgroundResource(android.R.drawable.ic_lock_lock)
            } else {
                v!!.setBackgroundResource(
                    android.R.drawable.ic_menu_rotate
                )
            }
        }
    }

    private fun centerDrawable() {
        matrix.reset()

        val drawableWidth = outerDrawable?.intrinsicWidth?.times(scaleFactor) ?: 0f
        val drawableHeight = outerDrawable?.intrinsicHeight?.times(scaleFactor) ?: 0f

        val dx = (width - drawableWidth) / 2f
        val dy = (height - drawableHeight) / 2f

        matrix.postScale(scaleFactor, scaleFactor)
        matrix.postTranslate(dx, dy)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        initialScaleFactor = w.toFloat() / outerDrawable!!.intrinsicWidth.toFloat()
        scaleFactor = initialScaleFactor

        matrix.reset()

        val drawableWidth = outerDrawable?.intrinsicWidth?.times(scaleFactor) ?: 0f
        val drawableHeight = outerDrawable?.intrinsicHeight?.times(scaleFactor) ?: 0f

        val dx = (w - drawableWidth) / 2f
        val dy = (h - drawableHeight) / 2f

        matrix.postScale(scaleFactor, scaleFactor)
        matrix.postTranslate(dx, dy)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(matrix)
        outerDrawable?.draw(canvas)
        canvas.save()

        innerMatrix.reset()
        innerMatrix.postRotate(
            currentRotation,
            outerDrawable!!.intrinsicWidth / 2f,
            outerDrawable!!.intrinsicHeight / 2f
        )
        canvas.concat(innerMatrix)
        innerDrawable?.draw(canvas)

        canvas.restore()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent): Boolean {
            isZoomedIn = !isZoomedIn
            scaleFactor = if (isZoomedIn) {
                1.1f
            } else {
                initialScaleFactor
            }
            centerDrawable()
            invalidate()
            return true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (!isLocked && isTouchInsideCircle(event.x, event.y)) {
                    lastAngle = calculateAngle(event.x, event.y)
                    isRotating = true
                } else {
                    isPanning = true
                    isRotating = false
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isRotating) {
                    val newAngle = calculateAngle(event.x, event.y)
                    val deltaAngle = newAngle - lastAngle
                    currentRotation += deltaAngle
                    lastAngle = newAngle
                    invalidate()
                } else if (isPanning) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    matrix.postTranslate(dx, dy)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isRotating = false
                isPanning = false
            }
        }

        return true
    }

    private fun isTouchInsideCircle(x: Float, y: Float): Boolean {
        val centerX = outerDrawable!!.bounds.centerX()
        val centerY = outerDrawable!!.bounds.centerY()

        // Calculate the radius of the circle
        val radius = (innerDrawable?.intrinsicWidth ?: 0) / 2f - 110
        // Apply the inverse matrix transformation to get the position before transformation
        val inverseMatrix = Matrix()
        matrix.invert(inverseMatrix)

        val point = floatArrayOf(x, y)
        inverseMatrix.mapPoints(point)

        // Get the transformed touch coordinates
        val transformedX = point[0]
        val transformedY = point[1]

        // Calculate the distance from the touch point to the center of the circle
        val distance = sqrt(((transformedX - centerX).pow(2) + (transformedY - centerY).pow(2)))

        // Return true if the distance is less than or equal to the radius
        return distance <= radius
    }

    private fun calculateAngle(x: Float, y: Float): Float {
        // Get the center of the outerDrawable in screen coordinates
        val centerX = outerDrawable!!.bounds.centerX()
        val centerY = outerDrawable!!.bounds.centerY()

        // Apply the inverse matrix transformation to get the position before transformation
        val inverseMatrix = Matrix()
        matrix.invert(inverseMatrix)

        val point = floatArrayOf(x, y)
        inverseMatrix.mapPoints(point)

        // Get the transformed touch coordinates
        val dx = point[0]
        val dy = point[1]

        // Calculate the angle from the adjusted touch point to the center of the drawable
        val angle = Math.toDegrees(
            atan2((dy - centerY).toDouble(), (dx - centerX).toDouble())
        ).toFloat()

        // Normalize the angle to be between 0 and 360 degrees
        return if (angle < 0) angle + 360f else angle
    }
}
