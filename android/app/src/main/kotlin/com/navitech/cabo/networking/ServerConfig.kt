package com.navitech.cabo.networking

/**
 * Single source of truth for the relay server URL.
 *
 *  - For local testing against a dev server running on your laptop, set
 *    [DEFAULT_LOCAL_DEV_URL] to your laptop's LAN IP (NOT `localhost`, since
 *    the phone needs to reach it).
 *  - For production play across the internet, set [DEFAULT_PRODUCTION_URL] to
 *    your deployed server. Use `wss://` so Android's network security config
 *    accepts it without cleartext exemptions.
 */
object ServerConfig {
    const val DEFAULT_PRODUCTION_URL = "wss://cabogame.onrender.com/ws"
    const val DEFAULT_LOCAL_DEV_URL = "ws://192.168.1.1:8080/ws"

    val current: String get() = DEFAULT_PRODUCTION_URL
}
