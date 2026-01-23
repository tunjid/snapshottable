// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// ENABLE_JAVA_SETTERS

package diagnostics

import com.tunjid.snapshottable.Snapshottable


@Snapshottable
public interface Person {
    @Deprecated("Hi")
    @Snapshottable.Spec
    public class Immutable(
        public val name: String,
        public val nickname: String? = name,
        public val age: Int = 0,
    ) : Person
}

fun person1(): Person.Mutable {
    return Person.Mutable(
        name = "John",
        nickname = null,
        age = 9,
    )
        .update(
            name = "John",
            nickname = null,
            age = 9,
        )
}

fun person2(): Person.Mutable {
    return Person.Mutable(
        name = "John",
        nickname = null,
        age = 9,
    )
        .update(
            name = "John",
        )
}
