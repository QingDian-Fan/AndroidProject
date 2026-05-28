package com.common.weight.titlebar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.common.weight.R

/**
 * 通用标题栏
 *
 * Originally based on: https://gitee.com/swjt-hy/android-titlebar
 */
class CommonTitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs), View.OnClickListener {

    private var viewStatusBarFill: View? = null
    private var viewBottomLine: View? = null
    private var viewBottomShadow: View? = null
    private var rlMain: RelativeLayout? = null

    private var tvLeft: TextView? = null
    private var btnLeft: ImageButton? = null
    private var viewCustomLeft: View? = null
    private var tvRight: TextView? = null
    private var btnRight: ImageButton? = null
    private var viewCustomRight: View? = null
    private var llMainCenter: LinearLayout? = null
    private var tvCenter: TextView? = null
    private var tvCenterSub: TextView? = null
    private var progressCenter: ProgressBar? = null
    private var rlMainCenterSearch: RelativeLayout? = null
    private var etSearchHint: EditText? = null
    private var ivSearch: ImageView? = null
    private var ivVoice: ImageView? = null
    private var centerCustomView: View? = null

    private var openStatusBar = true
    private var fillStatusBar = true
    private var titleBarColor = 0
    private var titleBarHeight = 0
    private var statusBarColor = 0
    private var statusBarMode = 0

    private var showBottomLine = true
    private var bottomLineColor = 0
    private var bottomShadowHeight = 0f

    private var leftType = TYPE_LEFT_NONE
    private var leftText: String? = null
    private var leftTextColor = 0
    private var leftTextSize = 0f
    private var leftDrawable = 0
    private var leftDrawablePadding = 0f
    private var leftImageResource = 0
    private var leftCustomViewRes = 0

    private var rightType = TYPE_RIGHT_NONE
    private var rightText: String? = null
    private var rightTextColor = 0
    private var rightTextSize = 0f
    private var rightImageResource = 0
    private var rightCustomViewRes = 0

    private var centerType = TYPE_CENTER_NONE
    private var centerText: String? = null
    private var centerTextColor = 0
    private var centerTextSize = 0f
    private var centerTextMarquee = true
    private var centerSubText: String? = null
    private var centerSubTextColor = 0
    private var centerSubTextSize = 0f
    private var centerSearchEditable = true
    private var centerSearchBgResource = 0
    private var centerSearchRightType = TYPE_CENTER_SEARCH_RIGHT_VOICE
    private var centerCustomViewRes = 0

    private var padding5 = 0
    private var padding12 = 0

    private var listener: OnTitleBarListener? = null
    private var doubleClickListener: OnTitleBarDoubleClickListener? = null
    private var lastClickMillis = 0L

