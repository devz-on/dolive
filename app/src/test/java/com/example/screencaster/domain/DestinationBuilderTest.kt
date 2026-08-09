package com.example.screencaster.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DestinationBuilderTest {
    @Test fun joinsServerAndKey() {
        assertEquals("rtmps://a.rtmps.youtube.com/live2/abc", DestinationBuilder.publishUrl("rtmps://a.rtmps.youtube.com/live2/", " abc "))
    }

    @Test fun rejectsMissingKey() {
        assertThrows(IllegalArgumentException::class.java) { DestinationBuilder.publishUrl("rtmps://host/live", "") }
    }
}
