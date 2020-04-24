package com.dede.fotoapparat_extend.reflect

import android.util.ArrayMap
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Created by hsh on 2020/4/24 9:52 AM
 */
private val fieldCache by lazy(LazyThreadSafetyMode.NONE) { ArrayMap<String, Field>() }
private val methodCache by lazy(LazyThreadSafetyMode.NONE) { ArrayMap<String, Method>() }

fun Class<*>.field(name: String): Field {
    val key = this.name + "#" + name
    var field = fieldCache[key]
    if (field == null) {
        field = this.getDeclaredField(name)
        field.isAccessible = true
        fieldCache[key] = field
    }
    return field
}

fun Class<*>.method(name: String, vararg parameterTypes: Class<*>): Method {
    var s: String = ""
    for (clazz in parameterTypes) {
        s += clazz.name
    }
    val key = this.name + "#" + name + "(" + s + ")"
    var method = methodCache[key]
    if (method == null) {
        method = this.getDeclaredMethod(name, *parameterTypes)
        methodCache[key] = method
    }
    return method
}

inline fun <reified T> Any.safeFieldValue(name: String): T? {
    try {
        return this.fieldValue(name)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

@Throws(Exception::class)
inline fun <reified T> Any.fieldValue(name: String): T {
    val field = this.javaClass.field(name)
    return field.get(this) as T
}
