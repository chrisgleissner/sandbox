package com.github.chrisgleissner.sandbox.kotlin.core

enum class Month(val englishName : String) {
    JANUARY("january"),
    FEBRUARY("february");

    override fun toString(): String {
        return englishName
    }
}