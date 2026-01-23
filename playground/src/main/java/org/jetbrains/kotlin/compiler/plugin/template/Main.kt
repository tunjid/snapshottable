package org.jetbrains.kotlin.compiler.plugin.template

import com.tunjid.snapshottable.Snapshottable

// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.println("Hello and welcome!")

        println(person().toString())
        println(person().name)
        person().update(name = "p")

    }
}

@Snapshottable.Parent
public interface Person {


    @Snapshottable
    data class Immutable(
        val name: String,
    ) : Person
}

fun person(): Person.Mutable {
    return Person.Mutable(
        name = "John",
    )
}
