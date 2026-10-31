package com.poketch.core.data.gamification

import com.poketch.core.domain.engine.UnlockEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnlockEventDispatcher @Inject constructor() {

    private val _unlockEvents = MutableSharedFlow<UnlockEvent.NewPokemonUnlocked>(
        extraBufferCapacity = 16
    )
    val unlockEvents: SharedFlow<UnlockEvent.NewPokemonUnlocked> = _unlockEvents.asSharedFlow()

    suspend fun emitUnlockEvents(events: List<UnlockEvent.NewPokemonUnlocked>) {
        for (event in events) {
            _unlockEvents.emit(event)
        }
    }
}
