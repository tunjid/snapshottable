// FIR_DUMP
// ENABLE_JAVA_SETTERS

package diagnostics

import com.tunjid.snapshottable.Snapshottable

@Snapshottable(
  source = Person::class
)
interface SnapshottablePerson

class Person(
  val name: String,
  val nickname: String? = name,
  val age: Int = 0,
)

fun person1(): SnapshottablePerson.Mutable {
  return SnapshottablePerson.Mutable()
    .setName("John")
}

fun person2(): SnapshottablePerson.Mutable {
  return SnapshottablePerson.Mutable().apply {
    name = "John"
  }
}
