package com.jstappdev.e6bflightcomputer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Magnifier
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class BackView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private var currentRotation = 0f
        private var lockState: LockState = LockState.UNLOCKED
        private var sliderY = 3f
        private var dotX = 0f
        private var dotY = 0f
        private var isDotSet: Boolean = false
    }

    private var sliderMatrixWithTranslation: Matrix = Matrix()
    private var screenHeight: Int = 0
    private var screenWidth: Int = 0
    private var isRotating = false
    private var initialScaleFactor = 1.0f
    private var initialX = 0f
    private var initialY = 0f
    private val matrix = Matrix()
    private var sliderDrawable: Drawable? = null
    private var outerDrawable: Drawable? = null
    private var innerDrawable: Drawable? = null
    private val innerMatrix = Matrix()
    private val gestureDetector = GestureDetector(context, GestureListener())
    private var scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleFactor = 1f
    private var isZoomedIn = false
    private var lastAngle = 0f
    private var isPanning = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val paint = Paint()
    private lateinit var lockButton: AppCompatImageButton

    private lateinit var magnifier: Magnifier

    enum class LockState {
        UNLOCKED, PARTIALLY_LOCKED, FULLY_LOCKED
    }

    fun setLockButton(button: AppCompatImageButton) {
        lockButton = button
        updateLockButton()

        lockButton.setOnClickListener { v: View? ->
            lockState = when (lockState) {
                LockState.UNLOCKED -> LockState.PARTIALLY_LOCKED
                LockState.PARTIALLY_LOCKED -> LockState.FULLY_LOCKED
                LockState.FULLY_LOCKED -> LockState.UNLOCKED
            }
            updateLockButton()
        }
    }

    fun setMagnifier(m: Magnifier) {
        this.magnifier = m
    }

    private fun updateLockButton() {
        when (lockState) {
            LockState.UNLOCKED -> {
                lockButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        context, android.R.drawable.ic_notification_overlay
                    )
                )
                lockButton.setBackgroundResource(
                    android.R.drawable.ic_menu_rotate
                )
            }

            LockState.PARTIALLY_LOCKED -> {
                lockButton.setBackgroundResource(android.R.drawable.ic_menu_rotate)
                lockButton.setImageDrawable(null)
            }

            LockState.FULLY_LOCKED -> {
                lockButton.setBackgroundResource(android.R.drawable.ic_lock_lock)
                lockButton.setImageDrawable(null)
            }
        }
    }

    init {
        sliderDrawable = ContextCompat.getDrawable(context, R.drawable.wind_slider)
        sliderDrawable?.setBounds(
            0, 0, sliderDrawable!!.intrinsicWidth, sliderDrawable!!.intrinsicHeight
        )
        outerDrawable = ContextCompat.getDrawable(context, R.drawable.wind_outer)
        outerDrawable?.setBounds(
            0, 0, outerDrawable!!.intrinsicWidth, outerDrawable!!.intrinsicHeight
        )
        innerDrawable = ContextCompat.getDrawable(context, R.drawable.wind_dial)
        innerDrawable?.setBounds(
            0, 0, innerDrawable!!.intrinsicWidth, innerDrawable!!.intrinsicHeight
        )
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        screenWidth = MeasureSpec.getSize(widthMeasureSpec)
        screenHeight = MeasureSpec.getSize(heightMeasureSpec)
        initialScaleFactor = screenWidth.toFloat() / outerDrawable!!.intrinsicWidth.toFloat()
        scaleFactor = initialScaleFactor

        matrix.reset()

        val (cx, cy) = getCenter()

        initialX = cx
        initialY = cy

        matrix.postScale(scaleFactor, scaleFactor)
        matrix.postTranslate(cx, cy)

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the sliderDrawable with a scaled vertical translation of sliderY
        canvas.save()
        sliderMatrixWithTranslation.set(matrix)
        sliderMatrixWithTranslation.postTranslate(
            0f, sliderY * scaleFactor
        ) // Scale the translation
        canvas.concat(sliderMatrixWithTranslation)
        sliderDrawable?.draw(canvas)
        canvas.restore()

        // Draw the outerDrawable without any additional transformation
        canvas.concat(matrix)
        outerDrawable?.draw(canvas)

        innerMatrix.setRotate(
            currentRotation,
            (outerDrawable?.intrinsicWidth ?: 0) / 2f,
            (outerDrawable?.intrinsicHeight ?: 0) / 2f
        )
        canvas.concat(innerMatrix)
        innerDrawable?.draw(canvas)

        if (isDotSet) canvas.drawCircle(dotX, dotY, 10f, paint)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent): Boolean {

            scaleFactor = if (isZoomedIn) initialScaleFactor else 1.3f

            val (cx, cy) = getCenter()

            matrix.reset()
            matrix.postScale(scaleFactor, scaleFactor)
            matrix.postTranslate(cx, cy)

            isZoomedIn = !isZoomedIn

            invalidate()

            return true
        }
    }

    private fun getCenter(): Pair<Float, Float> {
        val drawableWidth = outerDrawable?.intrinsicWidth?.times(scaleFactor) ?: 0f
        val drawableHeight = outerDrawable?.intrinsicHeight?.times(scaleFactor) ?: 0f
        val x = (screenWidth - drawableWidth) / 2f
        val y = (screenHeight - drawableHeight) / 2f
        return Pair(x, y)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (lockState != LockState.FULLY_LOCKED && isTouchInsideDial(event.x, event.y)) {
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
                updateDot(event.x, event.y)

                if (isRotating) {
                    val newAngle = calculateAngle(event.x, event.y)
                    val deltaAngle = newAngle - lastAngle
                    currentRotation += deltaAngle
                    lastAngle = newAngle
                    invalidate()
                } else if (isPanning) {
                    if (event.pointerCount == 1) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        if (lockState != LockState.FULLY_LOCKED && isTouchOutsideOuterDial(
                                event.x, event.y
                            )
                        ) sliderY += dy
                        else matrix.postTranslate(dx, dy)

                        invalidate()

                        lastTouchX = event.x
                        lastTouchY = event.y
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isRotating = false
                isPanning = false
                magnifier.dismiss()
            }
        }

        return true
    }

    private fun isTouchInsideDial(x: Float, y: Float): Boolean {
        // Center of the drawable after translation and scaling
        val centerX = outerDrawable!!.bounds.centerX().toFloat()
        val centerY = outerDrawable!!.bounds.centerY().toFloat()

        // Calculate the radius of the circle (considering scaling)
        val radius = (innerDrawable?.intrinsicWidth ?: 0) / 2f * scaleFactor

        // Apply the inverse of the outer matrix (translation + scaling)
        val inverseOuterMatrix = Matrix()
        matrix.invert(inverseOuterMatrix)

        val point = floatArrayOf(x, y)
        inverseOuterMatrix.mapPoints(point)

        // Now apply the inverse of the rotation around the center
        val angleInRadians = Math.toRadians(-currentRotation.toDouble()).toFloat()
        val sinAngle = sin(angleInRadians.toDouble()).toFloat()
        val cosAngle = cos(angleInRadians.toDouble()).toFloat()

        val dx = point[0] - centerX
        val dy = point[1] - centerY

        // Apply reverse rotation
        val transformedX = centerX + (dx * cosAngle - dy * sinAngle)
        val transformedY = centerY + (dx * sinAngle + dy * cosAngle)

        // Calculate the distance from the transformed touch point to the center of the circle
        val distance =
            sqrt((transformedX - centerX).pow(2) + (transformedY - centerY).pow(2)) * scaleFactor

        return distance <= radius
    }


    private fun updateDot(x: Float, y: Float) {
        // Center of the drawable after translation and scaling
        val centerX = outerDrawable!!.bounds.centerX().toFloat()
        val centerY = outerDrawable!!.bounds.centerY().toFloat()

        // Calculate the radius of the circle (considering scaling)
        val radius = (innerDrawable?.intrinsicWidth ?: 0) / 2f * scaleFactor

        // Apply the inverse of the outer matrix (translation + scaling)
        val inverseOuterMatrix = Matrix()
        matrix.invert(inverseOuterMatrix)
        val point = floatArrayOf(x, y)
        inverseOuterMatrix.mapPoints(point)

        // Now apply the inverse of the rotation around the center
        val angleInRadians = Math.toRadians(-currentRotation.toDouble()).toFloat()
        val sinAngle = sin(angleInRadians.toDouble()).toFloat()
        val cosAngle = cos(angleInRadians.toDouble()).toFloat()

        val dx = point[0] - centerX
        val dy = point[1] - centerY

        // Apply reverse rotation
        val transformedX = centerX + (dx * cosAngle - dy * sinAngle)
        val transformedY = centerY + (dx * sinAngle + dy * cosAngle)

        // Calculate the distance from the transformed touch point to the center of the circle
        val distance =
            sqrt((transformedX - centerX).pow(2) + (transformedY - centerY).pow(2)) * scaleFactor

        // Optionally, update the dot position if the touch is inside transparent area
        val isInnerDial = distance <= radius - 170 * scaleFactor
        if (lockState == LockState.UNLOCKED && isInnerDial) {
            isRotating = false
            dotX = centerX + ((dx) * cosAngle - (dy) * sinAngle)
            dotY = centerY + ((dx) * sinAngle + (dy) * cosAngle)
            isDotSet = true
            magnifier.show(x, y)
            invalidate()
        }
    }

    private fun isTouchOutsideOuterDial(x: Float, y: Float): Boolean {
        val centerX = outerDrawable!!.bounds.centerX()
        val centerY = outerDrawable!!.bounds.centerY()

        // Calculate the radius of the circle
        val radius = (innerDrawable?.intrinsicWidth ?: 0) / 2f
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
        return distance > (radius + 120)
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