    init {
        loadAttributes(context, attrs)
        initGlobalViews(context)
        initMainViews(context)
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?) {
        padding5 = ScreenUtils.dp2PxInt(context, 5f)
        padding12 = ScreenUtils.dp2PxInt(context, 12f)

        val array = context.obtainStyledAttributes(attrs, R.styleable.CommonTitleBar)
        try {
            openStatusBar = array.getBoolean(R.styleable.CommonTitleBar_openStatusBar, true)
            fillStatusBar = array.getBoolean(R.styleable.CommonTitleBar_fillStatusBar, true)
            titleBarColor = array.getColor(R.styleable.CommonTitleBar_titleBarColor, Color.WHITE)
            titleBarHeight = array.getDimension(
                R.styleable.CommonTitleBar_titleBarHeight,
                ScreenUtils.dp2PxInt(context, 44f).toFloat()
            ).toInt()
            statusBarColor = array.getColor(R.styleable.CommonTitleBar_statusBarColor, Color.WHITE)
            statusBarMode = array.getInt(R.styleable.CommonTitleBar_statusBarMode, 0)

            showBottomLine = array.getBoolean(R.styleable.CommonTitleBar_showBottomLine, true)
            bottomLineColor = array.getColor(R.styleable.CommonTitleBar_bottomLineColor, Color.parseColor("#dddddd"))
            bottomShadowHeight = array.getDimension(R.styleable.CommonTitleBar_bottomShadowHeight, 0f)

            leftType = array.getInt(R.styleable.CommonTitleBar_leftType, TYPE_LEFT_NONE)
            when (leftType) {
                TYPE_LEFT_TEXTVIEW -> {
                    leftText = array.getString(R.styleable.CommonTitleBar_leftText)
                    leftTextColor = array.getColor(
                        R.styleable.CommonTitleBar_leftTextColor,
                        ContextCompat.getColor(context, R.color.comm_titlebar_text_selector)
                    )
                    leftTextSize = array.getDimension(
                        R.styleable.CommonTitleBar_leftTextSize,
                        ScreenUtils.dp2PxInt(context, 15f).toFloat()
                    )
                    leftDrawable = array.getResourceId(R.styleable.CommonTitleBar_leftDrawable, 0)
                    leftDrawablePadding = array.getDimension(R.styleable.CommonTitleBar_leftDrawablePadding, 5f)
                }
                TYPE_LEFT_IMAGEBUTTON -> {
                    leftImageResource = array.getResourceId(
                        R.styleable.CommonTitleBar_leftImageResource,
                        R.drawable.comm_titlebar_reback_selector
                    )
                }
                TYPE_LEFT_CUSTOM_VIEW -> {
                    leftCustomViewRes = array.getResourceId(R.styleable.CommonTitleBar_leftCustomView, 0)
                }
            }

            rightType = array.getInt(R.styleable.CommonTitleBar_rightType, TYPE_RIGHT_NONE)
            when (rightType) {
                TYPE_RIGHT_TEXTVIEW -> {
                    rightText = array.getString(R.styleable.CommonTitleBar_rightText)
                    rightTextColor = array.getColor(
                        R.styleable.CommonTitleBar_rightTextColor,
                        ContextCompat.getColor(context, R.color.comm_titlebar_text_selector)
                    )
                    rightTextSize = array.getDimension(
                        R.styleable.CommonTitleBar_rightTextSize,
                        ScreenUtils.dp2PxInt(context, 15f).toFloat()
                    )
                }
                TYPE_RIGHT_IMAGEBUTTON -> {
                    rightImageResource = array.getResourceId(R.styleable.CommonTitleBar_rightImageResource, 0)
                }
                TYPE_RIGHT_CUSTOM_VIEW -> {
                    rightCustomViewRes = array.getResourceId(R.styleable.CommonTitleBar_rightCustomView, 0)
                }
            }

            centerType = array.getInt(R.styleable.CommonTitleBar_centerType, TYPE_CENTER_NONE)
            when (centerType) {
                TYPE_CENTER_TEXTVIEW -> {
                    centerText = array.getString(R.styleable.CommonTitleBar_centerText)
                    centerTextColor = array.getColor(R.styleable.CommonTitleBar_centerTextColor, Color.parseColor("#333333"))
                    centerTextSize = array.getDimension(
                        R.styleable.CommonTitleBar_centerTextSize,
                        ScreenUtils.dp2PxInt(context, 16f).toFloat()
                    )
                    centerTextMarquee = array.getBoolean(R.styleable.CommonTitleBar_centerTextMarquee, true)
                    centerSubText = array.getString(R.styleable.CommonTitleBar_centerSubText)
                    centerSubTextColor = array.getColor(R.styleable.CommonTitleBar_centerSubTextColor, Color.parseColor("#666666"))
                    centerSubTextSize = array.getDimension(
                        R.styleable.CommonTitleBar_centerSubTextSize,
                        ScreenUtils.dp2PxInt(context, 11f).toFloat()
                    )
                }
                TYPE_CENTER_SEARCHVIEW -> {
                    centerSearchEditable = array.getBoolean(R.styleable.CommonTitleBar_centerSearchEditable, true)
                    centerSearchBgResource = array.getResourceId(
                        R.styleable.CommonTitleBar_centerSearchBg,
                        R.drawable.comm_titlebar_search_gray_shape
                    )
                    centerSearchRightType = array.getInt(
                        R.styleable.CommonTitleBar_centerSearchRightType,
                        TYPE_CENTER_SEARCH_RIGHT_VOICE
                    )
                }
                TYPE_CENTER_CUSTOM_VIEW -> {
                    centerCustomViewRes = array.getResourceId(R.styleable.CommonTitleBar_centerCustomView, 0)
                }
            }
        } finally {
            array.recycle()
        }
    }

