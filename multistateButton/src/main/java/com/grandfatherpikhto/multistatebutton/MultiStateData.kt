package com.grandfatherpikhto.multistatebutton

data class MultiStateData(val resId:Int, var enabled: Boolean = true) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiStateData

        if (resId != other.resId) return false

        return true
    }

    override fun hashCode(): Int {
        return resId
    }
}
