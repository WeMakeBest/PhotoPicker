package com.softsun.photopicker.views

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.softsun.photopicker.R

class CustomButton(context: Context, attributes: AttributeSet) : LinearLayout(context, attributes)  {

    private var textView: TextView
    private var btnRoot: LinearLayout

    enum class IconPos {
        Left,
        Right
    }


    init {
        inflate(getContext(), R.layout.custom_btn_layout, this)

        textView = findViewById(R.id.textView)
        btnRoot = findViewById(R.id.btnRoot)

        val typedArray: TypedArray = context.obtainStyledAttributes(attributes, R.styleable.CustomButton)

        typedArray.getString(R.styleable.CustomButton_btn_text)?.let { setText(it) }

        val drawable = typedArray.getDrawable(R.styleable.CustomButton_btn_icon)

        val iconPosition = IconPos.entries[typedArray.getInt(R.styleable.CustomButton_btn_iconPosition, 0)]

        val textSize = typedArray.getDimension(R.styleable.CustomButton_btn_textSizeCustom, -1F)
        val iconColor = typedArray.getColor(R.styleable.CustomButton_btn_iconTint, ContextCompat.getColor(getContext(), R.color.white))
        val textColor = typedArray.getColor(R.styleable.CustomButton_btn_textColor, ContextCompat.getColor(getContext(), R.color.white))
        val backgroundColor = typedArray.getColor(R.styleable.CustomButton_btn_backgroundColor, ContextCompat.getColor(getContext(), R.color.photo_picker_color_primary))

        setImageIcon(drawable, iconPosition)
        setIconTint(iconColor)
        setBackgroundTint(backgroundColor)
        setTextTint(textColor)
        setTextSizeCustom(textSize)

        typedArray.recycle()
    }

    fun setTextSizeCustom(textSize : Float) {
        if (textSize != -1F) {
            textView.textSize = textSize
        }
    }

    fun setBackgroundTint(color: Int?) {
        if (color != null) {
            btnRoot.backgroundTintList = ColorStateList.valueOf(color)
        }
    }

    fun setIconTint(color: Int?) {
        if (color != null) {
            TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(color))
        }
    }

    fun setImageIcon(drawable : Drawable?, iconPos: IconPos) {
        if (drawable != null) {
            if (iconPos == IconPos.Left) {
                textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
            }
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

    fun setTextTint(color : Int) {
        textView.setTextColor(color)
    }

    fun setText(text : String) {
        textView.text = text
    }
}