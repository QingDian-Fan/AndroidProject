package com.dian.demo.repository.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "database_entity")
data class DatabaseEntity(
    @PrimaryKey(autoGenerate = true)
    val key_id: Long,
    val name: String,
)
