package com.manzft.wonderstudio.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

// Recibe por UDP los logs que Wonder Maker manda desde el juego (puerto 9174),
// igual que el Studio de PC. También interpreta el handshake "@@hello".
class UdpLogReceiver(private val onLine: (String) -> Unit) {

	private var socket: DatagramSocket? = null
	private var worker: Thread? = null

	@Volatile
	private var running = false

	fun start(port: Int = LOG_PORT) {
		if (running) return
		running = true
		worker = thread(name = "udp-log-receiver", isDaemon = true) {
			try {
				val datagramSocket = DatagramSocket(port)
				datagramSocket.broadcast = true
				socket = datagramSocket
				val buffer = ByteArray(BUFFER_SIZE)
				while (running && !datagramSocket.isClosed) {
					val packet = DatagramPacket(buffer, buffer.size)
					datagramSocket.receive(packet)
					val message = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
					if (message.isEmpty()) continue
					if (message == CONTROL_PREFIX) {
						onLine("Wonder Studio v$ENGINE_VERSION")
						onLine("Connected to Wonder Maker")
					} else {
						onLine(message)
					}
				}
			} catch (_: Exception) {
				if (running) onLine("Couldn't listen for logs on port $port")
			}
		}
	}

	fun stop() {
		running = false
		socket?.close()
		socket = null
		worker?.interrupt()
		worker = null
	}

	companion object {
		const val LOG_PORT = 9174
		private const val BUFFER_SIZE = 8192
		private const val CONTROL_PREFIX = "@@hello"
		private const val ENGINE_VERSION = "1.0"
	}
}