    private fun initGlobalViews(context: Context) {
        rlMain?.removeAllViews()
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        val transparentStatusBar = StatusBarUtils.supportTransparentStatusBar()
        val needsStatusFill = fillStatusBar && transparentStatusBar && openStatusBar

        if (needsStatusFill) {
            val statusBarHeight = StatusBarUtils.getStatusBarHeight(context)
            viewStatusBarFill = View(context).apply {
                id = StatusBarUtils.generateViewId()
                setBackgroundColor(statusBarColor)
            }
            val statusBarParams = LayoutParams(MATCH_PARENT, statusBarHeight).apply {
                addRule(ALIGN_PARENT_TOP)
            }
            addView(viewStatusBarFill, statusBarParams)
        }

        val main = RelativeLayout(context).apply {
            id = StatusBarUtils.generateViewId()
            setBackgroundColor(titleBarColor)
        }
        val mainParams = LayoutParams(MATCH_PARENT, titleBarHeight).apply {
            when {
                needsStatusFill -> addRule(BELOW, viewStatusBarFill!!.id)
                openStatusBar -> addRule(ALIGN_PARENT_TOP)
            }
            height = if (showBottomLine) {
                titleBarHeight - maxOf(1, ScreenUtils.dp2PxInt(context, 0.4f))
            } else {
                titleBarHeight
            }
        }
        addView(main, mainParams)
        rlMain = main

        if (showBottomLine) {
            viewBottomLine = View(context).apply { setBackgroundColor(bottomLineColor) }
            val bottomLineParams = LayoutParams(MATCH_PARENT, maxOf(1, ScreenUtils.dp2PxInt(context, 0.4f))).apply {
                addRule(BELOW, main.id)
            }
            addView(viewBottomLine, bottomLineParams)
        } else if (bottomShadowHeight != 0f) {
            viewBottomShadow = View(context).apply {
                setBackgroundResource(R.drawable.comm_titlebar_bottom_shadow)
            }
            // bottomShadowHeight comes from getDimension() — already in px.
            val bottomShadowParams = LayoutParams(MATCH_PARENT, bottomShadowHeight.toInt()).apply {
                addRule(BELOW, main.id)
            }
            addView(viewBottomShadow, bottomShadowParams)
        }
    }

    private fun initMainViews(context: Context) {
        if (leftType != TYPE_LEFT_NONE) initMainLeftViews(context)
        if (rightType != TYPE_RIGHT_NONE) initMainRightViews(context)
        if (centerType != TYPE_CENTER_NONE) initMainCenterViews(context)
    }

