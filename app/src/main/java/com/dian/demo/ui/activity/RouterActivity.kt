package com.dian.demo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.dian.demo.ProjectApplication
import com.dian.demo.utils.SchemaUtil

class RouterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        val uri = intent?.dataString ?: ""
        SchemaUtil.schemaToPage(ProjectApplication.getAppContext(), uri)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.dataString ?: ""
        SchemaUtil.schemaToPage(ProjectApplication.getAppContext(), uri)
        finish()
    }
}