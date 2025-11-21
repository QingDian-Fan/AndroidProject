package com.dian.demo.skin

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.SkinAppCompatDelegateImpl


open class SkinActivity: AppCompatActivity() {
    override fun getDelegate(): AppCompatDelegate {
        return SkinAppCompatDelegateImpl.get(this, this)
    }
}