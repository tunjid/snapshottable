// RUN_PIPELINE_TILL: FRONTEND
package diagnostics

import com.tunjid.snapshottable.Snapshottable

interface NormalInterface {
    @Snapshottable.Spec
    data class Spec(val i: Int)
}

// Case 1: @Snapshottable on a class (not interface)
@Snapshottable
class NotInterface

// Case 2: @Snapshottable interface without Spec
<!NO_SNAPSHOTTABLE_SPEC!>@Snapshottable
interface NoSpec<!>

// Case 3: @Snapshottable.Spec without primary constructor
@Snapshottable
interface NoPrimary {
    @Snapshottable.Spec
    <!NO_PRIMARY_CONSTRUCTOR!>class Spec<!> : NoPrimary {
        constructor(i: Int)
    }
}

// Case 4: @Snapshottable.Spec with private constructor
@Snapshottable
interface PrivateConstructor {
    @Snapshottable.Spec
    data class Spec <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING, PRIVATE_CONSTRUCTOR!>private<!> constructor(val i: Int) : PrivateConstructor
}

// Case 5: @Snapshottable.Spec on open/abstract class
@Snapshottable
interface OpenSpec {
    @Snapshottable.Spec
    open <!NOT_SNAPSHOTTABLE_SPEC!>class Spec<!>(val i: Int) : OpenSpec
}

// Case 6: @Snapshottable.Spec not nested in @Snapshottable interface
@Snapshottable.Spec
data class OrphanSpec(val i: Int)

@Snapshottable
interface AbstractSpec {
    @Snapshottable.Spec
    abstract <!NOT_SNAPSHOTTABLE_SPEC!>class Spec<!>(val i: Int) : AbstractSpec
}

// Case 7: @Snapshottable.Spec parameters with non-public visibility
@Snapshottable
interface VisibilityCheck {
    @Snapshottable.Spec
    data class Spec(
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>private<!> val p: Int,
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>protected<!> val pro: Int,
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>internal<!> val i: Int,
        val pub: Int
    ) : VisibilityCheck
}
