package ro.nucilandia.autocallresponder

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Base64
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** Sends a plain-text e-mail through an SMTP server using implicit TLS or STARTTLS. */
class EmailSender(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {
    /** Sends the message, throwing [IOException] when the server rejects the transaction. */
    fun send(senderAddress: String, recipientAddress: String, subject: String, body: String) {
        val connection = SmtpConnection(host, port)

        try {
            connection.open(useImplicitTls = port == IMPLICIT_TLS_PORT)

            if (port != IMPLICIT_TLS_PORT) {
                connection.startTls()
            }

            if (username.isNotBlank()) {
                connection.authenticate(username, password)
            }

            connection.sendMessage(senderAddress, recipientAddress, subject, body)
            connection.quit()
        } finally {
            connection.close()
        }
    }

    private companion object {
        const val IMPLICIT_TLS_PORT = 465
    }
}

/** Minimal SMTP client session bound to a single socket. */
private class SmtpConnection(
    private val host: String,
    private val port: Int
) {
    private var socket: Socket = Socket()
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter

    fun open(useImplicitTls: Boolean) {
        val plainSocket = Socket()
        plainSocket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLISECONDS)
        socket = if (useImplicitTls) {
            createTlsSocket(plainSocket)
        } else {
            plainSocket
        }
        socket.soTimeout = READ_TIMEOUT_MILLISECONDS

        attachStreams()
        expect(STATUS_READY)
        command("EHLO ${resolveLocalHostName()}", STATUS_OK)
    }

    fun startTls() {
        command("STARTTLS", STATUS_READY)
        socket = createTlsSocket(socket)
        socket.soTimeout = READ_TIMEOUT_MILLISECONDS

        attachStreams()
        command("EHLO ${resolveLocalHostName()}", STATUS_OK)
    }

    fun authenticate(user: String, secret: String) {
        command("AUTH LOGIN", STATUS_AUTH_CHALLENGE)
        commandBase64(user, STATUS_AUTH_CHALLENGE)
        commandBase64(secret, STATUS_AUTH_SUCCESS)
    }

    fun sendMessage(senderAddress: String, recipientAddress: String, subject: String, body: String) {
        command("MAIL FROM:<$senderAddress>", STATUS_OK)
        command("RCPT TO:<$recipientAddress>", STATUS_OK)
        command("DATA", STATUS_DATA_READY)

        writeLine("From: $senderAddress")
        writeLine("To: $recipientAddress")
        writeLine("Subject: ${encodeHeader(subject)}")
        writeLine("MIME-Version: 1.0")
        writeLine("Content-Type: text/plain; charset=UTF-8")
        writeLine("Content-Transfer-Encoding: base64")
        writeLine("")
        writeBase64Body(body)
        expect(STATUS_OK)
    }

    fun quit() {
        command("QUIT", STATUS_CLOSING)
    }

    fun close() {
        try {
            socket.close()
        } catch (exception: IOException) {
            // The connection is being discarded, so a failed close is not actionable.
        }
    }

    private fun attachStreams() {
        reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
        writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII))
    }

    private fun createTlsSocket(plainSocket: Socket): Socket {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val tlsSocket = factory.createSocket(plainSocket, host, port, true) as SSLSocket
        tlsSocket.startHandshake()
        return tlsSocket
    }

    private fun resolveLocalHostName(): String =
        try {
            InetAddress.getLocalHost().canonicalHostName
        } catch (exception: IOException) {
            "localhost"
        }

    private fun command(commandLine: String, expectedStatus: String) {
        writeLine(commandLine)
        expect(expectedStatus)
    }

    private fun commandBase64(value: String, expectedStatus: String) {
        writeLine(Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8)))
        expect(expectedStatus)
    }

    private fun writeLine(line: String) {
        writer.write(line)
        writer.write(LINE_TERMINATOR)
        writer.flush()
    }

    private fun writeBase64Body(body: String) {
        val encoded = Base64.getEncoder().encodeToString(body.toByteArray(Charsets.UTF_8))
        encoded.chunked(BASE64_LINE_LENGTH).forEach { chunk -> writeLine(chunk) }
        writeLine(END_OF_DATA)
    }

    private fun encodeHeader(value: String): String =
        if (value.all { character -> character.code in ASCII_PRINTABLE_RANGE }) {
            value
        } else {
            "=?UTF-8?B?${Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))}?="
        }

    private fun expect(expectedStatus: String) {
        while (true) {
            val line = reader.readLine() ?: throw IOException("The SMTP server closed the connection")

            val isMultiLine = line.length > EXPECTED_LINE_PREFIX_LENGTH && line[3] == '-'
            if (isMultiLine) continue

            if (!line.startsWith(expectedStatus)) {
                throw IOException("SMTP server rejected the request: $line")
            }
            return
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLISECONDS = 20_000
        const val READ_TIMEOUT_MILLISECONDS = 20_000
        const val BASE64_LINE_LENGTH = 76
        const val EXPECTED_LINE_PREFIX_LENGTH = 4
        const val LINE_TERMINATOR = "\r\n"
        const val END_OF_DATA = "."

        const val STATUS_READY = "220"
        const val STATUS_OK = "250"
        const val STATUS_DATA_READY = "354"
        const val STATUS_CLOSING = "221"
        const val STATUS_AUTH_CHALLENGE = "334"
        const val STATUS_AUTH_SUCCESS = "235"

        val ASCII_PRINTABLE_RANGE = 32..126
    }
}
