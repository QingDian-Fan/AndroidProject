package com.demo.project.base

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/9 8:01 PM
 * @description: 页面的常用操作
 * @since: 1.0.0
 */
interface ViewBehavior {
    /**
     * 是否显示Loading视图
     */
    fun showLoadingView(isShow: Boolean)

    /**
     * 是否显示空白视图
     */
    fun showEmptyView(isShow: Boolean)

    /**
     * 是否显示错误视图
     */
    fun showErrorView(isShow: Boolean)

    /**
     * 弹出Toast提示
     */
    fun showToast(event: ToastEvent)

    /**
     * 不带参数的页面跳转
     */
    fun navigate(page: Any)

    /**
     * 返回键点击
     */
    fun backPress(arg: Any?)

    /**
     * 关闭页面
     */
    fun finishPage(arg: Any?)
}