    private fun initMainLeftViews(context: Context) {
        val main = rlMain ?: return
        val leftInnerParams = LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
            addRule(ALIGN_PARENT_START)
            addRule(CENTER_VERTICAL)
        }
        when (leftType) {
            TYPE_LEFT_TEXTVIEW -> {
                tvLeft = TextView(context).apply {
                    id = StatusBarUtils.generateViewId()
                    text = leftText
                    setTextColor(leftTextColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, leftTextSize)
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    isSingleLine = true
                    setOnClickListener(this@CommonTitleBar)
                    if (leftDrawable != 0) {
                        compoundDrawablePadding = leftDrawablePadding.toInt()
                        setCompoundDrawablesRelativeWithIntrinsicBounds(leftDrawable, 0, 0, 0)
                    }
                    setPadding(padding12, 0, padding12, 0)
                }
                main.addView(tvLeft, leftInnerParams)
            }
            TYPE_LEFT_IMAGEBUTTON -> {
                btnLeft = ImageButton(context).apply {
                    id = StatusBarUtils.generateViewId()
                    setBackgroundColor(Color.TRANSPARENT)
                    setImageResource(leftImageResource)
                    setPadding(padding12, 0, padding12, 0)
                    setOnClickListener(this@CommonTitleBar)
                }
                main.addView(btnLeft, leftInnerParams)
            }
            TYPE_LEFT_CUSTOM_VIEW -> {
                if (leftCustomViewRes == 0) return
                val view = LayoutInflater.from(context).inflate(leftCustomViewRes, main, false).apply {
                    if (id == NO_ID) id = StatusBarUtils.generateViewId()
                }
                viewCustomLeft = view
                main.addView(view, leftInnerParams)
            }
        }
    }

    private fun initMainRightViews(context: Context) {
        val main = rlMain ?: return
        val rightInnerParams = LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
            addRule(ALIGN_PARENT_END)
            addRule(CENTER_VERTICAL)
        }
        when (rightType) {
            TYPE_RIGHT_TEXTVIEW -> {
                tvRight = TextView(context).apply {
                    id = StatusBarUtils.generateViewId()
                    text = rightText
                    setTextColor(rightTextColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, rightTextSize)
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    isSingleLine = true
                    setPadding(padding12, 0, padding12, 0)
                    setOnClickListener(this@CommonTitleBar)
                }
                main.addView(tvRight, rightInnerParams)
            }
            TYPE_RIGHT_IMAGEBUTTON -> {
                btnRight = ImageButton(context).apply {
                    id = StatusBarUtils.generateViewId()
                    setImageResource(rightImageResource)
                    setBackgroundColor(Color.TRANSPARENT)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(padding12, 0, padding12, 0)
                    setOnClickListener(this@CommonTitleBar)
                }
                main.addView(btnRight, rightInnerParams)
            }
            TYPE_RIGHT_CUSTOM_VIEW -> {
                if (rightCustomViewRes == 0) return
                val view = LayoutInflater.from(context).inflate(rightCustomViewRes, main, false).apply {
                    if (id == NO_ID) id = StatusBarUtils.generateViewId()
                }
                viewCustomRight = view
                main.addView(view, rightInnerParams)
            }
        }
    }

    private fun initMainCenterViews(context: Context) {
        val main = rlMain ?: return
        when (centerType) {
            TYPE_CENTER_TEXTVIEW -> {
                val container = LinearLayout(context).apply {
                    id = StatusBarUtils.generateViewId()
                    gravity = Gravity.CENTER
                    orientation = LinearLayout.VERTICAL
                    setOnClickListener(this@CommonTitleBar)
                }
                llMainCenter = container
                val centerParams = LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
                    marginStart = padding12
                    marginEnd = padding12
                    addRule(CENTER_IN_PARENT)
                }
                main.addView(container, centerParams)

                tvCenter = TextView(context).apply {
                    text = centerText
                    setTextColor(centerTextColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, centerTextSize)
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    maxWidth = (ScreenUtils.getScreenPixelSize(context)[0] * 3 / 5.0).toInt()
                    if (centerTextMarquee) {
                        ellipsize = TextUtils.TruncateAt.MARQUEE
                        marqueeRepeatLimit = -1
                        requestFocus()
                        isSelected = true
                    }
                }
                container.addView(tvCenter, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))

                val progressWidth = ScreenUtils.dp2PxInt(context, 18f)
                progressCenter = ProgressBar(context).apply {
                    indeterminateDrawable = ContextCompat.getDrawable(context, R.drawable.comm_titlebar_progress_draw)
                    visibility = View.GONE
                }
                val progressParams = LayoutParams(progressWidth, progressWidth).apply {
                    addRule(CENTER_VERTICAL)
                    addRule(START_OF, container.id)
                }
                main.addView(progressCenter, progressParams)

                tvCenterSub = TextView(context).apply {
                    text = centerSubText
                    setTextColor(centerSubTextColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, centerSubTextSize)
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    if (centerSubText.isNullOrEmpty()) visibility = View.GONE
                }
                container.addView(tvCenterSub, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            }

            TYPE_CENTER_SEARCHVIEW -> {
                val searchContainer = RelativeLayout(context).apply {
                    setBackgroundResource(centerSearchBgResource)
                }
                rlMainCenterSearch = searchContainer
                val centerParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    topMargin = ScreenUtils.dp2PxInt(context, 7f)
                    bottomMargin = ScreenUtils.dp2PxInt(context, 7f)
                    when (leftType) {
                        TYPE_LEFT_TEXTVIEW -> {
                            addRule(END_OF, tvLeft!!.id); marginStart = padding5
                        }
                        TYPE_LEFT_IMAGEBUTTON -> {
                            addRule(END_OF, btnLeft!!.id); marginStart = padding5
                        }
                        TYPE_LEFT_CUSTOM_VIEW -> {
                            viewCustomLeft?.id?.let { addRule(END_OF, it); marginStart = padding5 }
                                ?: run { marginStart = padding12 }
                        }
                        else -> marginStart = padding12
                    }
                    when (rightType) {
                        TYPE_RIGHT_TEXTVIEW -> {
                            addRule(START_OF, tvRight!!.id); marginEnd = padding5
                        }
                        TYPE_RIGHT_IMAGEBUTTON -> {
                            addRule(START_OF, btnRight!!.id); marginEnd = padding5
                        }
                        TYPE_RIGHT_CUSTOM_VIEW -> {
                            viewCustomRight?.id?.let { addRule(START_OF, it); marginEnd = padding5 }
                                ?: run { marginEnd = padding12 }
                        }
                        else -> marginEnd = padding12
                    }
                }
                main.addView(searchContainer, centerParams)

                val searchIconWidth = ScreenUtils.dp2PxInt(context, 15f)
                ivSearch = ImageView(context).apply {
                    id = StatusBarUtils.generateViewId()
                    setOnClickListener(this@CommonTitleBar)
                    setImageResource(R.mipmap.comm_titlebar_search_normal)
                }
                val searchParams = LayoutParams(searchIconWidth, searchIconWidth).apply {
                    addRule(CENTER_VERTICAL)
                    addRule(ALIGN_PARENT_START)
                    marginStart = padding12
                }
                searchContainer.addView(ivSearch, searchParams)

                ivVoice = ImageView(context).apply {
                    id = StatusBarUtils.generateViewId()
                    setOnClickListener(this@CommonTitleBar)
                    if (centerSearchRightType == TYPE_CENTER_SEARCH_RIGHT_VOICE) {
                        setImageResource(R.mipmap.comm_titlebar_voice)
                    } else {
                        setImageResource(R.mipmap.comm_titlebar_delete_normal)
                        visibility = View.GONE
                    }
                }
                val voiceParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    addRule(CENTER_VERTICAL)
                    addRule(ALIGN_PARENT_END)
                    marginEnd = padding12
                }
                searchContainer.addView(ivVoice, voiceParams)

                etSearchHint = EditText(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    hint = resources.getString(R.string.titlebar_search_hint)
                    setTextColor(Color.parseColor("#666666"))
                    setHintTextColor(Color.parseColor("#999999"))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, ScreenUtils.dp2PxInt(context, 14f).toFloat())
                    setPadding(padding5, 0, padding5, 0)
                    if (!centerSearchEditable) {
                        isCursorVisible = false
                        clearFocus()
                        isFocusable = false
                        setOnClickListener(this@CommonTitleBar)
                    } else {
                        setOnClickListener { isCursorVisible = true }
                    }
                    isCursorVisible = false
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    imeOptions = EditorInfo.IME_ACTION_SEARCH
                    addTextChangedListener(centerSearchWatcher)
                    onFocusChangeListener = focusChangeListener
                    setOnEditorActionListener(editorActionListener)
                }
                val searchHintParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    addRule(END_OF, ivSearch!!.id)
                    addRule(START_OF, ivVoice!!.id)
                    addRule(CENTER_VERTICAL)
                    marginStart = padding5
                    marginEnd = padding5
                }
                searchContainer.addView(etSearchHint, searchHintParams)
            }

            TYPE_CENTER_CUSTOM_VIEW -> {
                if (centerCustomViewRes == 0) return
                val view = LayoutInflater.from(context).inflate(centerCustomViewRes, main, false).apply {
                    if (id == NO_ID) id = StatusBarUtils.generateViewId()
                }
                centerCustomView = view
                val centerCustomParams = LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
                    marginStart = padding12
                    marginEnd = padding12
                    addRule(CENTER_IN_PARENT)
                }
                main.addView(view, centerCustomParams)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (openStatusBar) setUpImmersionTitleBar()
    }

    private fun setUpImmersionTitleBar() {
        val window = findWindow() ?: return
        StatusBarUtils.transparentStatusBar(window)
        if (statusBarMode == 0) {
            StatusBarUtils.setDarkMode(window)
        } else {
            StatusBarUtils.setLightMode(window)
        }
    }

    private fun findWindow(): Window? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx.window
            ctx = ctx.baseContext
        }
        return null
    }

    private val centerSearchWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            val voice = ivVoice ?: return
            val empty = s.isNullOrEmpty()
            if (centerSearchRightType == TYPE_CENTER_SEARCH_RIGHT_VOICE) {
                voice.setImageResource(
                    if (empty) R.mipmap.comm_titlebar_voice
                    else R.mipmap.comm_titlebar_delete_normal
                )
            } else {
                voice.visibility = if (empty) View.GONE else View.VISIBLE
            }
        }
    }

    private val focusChangeListener = OnFocusChangeListener { _, hasFocus ->
        if (centerSearchRightType != TYPE_CENTER_SEARCH_RIGHT_DELETE) return@OnFocusChangeListener
        val voice = ivVoice ?: return@OnFocusChangeListener
        val input = etSearchHint?.text?.toString().orEmpty()
        voice.visibility = if (hasFocus && input.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private val editorActionListener = TextView.OnEditorActionListener { v, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            listener?.onClicked(v, ACTION_SEARCH_SUBMIT, etSearchHint?.text?.toString().orEmpty())
        }
        false
    }

    override fun onClick(v: View) {
        val cb = listener ?: return
        when {
            v === llMainCenter -> {
                val dbl = doubleClickListener ?: return
                val now = System.currentTimeMillis()
                if (now - lastClickMillis < 500) dbl.onClicked(v)
                lastClickMillis = now
            }
            v === tvLeft -> cb.onClicked(v, ACTION_LEFT_TEXT, null)
            v === btnLeft -> cb.onClicked(v, ACTION_LEFT_BUTTON, null)
            v === tvRight -> cb.onClicked(v, ACTION_RIGHT_TEXT, null)
            v === btnRight -> cb.onClicked(v, ACTION_RIGHT_BUTTON, null)
            v === etSearchHint || v === ivSearch -> cb.onClicked(v, ACTION_SEARCH, null)
            v === ivVoice -> {
                val edit = etSearchHint
                if (centerSearchRightType == TYPE_CENTER_SEARCH_RIGHT_VOICE && edit?.text.isNullOrEmpty()) {
                    cb.onClicked(v, ACTION_SEARCH_VOICE, null)
                } else {
                    edit?.setText("")
                    cb.onClicked(v, ACTION_SEARCH_DELETE, null)
                }
            }
            v === tvCenter -> cb.onClicked(v, ACTION_CENTER_TEXT, null)
        }
    }

    override fun setBackgroundColor(color: Int) {
        viewStatusBarFill?.setBackgroundColor(color)
        // rlMain may still be null during super constructor's attribute pass.
        rlMain?.setBackgroundColor(color)
    }

    fun setOpenStatusBar(openStatusBar: Boolean) {
        this.openStatusBar = openStatusBar
        if (!openStatusBar) viewStatusBarFill?.let { removeView(it) }
    }

    override fun setBackgroundResource(resource: Int) {
        setBackgroundColor(Color.TRANSPARENT)
        super.setBackgroundResource(resource)
    }

    fun setStatusBarColor(@ColorInt color: Int) {
        viewStatusBarFill?.setBackgroundColor(color)
    }

    fun showStatusBar(show: Boolean) {
        viewStatusBarFill?.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun toggleStatusBarMode() {
        val window = findWindow() ?: return
        StatusBarUtils.transparentStatusBar(window)
        if (statusBarMode == 0) {
            statusBarMode = 1
            StatusBarUtils.setLightMode(window)
        } else {
            statusBarMode = 0
            StatusBarUtils.setDarkMode(window)
        }
    }

    fun getMainView(): View? = rlMain
    fun getBottomLine(): View? = viewBottomLine
    fun getLeftTextView(): TextView? = tvLeft
    fun getLeftImageButton(): ImageButton? = btnLeft
    fun getRightTextView(): TextView? = tvRight
    fun getRightImageButton(): ImageButton? = btnRight
    fun getCenterLayout(): LinearLayout? = llMainCenter
    fun getCenterTextView(): TextView? = tvCenter
    fun getCenterSubTextView(): TextView? = tvCenterSub
    fun getCenterSearchView(): RelativeLayout? = rlMainCenterSearch
    fun getCenterSearchEditText(): EditText? = etSearchHint
    fun getCenterSearchRightImageView(): ImageView? = ivVoice
    fun getCenterSearchLeftImageView(): ImageView? = ivSearch
    fun getLeftCustomView(): View? = viewCustomLeft
    fun getRightCustomView(): View? = viewCustomRight
    fun getCenterCustomView(): View? = centerCustomView

    fun setLeftView(leftView: View) {
        val main = rlMain ?: return
        removeExistingLeftView(main)
        if (leftView.id == NO_ID) leftView.id = StatusBarUtils.generateViewId()
        val params = LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
            addRule(ALIGN_PARENT_START)
            addRule(CENTER_VERTICAL)
        }
        main.addView(leftView, params)
        viewCustomLeft = leftView
        leftType = TYPE_LEFT_CUSTOM_VIEW
    }

    fun setCenterView(centerView: View) {
        val main = rlMain ?: return
        removeExistingCenterView(main)
        if (centerView.id == NO_ID) centerView.id = StatusBarUtils.generateViewId()
        val params = LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
            addRule(CENTER_IN_PARENT)
            addRule(CENTER_VERTICAL)
        }
        main.addView(centerView, params)
        centerCustomView = centerView
        centerType = TYPE_CENTER_CUSTOM_VIEW
    }

    fun setRightView(rightView: View) {
        val main = rlMain ?: return
        removeExistingRightView(main)
        if (rightView.id == NO_ID) rightView.id = StatusBarUtils.generateViewId()
        val params = LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
            addRule(ALIGN_PARENT_END)
            addRule(CENTER_VERTICAL)
        }
        main.addView(rightView, params)
        viewCustomRight = rightView
        rightType = TYPE_RIGHT_CUSTOM_VIEW
    }

    fun setCenterText(textString: CharSequence?) {
        tvCenter?.text = textString
    }

    fun setLeftVisibility(visibleType: Int) {
        when (leftType) {
            TYPE_LEFT_TEXTVIEW -> tvLeft?.visibility = visibleType
            TYPE_LEFT_IMAGEBUTTON -> btnLeft?.visibility = visibleType
            TYPE_LEFT_CUSTOM_VIEW -> viewCustomLeft?.visibility = visibleType
        }
    }

    fun setRightIcon(@DrawableRes drawableRes: Int) {
        val main = rlMain ?: return
        removeExistingRightView(main)
        rightType = TYPE_RIGHT_IMAGEBUTTON
        rightImageResource = drawableRes
        initMainRightViews(context)
    }

    fun setRightText(text: String?, textColor: Int, textSize: Int) {
        val main = rlMain ?: return
        removeExistingRightView(main)
        rightType = TYPE_RIGHT_TEXTVIEW
        rightText = text
        rightTextColor = textColor
        rightTextSize = textSize.toFloat()
        initMainRightViews(context)
    }

    fun setLeftIcon(@DrawableRes drawableRes: Int) {
        val main = rlMain ?: return
        removeExistingLeftView(main)
        leftType = TYPE_LEFT_IMAGEBUTTON
        leftImageResource = drawableRes
        initMainLeftViews(context)
    }

    private fun removeExistingLeftView(main: RelativeLayout) {
        btnLeft?.let { main.removeView(it); btnLeft = null }
        tvLeft?.let { main.removeView(it); tvLeft = null }
        viewCustomLeft?.let { main.removeView(it); viewCustomLeft = null }
    }

    private fun removeExistingRightView(main: RelativeLayout) {
        btnRight?.let { main.removeView(it); btnRight = null }
        tvRight?.let { main.removeView(it); tvRight = null }
        viewCustomRight?.let { main.removeView(it); viewCustomRight = null }
    }

    private fun removeExistingCenterView(main: RelativeLayout) {
        llMainCenter?.let { main.removeView(it); llMainCenter = null }
        rlMainCenterSearch?.let { main.removeView(it); rlMainCenterSearch = null }
        centerCustomView?.let { main.removeView(it); centerCustomView = null }
        progressCenter?.let { main.removeView(it); progressCenter = null }
    }

    fun showCenterProgress() {
        progressCenter?.visibility = View.VISIBLE
    }

    fun dismissCenterProgress() {
        progressCenter?.visibility = View.GONE
    }

    fun showSoftInputKeyboard(show: Boolean) {
        val edit = etSearchHint ?: return
        if (centerSearchEditable && show) {
            edit.isFocusable = true
            edit.isFocusableInTouchMode = true
            edit.requestFocus()
            ScreenUtils.showSoftInputKeyBoard(context, edit)
        } else {
            ScreenUtils.hideSoftInputKeyBoard(context, edit)
        }
    }

    fun setSearchRightImageResource(res: Int) {
        ivVoice?.setImageResource(res)
    }

    fun getSearchKey(): String = etSearchHint?.text?.toString().orEmpty()

    fun setListener(listener: OnTitleBarListener?) {
        this.listener = listener
    }

    fun setDoubleClickListener(doubleClickListener: OnTitleBarDoubleClickListener?) {
        this.doubleClickListener = doubleClickListener
    }

    fun interface OnTitleBarListener {
        fun onClicked(v: View, action: Int, extra: String?)
    }

    fun interface OnTitleBarDoubleClickListener {
        fun onClicked(v: View)
    }

    companion object {
        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT

        private const val TYPE_LEFT_NONE = 0
        private const val TYPE_LEFT_TEXTVIEW = 1
        private const val TYPE_LEFT_IMAGEBUTTON = 2
        private const val TYPE_LEFT_CUSTOM_VIEW = 3
        private const val TYPE_RIGHT_NONE = 0
        private const val TYPE_RIGHT_TEXTVIEW = 1
        private const val TYPE_RIGHT_IMAGEBUTTON = 2
        private const val TYPE_RIGHT_CUSTOM_VIEW = 3
        private const val TYPE_CENTER_NONE = 0
        private const val TYPE_CENTER_TEXTVIEW = 1
        private const val TYPE_CENTER_SEARCHVIEW = 2
        private const val TYPE_CENTER_CUSTOM_VIEW = 3
        private const val TYPE_CENTER_SEARCH_RIGHT_VOICE = 0
        private const val TYPE_CENTER_SEARCH_RIGHT_DELETE = 1

        const val ACTION_LEFT_TEXT = 1
        const val ACTION_LEFT_BUTTON = 2
        const val ACTION_RIGHT_TEXT = 3
        const val ACTION_RIGHT_BUTTON = 4
        const val ACTION_SEARCH = 5
        const val ACTION_SEARCH_SUBMIT = 6
        const val ACTION_SEARCH_VOICE = 7
        const val ACTION_SEARCH_DELETE = 8
        const val ACTION_CENTER_TEXT = 9
    }
}
