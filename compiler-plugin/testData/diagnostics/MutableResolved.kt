// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ENABLE_JAVA_SETTERS

package diagnostics

import com.tunjid.snapshottable.Snapshottable


@Snapshottable
public interface Person {
    public class Immutable(
        val name: String,
        val nickname: String? = name,
        val age: Int = 0,
    ): Person
}

fun person1(): Person.Mutable {
    return Person.Mutable()
        .setName("John")
}

fun person2(): Person.Mutable {
    return Person.Mutable().apply {
        name = "John"
    }
}
