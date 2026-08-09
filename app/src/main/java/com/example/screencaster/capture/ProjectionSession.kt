package com.example.screencaster.capture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Handler
import android.view.Surface
class ProjectionSession(private val projection:MediaProjection){ private var display:VirtualDisplay?=null; fun start(width:Int,height:Int,density:Int,surface:Surface,onStop:()->Unit){ projection.registerCallback(object:MediaProjection.Callback(){ override fun onStop()=onStop() }, Handler(android.os.Looper.getMainLooper())); display=projection.createVirtualDisplay("ScreenCaster",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,surface,null,null) } fun stop(){ runCatching{display?.release()}; runCatching{projection.stop()} } }
