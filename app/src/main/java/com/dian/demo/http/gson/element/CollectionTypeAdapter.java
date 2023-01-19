package com.dian.demo.http.gson.element;



import com.dian.demo.http.gson.GsonFactory;
import com.dian.demo.http.gson.JsonCallback;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;


public class CollectionTypeAdapter<E> extends TypeAdapter<Collection<E>> {

    private final TypeAdapter<E> mElementTypeAdapter;
    private final ObjectConstructor<? extends Collection<E>> mObjectConstructor;

    private TypeToken<?> mTypeToken;
    private String mFieldName;

    public CollectionTypeAdapter(Gson gson, Type elementType, TypeAdapter<E> elementTypeAdapter, ObjectConstructor<? extends Collection<E>> constructor) {
        mElementTypeAdapter = new TypeAdapterRuntimeTypeWrapper<>(gson, elementTypeAdapter, elementType);
        mObjectConstructor = constructor;
    }

    public void setReflectiveType(TypeToken<?> typeToken, String fieldName) {
        mTypeToken = typeToken;
        mFieldName = fieldName;
    }

    @Override
    public Collection<E> read(JsonReader in) throws IOException {
        JsonToken jsonToken = in.peek();

        if (jsonToken == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        if (jsonToken != JsonToken.BEGIN_ARRAY) {
            in.skipValue();
            JsonCallback callback = GsonFactory.getJsonCallback();
            if (callback != null) {
                callback.onTypeException(mTypeToken, mFieldName, jsonToken);
            }
            return null;
        }

        Collection<E> collection = mObjectConstructor.construct();
        in.beginArray();
        while (in.hasNext()) {
            E instance = mElementTypeAdapter.read(in);
            collection.add(instance);
        }
        in.endArray();
        return collection;
    }

    @Override
    public void write(JsonWriter out, Collection<E> collection) throws IOException {
        if (collection == null) {
            out.nullValue();
            return;
        }

        out.beginArray();
        for (E element : collection) {
            mElementTypeAdapter.write(out, element);
        }
        out.endArray();
    }
}