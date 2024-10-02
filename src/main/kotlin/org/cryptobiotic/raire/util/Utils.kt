package org.cryptobiotic.raire.util


inline fun <reified T> toArray(list: List<*>): Array<T> {
    return (list as List<T>).toTypedArray()
}

inline fun toIntArray(list: List<Int>): IntArray {
    return IntArray(list.size) { list[it] }
}