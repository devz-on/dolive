package com.example.screencaster.domain

class SessionStateMachine(initial: StreamState = StreamState.IDLE) {
    var state: StreamState = initial
        private set

    fun transitionTo(next: StreamState): Boolean {
        val allowed = when (state) {
            StreamState.IDLE -> setOf(StreamState.PREPARING, StreamState.WAITING_FOR_PROJECTION_PERMISSION)
            StreamState.PREPARING -> setOf(StreamState.WAITING_FOR_PROJECTION_PERMISSION, StreamState.CONNECTING, StreamState.ERROR, StreamState.STOPPING)
            StreamState.WAITING_FOR_PROJECTION_PERMISSION -> setOf(StreamState.CONNECTING, StreamState.STOPPED, StreamState.ERROR)
            StreamState.CONNECTING -> setOf(StreamState.STARTING_ENCODER, StreamState.RECONNECTING, StreamState.ERROR, StreamState.STOPPING)
            StreamState.STARTING_ENCODER -> setOf(StreamState.LIVE, StreamState.ERROR, StreamState.STOPPING)
            StreamState.LIVE -> setOf(StreamState.RECONNECTING, StreamState.STOPPING, StreamState.ERROR)
            StreamState.RECONNECTING -> setOf(StreamState.CONNECTING, StreamState.STOPPING, StreamState.ERROR)
            StreamState.STOPPING -> setOf(StreamState.STOPPED, StreamState.ERROR)
            StreamState.STOPPED -> setOf(StreamState.IDLE, StreamState.PREPARING)
            StreamState.ERROR -> setOf(StreamState.IDLE, StreamState.STOPPING)
        }
        if (next !in allowed) return false
        state = next
        return true
    }
}
