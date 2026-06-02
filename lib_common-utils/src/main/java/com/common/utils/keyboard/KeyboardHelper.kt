package com.common.utils.keyboard

import android.animation.ObjectAnimator
import android.app.Activity
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.EditText

/**
 * 监听软键盘的打开和隐藏
 * 打开时滚动布局，可设置仅在某几个EditText获取焦点时开启
 *
 * @author Cuizhen
 * @version v1.0.0
 * @date 2018/3/30-上午9:06
 */
class KeyboardHelper private constructor() :
    ViewTreeObserver.OnGlobalFocusChangeListener, KeyboardCompat.OnStateChangeListener {

    private var window: Window? = null
    private var rootView: View? = null

    private var moveView: View? = null
    private var bottomView: View? = null
    private var focusViews: Array<out EditText>? = null
    private var duration: Long = 200
    private var onSoftInputListener: OnSoftInputListener? = null

    private var moveWithScroll = false

    private var isOpened = false
    private var moveHeight = 0
    private var isFocusChange = false

    private val moveRunnable = Runnable {
        if (isViewFocus()) {
            getBottomViewBottom()
            if (mBottomViewBottom < mKeyboardNowHeight) {
                val offHeight = mKeyboardNowHeight - mBottomViewBottom
                moveHeight = offHeight
                move()
            }
        } else {
            moveHeight = 0
            move()
        }
    }
    private var mBottomViewBottom = -1
    private var mKeyboardCompat: KeyboardCompat? = null
    private var mKeyboardNowHeight = 0

    fun attach(activity: Activity) {
        this.window = activity.window
        this.rootView = window!!.decorView.findViewById(android.R.id.content)
        window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        mKeyboardCompat = KeyboardCompat.with(activity)
        mKeyboardCompat!!.setOnStateChangeListener(this)
        mKeyboardCompat!!.attach()
    }

    fun detach() {
        mKeyboardCompat?.let {
            it.detach()
            mKeyboardCompat = null
        }
        window = null
        rootView = null
        moveView = null
        bottomView = null
        focusViews = null
    }

    fun `init`(moveView: View, bottomView: View, vararg focusViews: EditText): KeyboardHelper {
        this.moveView = moveView
        this.bottomView = bottomView
        this.focusViews = focusViews
        return this
    }

    fun listener(onSoftInputListener: OnSoftInputListener?): KeyboardHelper {
        this.onSoftInputListener = onSoftInputListener
        return this
    }

    fun duration(duration: Long): KeyboardHelper {
        this.duration = duration
        return this
    }

    /**
     * 设置moveView移动以ScrollY属性滚动内容
     */
    fun moveWithScroll(): KeyboardHelper {
        this.moveWithScroll = true
        return this
    }

    /**
     * 设置moveView移动以TranslationY属性移动位置
     */
    fun moveWithTranslation(): KeyboardHelper {
        this.moveWithScroll = false
        return this
    }

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        if (isOpened) {
            if (moveView != null && bottomView != null && focusViews != null) {
                isFocusChange = true
                rootView?.postDelayed(moveRunnable, 100)
            }
        }
    }

    override fun onStateChanged(isShown: Boolean, height: Int, orientation: Int) {
        mKeyboardNowHeight = height
        if (isShown) {
            if (!isOpened) {
                isOpened = true
                onSoftInputListener?.onOpen()
            }
            if (moveView != null && bottomView != null && focusViews != null) {
                if (isFocusChange) {
                    isFocusChange = false
                    rootView?.removeCallbacks(moveRunnable)
                }
                if (isViewFocus()) {
                    getBottomViewBottom()
                    if (mBottomViewBottom < mKeyboardNowHeight) {
                        val offHeight = height - mBottomViewBottom
                        moveHeight = offHeight
                        move()
                    }
                } else {
                    moveHeight = 0
                    move()
                }
            }
        } else {
            if (isOpened) {
                isOpened = false
                onSoftInputListener?.onClose()
            }
            if (moveView != null && bottomView != null && focusViews != null) {
                moveHeight = 0
                move()
            }
        }
    }

    private fun getBottomViewBottom() {
        if (mBottomViewBottom != -1) {
            return
        }
        val root = rootView ?: return
        val bottom = bottomView ?: return
        val rootViewLocation = IntArray(2)
        root.getLocationOnScreen(rootViewLocation)
        val rootViewY = rootViewLocation[1] + root.height
        val bottomLocation = IntArray(2)
        bottom.getLocationOnScreen(bottomLocation)
        val bottomY = bottomLocation[1] + bottom.height
        mBottomViewBottom = rootViewY - bottomY
    }

    private fun move() {
        if (moveWithScroll) {
            scrollTo(moveHeight)
        } else {
            translationTo(-moveHeight)
        }
    }

    private fun translationTo(to: Int) {
        val view = moveView ?: return
        val translationY = view.translationY
        if (translationY == to.toFloat()) {
            return
        }
        val anim = ObjectAnimator.ofFloat(view, "translationY", translationY, to.toFloat())
        anim.interpolator = DecelerateInterpolator()
        anim.duration = duration
        anim.start()
    }

    private fun scrollTo(to: Int) {
        val view = moveView ?: return
        val scrollY = view.scrollY
        if (scrollY == to) {
            return
        }
        val anim = ObjectAnimator.ofInt(view, "scrollY", scrollY, to)
        anim.interpolator = DecelerateInterpolator()
        anim.duration = duration
        anim.start()
    }

    /**
     * 判断软键盘打开状态的阈值
     * 此处以用户可用高度变化值大于1/4总高度时作为判断依据。
     *
     * @param usableHeightNow          当前可被用户使用的高度
     * @param usableHeightSansKeyboard 总高度，及包含软键盘占位的高度
     * @return boolean
     */
    private fun isSoftOpen(usableHeightNow: Int, usableHeightSansKeyboard: Int): Boolean {
        val heightDifference = usableHeightSansKeyboard - usableHeightNow
        return heightDifference > (usableHeightSansKeyboard / 4)
    }

    private fun isViewFocus(): Boolean {
        val views = focusViews
        if (views == null || views.isEmpty()) {
            return true
        }
        val focusView = window?.currentFocus
        for (editText in views) {
            if (focusView === editText) {
                return true
            }
        }
        return false
    }

    interface OnSoftInputListener {
        /**
         * 软键盘由关闭变为打开时调用
         */
        fun onOpen()

        /**
         * 软键盘由打开变为关闭时调用
         */
        fun onClose()
    }

    companion object {
        @Volatile
        private var singleton: KeyboardHelper? = null

        @JvmStatic
        fun getInstance(): KeyboardHelper {
            return singleton ?: synchronized(this) {
                singleton ?: KeyboardHelper().also { singleton = it }
            }
        }
    }
}