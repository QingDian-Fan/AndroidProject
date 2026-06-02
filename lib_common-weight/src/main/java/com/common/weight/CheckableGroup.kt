package com.common.weight

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout

class CheckableGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GridLayout(context, attrs) {

    enum class CheckMode { SINGLE, MULTIPLE }

    private val items = mutableListOf<CheckableItemView>()

    var checkMode: CheckMode = CheckMode.SINGLE
        private set

    /**
     * 类似 RadioGroup 的选中监听
     */
    fun interface OnCheckedChangeListener {
        fun onCheckedChanged(
            group: CheckableGroup,
            checkedItem: CheckableItemView?
        )
    }

    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        this.onCheckedChangeListener = listener
    }

    init {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.CheckableGroup)
            val mode = ta.getInt(R.styleable.CheckableGroup_checkMode, 0)
            checkMode = if (mode == 1) CheckMode.MULTIPLE else CheckMode.SINGLE
            ta.recycle()
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val contentWidth = parentWidth - paddingLeft - paddingRight

        if (contentWidth > 0 && columnCount > 0) {
            val cellWidth = contentWidth / columnCount

            for (i in 0 until childCount) {
                val child = getChildAt(i)

                if (child is CheckableItemView) {
                    val lp = child.layoutParams as? LayoutParams
                        ?: LayoutParams()

                    val horizontalMargin = lp.leftMargin + lp.rightMargin
                    val realWidth = (cellWidth - horizontalMargin).coerceAtLeast(0)

                    lp.width = realWidth
                    lp.height = LayoutParams.WRAP_CONTENT

                    child.layoutParams = lp
                }
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)

        if (child is CheckableItemView) {
            register(child)
        }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)

        if (child is CheckableItemView) {
            items.remove(child)
        }
    }

    private fun register(item: CheckableItemView) {
        if (items.contains(item)) return
        items.add(item)

        item.setOnClickListener {
            handleClick(item)
        }
    }

    private fun handleClick(target: CheckableItemView) {
        when (checkMode) {
            CheckMode.SINGLE -> {
                val oldCheckedItem = getCheckedItem()

                items.forEach {
                    it.setChecked(it == target)
                }

                val newCheckedItem = getCheckedItem()

                if (oldCheckedItem != newCheckedItem) {
                    onCheckedChangeListener?.onCheckedChanged(this, newCheckedItem)
                }
            }

            CheckMode.MULTIPLE -> {
                target.toggle()

                onCheckedChangeListener?.onCheckedChanged(this, target)
            }
        }
    }

    fun clearCheck() {
        val oldCheckedItem = getCheckedItem()

        items.forEach { it.setChecked(false) }

        if (oldCheckedItem != null) {
            onCheckedChangeListener?.onCheckedChanged(this, null)
        }
    }

    fun getCheckedItems(): List<CheckableItemView> =
        items.filter { it.isChecked }

    fun getCheckedItem(): CheckableItemView? =
        items.firstOrNull { it.isChecked }
}