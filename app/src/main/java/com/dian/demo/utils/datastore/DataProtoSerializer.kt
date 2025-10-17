package com.dian.demo.utils.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.dian.demo.DataProtoOuterClass.DataProto
import java.io.InputStream
import java.io.OutputStream

object DataProtoSerializer : Serializer<DataProto> {

    override val defaultValue: DataProto = DataProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): DataProto {
        try {
            return DataProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: DataProto, output: OutputStream) = t.writeTo(output)
}

