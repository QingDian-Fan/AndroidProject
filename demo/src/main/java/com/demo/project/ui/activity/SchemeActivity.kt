package com.demo.project.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.project.utils.scheme.SchemeUtils

class SchemaActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        uri?.let {
            SchemeUtils.toOpenActivity(this@SchemaActivity,it)
        }
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data
        uri?.let {
            SchemeUtils.toOpenActivity(this@SchemaActivity,it)
        }
        finish()
    }
}