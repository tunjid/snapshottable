package org.jetbrains.kotlin.compiler.plugin.template

import com.tunjid.snapshottable.Snapshottable
import java.util.*
import org.jetbrains.kotlin.compiler.plugin.template.Person.Companion.toSnapshotSpec
import org.jetbrains.kotlin.compiler.plugin.template.Person.Companion.toSnapshotMutable

// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Hello and welcome!")

        val person = person()
        println(person.toSnapshotSpec())

        person.nickname = "Pt"
        println(person.toSnapshotSpec())

        person.update(nickname = "ppppp", age = 8)
        println(person.toSnapshotSpec())

        person.update(age = 14)
        println(person.toSnapshotSpec())

        person.update(date = Date().time)
        println(person.toSnapshotSpec())

        person.update(progress = 0.9f)
        println(person.toSnapshotSpec())

        println(person.toSnapshotSpec().toSnapshotMutable())
        println(person.toSnapshotSpec().toSnapshotMutable().toSnapshotSpec())
    }
}

@Snapshottable
interface Person {
    @Snapshottable.Spec
    data class Immutable(
        val nickname: String,
        val age: Int,
        val date: Long,
        val progress: Float,
    ) : Person
}

fun person(): Person.SnapshotMutable {
    return Person.SnapshotMutable(
        nickname = "John",
        age = 7,
        date = Date().time,
        progress = 0.8f,
    )
}
