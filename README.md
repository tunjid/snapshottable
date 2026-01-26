# Snapshottable

Snapshottable is a Kotlin compiler plugin that automatically generates mutable, snapshot-backed classes from immutable data definitions. It is designed to simplify state management in Jetpack Compose (and other Compose-based UI frameworks) by allowing you to define your state as clean, immutable interfaces and data classes, while automatically generating the mutable, observable counterparts needed for the UI.

## Features

*   **Automatic Generation:** Generates a `SnapshotMutable` class for your state interfaces.
*   **Compose Integration:** The generated mutable classes are backed by Compose `Snapshot` state, making them observable and thread-safe.
*   **Flexible Mutation:** Supports both direct property assignment for individual updates and a generated `update` method (similar to `data class copy`) for bulk updates.
*   **Optimized Primitives:** Automatically delegates to specialized Compose snapshot state implementations for primitives (`Int`, `Float`, `Long`, `Double`) to avoid boxing overhead.
*   **Two-Way Conversion:** Easily convert between your immutable "Spec" and the mutable "Snapshot" representation.
*   **Serialization Support:** The separation of concerns allows you to mark the immutable "Spec" class as `@Serializable` or `@Parcelize`. This enables seamless integration with `rememberSaveable`, allowing you to persist state across configuration changes using the serializable spec, while using the mutable version for runtime updates.
*   **Boilerplate Reduction:** Eliminates the need to manually write mutable state holders and update logic.

## Usage

1.  **Define your State Interface:**
    Annotate an interface with `@Snapshottable`. Inside, define a nested `data class` annotated with `@Snapshottable.Spec` that implements the interface. This data class represents the immutable snapshot of your state. You can also annotate it with `@Serializable` or `@Parcelize` for persistence.

    ```kotlin
    import com.tunjid.snapshottable.Snapshottable
    import kotlinx.serialization.Serializable
    import kotlinx.parcelize.Parcelize
    import android.os.Parcelable

    @Snapshottable
    interface State {
        @Serializable
        @Parcelize
        @Snapshottable.Spec
        data class Immutable(
            val count: Int = 0,
            val text: String = "Hello"
        ) : State, Parcelable
    }
    ```

2.  **Use in Composable:**
    The plugin generates a `SnapshotMutable` class nested within your interface (e.g., `State.SnapshotMutable`). You can create instances of this class, modify its properties (which updates the underlying Compose state), and convert back to the immutable spec.

    ```kotlin
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.saveable.rememberSaveable
    import androidx.compose.runtime.saveable.Saver
    import com.tunjid.snapshottable.Snapshottable
    // Import generated extension functions
    import com.example.mypackage.State.Companion.toSnapshotMutable
    import com.example.mypackage.State.Companion.toSnapshotSpec

    @Composable
    fun Counter() {
        // Use rememberSaveable to persist state across configuration changes
        // The Saver persists the Serializable 'Immutable' spec, but we work with the 'SnapshotMutable' at runtime
        val state = rememberSaveable(
            saver = Saver(
                save = { it.toSnapshotSpec() },
                restore = { it.toSnapshotMutable() }
            )
        ) { 
            State.Immutable().toSnapshotMutable() 
        }

        // Read properties (Compose will track these reads)
        Text("Count: ${state.count}")
        Text("Message: ${state.text}")

        Button(onClick = {
            // Mutate properties directly
            state.count++ 
            state.text = "Clicked!"
            
            // Or use the generated update method for bulk updates
            state.update(count = 0, text = "Reset")
        }) {
            Text("Increment")
        }
    }
    ```

3.  **Use in ViewModel:**
    You can also use the generated mutable class inside a `ViewModel` to manage state. Expose the state as the parent interface (which is read-only from the outside perspective) while mutating the internal `SnapshotMutable` instance.

    ```kotlin
    import androidx.lifecycle.ViewModel
    import com.tunjid.snapshottable.toSnapshotMutable

    class MyViewModel : ViewModel() {
        // Internal mutable state
        private val mutableState = State.Immutable().toSnapshotMutable()
        
        // Public read-only state exposed as the interface
        val state: State get() = mutableState

        fun increment() {
            mutableState.count++
        }

        fun updateText(newText: String) {
            mutableState.text = newText
        }
        
        fun reset() {
            // Bulk update
            mutableState.update(count = 0, text = "Hello")
        }
    }
    ```

    With Kotlin 2.0+ (and enabled in 2.3.0), you can use explicit backing fields for a more concise syntax:

    ```kotlin
    class MyViewModel : ViewModel() {
        val state: State
            field = State.Immutable().toSnapshotMutable()

        fun increment() {
            // Access the backing field directly to mutate
            state.field.count++
        }
    }
    ```

## Known Caveats

-   **Generics:** Generic type parameters in the `@Snapshottable` interface or `@Snapshottable.Spec` class are currently not supported.
-   **Visibility:** All properties in the `@Snapshottable.Spec` data class must be `public`. Private or internal properties
are not supported for snapshot generation as the parent interface cannot have non-public properties.

## Project Structure

This project has three modules:
- The [`:compiler-plugin`](compiler-plugin/src) module contains the compiler plugin itself.
- The [`:plugin-annotations`](plugin-annotations/src/commonMain/kotlin) module contains annotations which can be used in
user code for interacting with compiler plugin.
- The [`:gradle-plugin`](gradle-plugin/src) module contains a simple Gradle plugin to add the compiler plugin and
annotation dependency to a Kotlin project.

## Tests

The [Kotlin compiler test framework][test-framework] is set up for this project.
To create a new test, add a new `.kt` file in a [compiler-plugin/testData](compiler-plugin/testData) sub-directory:
`testData/box` for codegen tests and `testData/diagnostics` for diagnostics tests.
The generated JUnit 5 test classes will be updated automatically when tests are next run.
They can be manually updated with the `generateTests` Gradle task as well.
To aid in running tests, it is recommended to install the [Kotlin Compiler DevKit][test-plugin] IntelliJ plugin,
which is pre-configured in this repository.

[//]: # (Links)

[test-framework]: https://github.com/JetBrains/kotlin/blob/master/compiler/test-infrastructure/ReadMe.md
[test-plugin]: https://github.com/JetBrains/kotlin-compiler-devkit
