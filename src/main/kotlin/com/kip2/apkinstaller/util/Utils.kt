package com.kip2.apkinstaller.util

import kotlin.jvm.Throws

@Throws
fun Any.callMethod(methodName: String, vararg args: Any): Any {
    val method = this.javaClass.getMethod(methodName, *args.map { it.javaClass }.toTypedArray())
    return method.invoke(this, *args)
}

fun Any.callMethodSafe(methodName: String, vararg args: Any): Any? = runCatching {
    this.callMethod(methodName, *args)
}.getOrNull()
