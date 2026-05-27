package com.common.ui.skin

import androidx.databinding.ViewDataBinding
import com.common.ui.BaseAppVMActivity
import com.common.ui.BaseViewModel

abstract class BaseSkinVMActivity<B : ViewDataBinding, VM : BaseViewModel>: BaseAppVMActivity<B , VM >() {

}