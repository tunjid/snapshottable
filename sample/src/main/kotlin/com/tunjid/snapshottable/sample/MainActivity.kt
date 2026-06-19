package com.tunjid.snapshottable.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.Channel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialMode = when (intent.getStringExtra("MODE")) {
            "Immutable" -> Mode.Immutable
            else -> Mode.SnapshotMutable
        }

        setContent {
            val repository = remember { StockRepository() }
            val actions = remember { Channel<Action>(Channel.UNLIMITED) }
            var mode by remember { mutableStateOf(initialMode) }

            val state = produceState(
                mode = mode,
                repository = repository,
                actions = actions,
            )

            Screen(
                state = state,
                mode = mode,
                toggleSector = { actions.trySend(Action.ToggleSector(it)) },
                toggleMode = {
                    mode = when (mode) {
                        Mode.SnapshotMutable -> Mode.Immutable
                        else -> Mode.SnapshotMutable
                    }
                },
            )
        }
    }
}
