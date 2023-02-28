package com.dian.demo.utils.datastore

import android.text.TextUtils
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dian.demo.DataProtoOuterClass
import com.dian.demo.DataProtoOuterClass.DataProto
import com.dian.demo.ProjectApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


object AppDataStore {


    // 创建DataStore
    private val ProjectApplication.appDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "App"
    )

    // DataStore变量
    private val dataStore = ProjectApplication.getAppInstance().appDataStore

    fun <T> putData(key: String, value: T) {
        dataStore.putData(key, value)
    }

    fun <T> getData(key: String, value: T): T {
        return dataStore.getData(key, value)
    }

    fun clear() {
        dataStore.clear()
    }


    /**
     * 创建 ProtoBufDataStore  案例待测试
     */
    private val ProjectApplication.protoDataStore: DataStore<DataProto> by dataStore(
        fileName = "data.pb",
        serializer = DataProtoSerializer
    )

    private val pbDataStore = ProjectApplication.getAppInstance().protoDataStore


    fun getProtoData(): DataProto = runBlocking {
        pbDataStore.data.first()
    }

    fun putProtoData(name: String? = null, age: Int? = null, course: String? = null, courses: List<String>? = null) {
        runBlocking {
            pbDataStore.updateData {
                val protoBuilder = it.toBuilder()
                if (name != null && !TextUtils.isEmpty(name)) protoBuilder.name = name
                if (age != null) protoBuilder.age = age
                if (course != null && TextUtils.isEmpty(course)) protoBuilder.addCourse(course)
                if (courses != null && courses.isNotEmpty()) protoBuilder.addAllCourse(courses)
                protoBuilder.build()
            }
        }
    }

    enum class ProtoType {
        NAME, AGE, COURSE
    }

    fun clearProtoData(vararg types: ProtoType) {
        runBlocking {
            pbDataStore.updateData {
                val protoBuilder = it.toBuilder()
                types.forEach { type ->
                    when (type) {
                        ProtoType.NAME -> {
                            protoBuilder.clearName()
                        }
                        ProtoType.AGE -> {
                            protoBuilder.clearAge()
                        }
                        ProtoType.COURSE -> {
                            protoBuilder.clearCourse()
                        }
                    }
                }
                protoBuilder.build()
            }
        }


    }


}