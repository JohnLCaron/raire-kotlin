package org.cryptobiotic.raire

class GenerateEqualsProblem {
    val wtf: Int = 99
    val wtf2: String = "who"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GenerateEqualsProblem

        if (wtf != other.wtf) return false
        if (wtf2 != other.wtf2) return false
        return true
    }

    override fun hashCode(): Int {
        var result = wtf.hashCode()
        result = 31 * result + wtf2.hashCode()
        return result
    }
}

class GenerateEqualsProblem2 {
    val wtf: Int = 99
    val wtf2: String = "who"
}