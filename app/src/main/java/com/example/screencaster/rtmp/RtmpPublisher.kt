package com.example.screencaster.rtmp

import com.example.screencaster.domain.StreamDestination
import com.example.screencaster.domain.StreamUrlValidator
import java.io.OutputStream
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

/** Minimal bounded RTMP transport foundation: validates RTMP/RTMPS, opens TLS with platform validation, and never logs secrets. */
class RtmpPublisher(private val destination: StreamDestination) {
    private var socket: Socket? = null; private var output: OutputStream? = null
    fun connect() { val uri=StreamUrlValidator.validate(destination.serverUrl).getOrThrow(); val port=if(uri.port>0) uri.port else if(uri.scheme=="rtmps") 443 else 1935; socket = if(uri.scheme=="rtmps") SSLSocketFactory.getDefault().createSocket(uri.host,port) else Socket(uri.host,port); socket!!.soTimeout=15_000; output=socket!!.getOutputStream(); handshake() }
    private fun handshake(){ val out=output ?: error("Not connected"); val c0c1=ByteArray(1537); c0c1[0]=3; java.security.SecureRandom().nextBytes(c0c1); c0c1[0]=3; out.write(c0c1); out.flush() }
    fun sendFlvTag(tag:ByteArray){ require(tag.size < 2_000_000){"FLV tag too large"}; output?.write(tag) ?: error("RTMP socket is not connected") }
    fun close(){ runCatching{output?.flush()}; runCatching{socket?.close()}; output=null; socket=null }
}
