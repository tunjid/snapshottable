package org.jetbrains.kotlin.compiler.plugin.template

import com.tunjid.snapshottable.Snapshottable

// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.println("Hello and welcome!")

//        println(person().toString())
        val person = person()
        println("nickname: ${person.nickname}; age: ${person.age}")
        person.nickname = "Pt"
        println("nickname: ${person.nickname}; age: ${person.age}")
        person.update(nickname = "ppppp", age = 8)
        println("nickname: ${person.nickname}; age: ${person.age}")

    }
}

@Snapshottable.Parent
public interface Person {


    @Snapshottable
    data class Immutable(
        val nickname: String,
        val age: Int,
    ) : Person
}

fun person(): Person.Mutable {
    return Person.Mutable(
        nickname = "John",
        age = 7,
    )
}
