package com.example.screencaster.network
import android.content.Context
import android.net.*
class NetworkMonitor(context:Context){ private val cm=context.getSystemService(ConnectivityManager::class.java); fun currentTransport():String { val n=cm.activeNetwork ?: return "No internet"; val c=cm.getNetworkCapabilities(n) ?: return "Unknown"; if(!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return "No internet"; return when { c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->"Wi-Fi"; c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->"Cellular"; c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->"Ethernet"; c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)->"VPN"; else->"Other" } } }
