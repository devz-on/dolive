package com.example.screencaster.domain
import org.junit.Assert.*
import org.junit.Test
class DomainTest {
 @Test fun validatesRtmpUrls(){ assertTrue(StreamUrlValidator.validate("rtmps://a.rtmps.youtube.com/live2").isSuccess); assertTrue(StreamUrlValidator.validate("https://example.com").isFailure) }
 @Test fun formatsBitrate(){ assertEquals("4.0 Mbps", BitrateFormatter.mbps(4_000_000)); assertEquals("128 Kbps", BitrateFormatter.kbps(128_000)) }
 @Test fun backoffIsBounded(){ val b=ReconnectBackoff(); assertEquals(500,b.delayForAttempt(1)); assertTrue(b.delayForAttempt(20)<=30_000) }
 @Test fun redactsSecrets(){ val out=SecretRedactor.redact("rtmps://host/live2/abcd?stream_key=secret"); assertFalse(out.contains("secret")); assertFalse(out.contains("abcd")) }
}
