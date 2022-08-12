package com.grandfatherpikhto.multistatebutton

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat

class MultiStateButton (context: Context, private val attrs: AttributeSet) : View(context, attrs) {
    companion object {
        const val MIN_WIDTH  = 64
        const val MIN_HEIGHT = 64
    }
    private val tagLog = javaClass.simpleName

    private var slicePaint: Paint = Paint()

    private var srcCompat:Int = 0

    private var currentState = 0
    private val states = mutableListOf <MultiStateData>()
    private val newMatrix = Matrix()
    val state:Int
        get() {
            if (states.isNotEmpty()) {
                return states[currentState].resId
            }

            return 0
        }

    private var changeStatusListener:((Int, Int, Boolean, View) -> Unit)? = null

    private val minHeight:Int
        get() {
            getCurrentBitmap()?.let { bitmap ->
                return bitmap.height.coerceAtLeast(MIN_HEIGHT) + paddingTop + paddingBottom
            }
            return MIN_HEIGHT + paddingTop + paddingBottom
        }

    private val minWidth:Int
        get() {
            getCurrentBitmap()?.let { bitmap ->
                return bitmap.width.coerceAtLeast(MIN_HEIGHT) + paddingLeft + paddingRight
            }
            return MIN_WIDTH + paddingLeft + paddingRight
        }

    init {
        readAttributes()
        slicePaint.isAntiAlias = true
        slicePaint.isDither    = true
        slicePaint.style       = Paint.Style.FILL
        setOnClickListener { _ ->
            nextState()
            invalidate()
        }
    }

    fun setCurrentResId(resId: Int) : Boolean {
        states.forEachIndexed { index, multiStateData ->
            if (multiStateData.resId == resId) {
                states[index].enabled = true
                currentState = index
                invalidate()
                changeStatusListener?.let { listener ->
                    listener(currentState, states[index].resId, false, this)
                }
                return true
            }
        }
        return false
    }

    private fun nextState() {
        do {
            currentState = currentState.inc().rem(states.size)
        } while (!states[currentState].enabled)
        changeStatusListener?.let { listener ->
            listener(currentState, states[currentState].resId, true, this)
        }
    }

    private fun readAttributes() {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MultistateButton,0, 0).apply {
            try {
                srcCompat = getResourceId(R.styleable.MultistateButton_srcCompat, 0)
            } finally {
                recycle()
            }
        }
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int) : Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> {
                //Must be this size
                specSize
            }
            MeasureSpec.AT_MOST -> {
                //Can't be bigger than...
                desiredSize.coerceAtMost(specSize)
            }
            else -> {
                //Be whatever you want
                desiredSize
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //Measure Width
        val width  = measureDimension(minWidth, widthMeasureSpec)
        //Measure Height
        val height = measureDimension(minHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    private fun getBitmap(vectorDrawable: VectorDrawable): Bitmap? {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    private fun getBitmap(context: Context, drawableId: Int): Bitmap? {
        return when (val drawable: Drawable? = ContextCompat.getDrawable(context, drawableId)) {
            is BitmapDrawable -> {
                BitmapFactory.decodeResource(context.resources, drawableId)
            }
            is VectorDrawable -> {
                getBitmap(drawable)
            }
            else -> {
                throw IllegalArgumentException("unsupported drawable type")
            }
        }
    }

    private fun getCurrentBitmap() : Bitmap? =
        if (states.isNotEmpty() && states[currentState].enabled) {
            getBitmap(context, states[currentState].resId)
        } else if (srcCompat > 0) {
            getBitmap(context, srcCompat)
        } else null

    fun addState(multiState: MultiStateData) {
        if (!states.contains(multiState)) {
            states.add(multiState)
            currentState = 0
        }
    }

    fun setStates(multiStates: List<MultiStateData>) {
        states.clear()
        multiStates.forEach { state ->
            addState(state)
        }
    }

    fun enableState(resId: Int, enable: Boolean = true) {
        if (states.isNotEmpty()) {
            states.forEachIndexed { index, state ->
                if (state.resId == resId) {
                    state.enabled = enable
                    if (!state.enabled && currentState == index) {
                        nextState()
                    }
                    return@forEachIndexed
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let { piece ->
            getCurrentBitmap()?.let { bitmap ->
                val translateX = (measuredWidth.toFloat() - bitmap.width.toFloat()) / 2F
                val translateY = (measuredHeight.toFloat() - bitmap.height.toFloat()) / 2F
                newMatrix.setTranslate(translateX, translateY)
                piece.drawBitmap(bitmap, newMatrix, slicePaint)
            }
        }

        super.onDraw(canvas)
    }

    fun setOnChangeStatusListener(listener:((current: Int, resId:Int, user: Boolean, view: View) -> Unit)) {
        changeStatusListener = listener
    }
}