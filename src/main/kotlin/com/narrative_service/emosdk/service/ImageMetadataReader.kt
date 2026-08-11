package com.narrative_service.emosdk.service

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ImageMetadata(
    val mimeType: String,
    val extension: String,
    val width: Int,
    val height: Int
)

object ImageMetadataReader {

    fun read(bytes: ByteArray): ImageMetadata? {
        return readPng(bytes)
            ?: readJpeg(bytes)
            ?: readGif(bytes)
            ?: readWebp(bytes)
    }

    private fun readPng(bytes: ByteArray): ImageMetadata? {
        val signature = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        if (bytes.size < 24 || !bytes.copyOfRange(0, 8).contentEquals(signature)) {
            return null
        }

        val width = readInt(bytes, 16, ByteOrder.BIG_ENDIAN)
        val height = readInt(bytes, 20, ByteOrder.BIG_ENDIAN)

        return metadata("image/png", "png", width, height)
    }

    private fun readJpeg(bytes: ByteArray): ImageMetadata? {
        if (bytes.size < 4 || unsigned(bytes[0]) != 0xFF || unsigned(bytes[1]) != 0xD8) {
            return null
        }

        var offset = 2
        while (offset + 9 < bytes.size) {
            while (offset < bytes.size && unsigned(bytes[offset]) == 0xFF) {
                offset++
            }

            if (offset >= bytes.size) {
                return null
            }

            val marker = unsigned(bytes[offset])
            offset++

            if (marker == 0xD9 || marker == 0xDA) {
                return null
            }

            if (offset + 2 > bytes.size) {
                return null
            }

            val length = readUnsignedShort(bytes, offset, ByteOrder.BIG_ENDIAN)
            if (length < 2 || offset + length > bytes.size) {
                return null
            }

            if (marker in setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)) {
                val height = readUnsignedShort(bytes, offset + 3, ByteOrder.BIG_ENDIAN)
                val width = readUnsignedShort(bytes, offset + 5, ByteOrder.BIG_ENDIAN)
                return metadata("image/jpeg", "jpg", width, height)
            }

            offset += length
        }

        return null
    }

    private fun readGif(bytes: ByteArray): ImageMetadata? {
        if (bytes.size < 10) {
            return null
        }

        val header = bytes.copyOfRange(0, 6).toString(Charsets.US_ASCII)
        if (header != "GIF87a" && header != "GIF89a") {
            return null
        }

        val width = readUnsignedShort(bytes, 6, ByteOrder.LITTLE_ENDIAN)
        val height = readUnsignedShort(bytes, 8, ByteOrder.LITTLE_ENDIAN)

        return metadata("image/gif", "gif", width, height)
    }

    private fun readWebp(bytes: ByteArray): ImageMetadata? {
        if (bytes.size < 30) {
            return null
        }

        if (ascii(bytes, 0, 4) != "RIFF" || ascii(bytes, 8, 4) != "WEBP") {
            return null
        }

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkType = ascii(bytes, offset, 4)
            val chunkSize = readInt(bytes, offset + 4, ByteOrder.LITTLE_ENDIAN)
            val payloadOffset = offset + 8
            if (chunkSize < 0 || payloadOffset + chunkSize > bytes.size) {
                return null
            }

            when (chunkType) {
                "VP8X" -> {
                    if (chunkSize < 10) {
                        return null
                    }

                    val width = read24(bytes, payloadOffset + 4) + 1
                    val height = read24(bytes, payloadOffset + 7) + 1
                    return metadata("image/webp", "webp", width, height)
                }

                "VP8L" -> {
                    if (chunkSize < 5 || unsigned(bytes[payloadOffset]) != 0x2F) {
                        return null
                    }

                    val bits = unsigned(bytes[payloadOffset + 1]) or
                        (unsigned(bytes[payloadOffset + 2]) shl 8) or
                        (unsigned(bytes[payloadOffset + 3]) shl 16) or
                        (unsigned(bytes[payloadOffset + 4]) shl 24)
                    val width = (bits and 0x3FFF) + 1
                    val height = ((bits shr 14) and 0x3FFF) + 1
                    return metadata("image/webp", "webp", width, height)
                }

                "VP8 " -> {
                    if (chunkSize < 10 ||
                        unsigned(bytes[payloadOffset + 3]) != 0x9D ||
                        unsigned(bytes[payloadOffset + 4]) != 0x01 ||
                        unsigned(bytes[payloadOffset + 5]) != 0x2A
                    ) {
                        return null
                    }

                    val width = readUnsignedShort(bytes, payloadOffset + 6, ByteOrder.LITTLE_ENDIAN) and 0x3FFF
                    val height = readUnsignedShort(bytes, payloadOffset + 8, ByteOrder.LITTLE_ENDIAN) and 0x3FFF
                    return metadata("image/webp", "webp", width, height)
                }
            }

            offset = payloadOffset + chunkSize + (chunkSize % 2)
        }

        return null
    }

    private fun metadata(
        mimeType: String,
        extension: String,
        width: Int,
        height: Int
    ): ImageMetadata? {
        if (width <= 0 || height <= 0) {
            return null
        }

        return ImageMetadata(
            mimeType = mimeType,
            extension = extension,
            width = width,
            height = height
        )
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String {
        return bytes.copyOfRange(offset, offset + length).toString(Charsets.US_ASCII)
    }

    private fun readInt(bytes: ByteArray, offset: Int, order: ByteOrder): Int {
        return ByteBuffer.wrap(bytes, offset, 4).order(order).int
    }

    private fun readUnsignedShort(bytes: ByteArray, offset: Int, order: ByteOrder): Int {
        return ByteBuffer.wrap(bytes, offset, 2).order(order).short.toInt() and 0xFFFF
    }

    private fun read24(bytes: ByteArray, offset: Int): Int {
        return unsigned(bytes[offset]) or
            (unsigned(bytes[offset + 1]) shl 8) or
            (unsigned(bytes[offset + 2]) shl 16)
    }

    private fun unsigned(byte: Byte): Int {
        return byte.toInt() and 0xFF
    }
}
