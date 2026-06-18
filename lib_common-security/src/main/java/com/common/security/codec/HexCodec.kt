package com.common.security.codec

object HexCodec {
    private val HEX_ARRAY = "0123456789abcdef".toCharArray()

    @JvmStatic
    @JvmOverloads
    fun encode(bytes: ByteArray, upperCase: Boolean = false): String {
        val chars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val value = bytes[i].toInt() and 0xFF
            chars[i * 2] = HEX_ARRAY[value ushr 4]
            chars[i * 2 + 1] = HEX_ARRAY[value and 0x0F]
        }
        val hex = String(chars)
        return if (upperCase) hex.uppercase() else hex
    }

    @JvmStatic
    fun decode(hex: String): ByteArray {
        val normalized = hex.replace(":", "").replace(" ", "").lowercase()
        require(normalized.length % 2 == 0) { "Hex string length must be even." }

        val bytes = ByteArray(normalized.length / 2)
        for (i in bytes.indices) {
            val high = Character.digit(normalized[i * 2], 16)
            val low = Character.digit(normalized[i * 2 + 1], 16)
            require(high >= 0 && low >= 0) { "Invalid hex string." }
            bytes[i] = ((high shl 4) + low).toByte()
        }
        return bytes
    }

    @JvmStatic
    @JvmOverloads
    fun fingerprint(bytes: ByteArray, upperCase: Boolean = true): String {
        return encode(bytes, upperCase).chunked(2).joinToString(":")
    }
}
