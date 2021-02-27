package com.github.chrisgleissner.sandbox.kotlin

data class Person(val name: String) {
    val likedPeople = mutableListOf<Person>()
    infix fun likes(p : Person) { likedPeople.add(p)}
}