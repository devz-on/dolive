package com.example.screencaster.domain

import org.junit.Assert.*
import org.junit.Test

class SessionStateMachineTest {
    @Test fun preventsStartingTwice() {
        val sm = SessionStateMachine()
        assertTrue(sm.transitionTo(StreamState.PREPARING))
        assertFalse(sm.transitionTo(StreamState.PREPARING))
    }

    @Test fun supportsReconnectAndStop() {
        val sm = SessionStateMachine(StreamState.LIVE)
        assertTrue(sm.transitionTo(StreamState.RECONNECTING))
        assertTrue(sm.transitionTo(StreamState.STOPPING))
        assertTrue(sm.transitionTo(StreamState.STOPPED))
    }
}
