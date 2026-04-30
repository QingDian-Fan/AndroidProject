package com.common.utils.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.common.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


object AppDataStore {
    private lateinit var dataStore: DataStore<Preferences>

    fun init(context: Context = Utils.getAppContext()) {
        if (::dataStore.isInitialized) return

        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                context.applicationContext
                    .preferencesDataStoreFile("App")
            }
        )
    }

    private fun checkInit() {
        if (!::dataStore.isInitialized) {
            init()
        }
    }

    fun <T> putData(key: String, value: T) {
        checkInit()
        dataStore.putData(key, value)
    }

    fun <T> getData(key: String, value: T): T {
        checkInit()
        return dataStore.getData(key, value)
    }

    fun clear() {
        checkInit()
        dataStore.clear()
    }

    fun clearKey(key: String) {
        checkInit()
        dataStore.clearKey(key)
    }


    /*
        */
    /**
     * 创建 ProtoBufDataStore  案例待测试
     *//*

    private val BaseApplication.protoDataStore: DataStore<DataProto> by dataStore(
        fileName = "data.pb",
        serializer = DataProtoSerializer
    )

    private val pbDataStore = BaseApplication.getAppInstance().protoDataStore


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
*/


}