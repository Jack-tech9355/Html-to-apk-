package com.example.compiler

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary Android XML (AXML) modifier that replaces package names in-place
 * within AndroidManifest.xml's StringPool table, updating string offsets,
 * chunk sizes, and header lengths so Android OS treats the compiled APK
 * as an independent application and never prompts to update the builder app.
 */
object AxmlPackageModifier {

  fun modifyPackageName(axmlData: ByteArray, oldPkg: String, newPkg: String): ByteArray {
    if (axmlData.size < 36 || oldPkg == newPkg) return axmlData

    val buf = ByteBuffer.wrap(axmlData).order(ByteOrder.LITTLE_ENDIAN)

    val chunkType = buf.short.toInt() and 0xFFFF
    val headerSize = buf.short.toInt() and 0xFFFF
    val fileSize = buf.int

    // AXML file header must be 0x0003
    if (chunkType != 0x0003) {
      return axmlData
    }

    // StringPool chunk starts at byte offset 8
    buf.position(8)
    val spType = buf.short.toInt() and 0xFFFF
    val spHeaderSize = buf.short.toInt() and 0xFFFF
    val spChunkSize = buf.int
    val stringCount = buf.int
    val styleCount = buf.int
    val flags = buf.int
    val stringsStart = buf.int
    val stylesStart = buf.int

    if (spType != 0x0001) {
      return axmlData
    }

    val isUtf8 = (flags and (1 shl 8)) != 0

    // Read string offsets table
    val offsets = IntArray(stringCount)
    for (i in 0 until stringCount) {
      offsets[i] = buf.int
    }

    // Extract all strings from pool and substitute package name
    val strings = ArrayList<String>(stringCount)
    val stringPoolBase = 8 + stringsStart

    for (i in 0 until stringCount) {
      val strOffset = stringPoolBase + offsets[i]
      if (strOffset < 0 || strOffset >= axmlData.size) {
        return axmlData
      }
      buf.position(strOffset)
      if (isUtf8) {
        val u16len = buf.get().toInt() and 0xFF
        val u8len = buf.get().toInt() and 0xFF
        val strBytes = ByteArray(u8len)
        buf.get(strBytes)
        val s = String(strBytes, Charsets.UTF_8)
        strings.add(s.replace(oldPkg, newPkg))
      } else {
        val u16len = buf.short.toInt() and 0xFFFF
        val charBytes = ByteArray(u16len * 2)
        buf.get(charBytes)
        val s = String(charBytes, Charsets.UTF_16LE)
        strings.add(s.replace(oldPkg, newPkg))
      }
    }

    // Re-encode strings and calculate new relative offsets
    val newStringData = ByteArrayOutputStream()
    val newOffsets = IntArray(stringCount)

    for (i in 0 until stringCount) {
      newOffsets[i] = newStringData.size()
      val s = strings[i]
      if (isUtf8) {
        val encoded = s.toByteArray(Charsets.UTF_8)
        newStringData.write(s.length and 0xFF)
        newStringData.write(encoded.size and 0xFF)
        newStringData.write(encoded)
        newStringData.write(0) // null terminator
      } else {
        val encoded = s.toByteArray(Charsets.UTF_16LE)
        val lenBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(s.length.toShort()).array()
        newStringData.write(lenBytes)
        newStringData.write(encoded)
        newStringData.write(0) // 2 null bytes for UTF-16
        newStringData.write(0)
      }
    }

    // Pad newStringData to 4-byte boundary
    val pad = (4 - (newStringData.size() % 4)) % 4
    for (p in 0 until pad) {
      newStringData.write(0)
    }

    // Styles data (if any)
    val stylesData = if (styleCount > 0 && stylesStart > 0) {
      val stylesOffset = 8 + stylesStart
      val stylesLen = (8 + spChunkSize) - stylesOffset
      if (stylesLen > 0 && stylesOffset + stylesLen <= axmlData.size) {
        val b = ByteArray(stylesLen)
        System.arraycopy(axmlData, stylesOffset, b, 0, stylesLen)
        b
      } else ByteArray(0)
    } else {
      ByteArray(0)
    }

    val newSpHeaderSize = 28
    val newStringsStart = newSpHeaderSize + stringCount * 4 + styleCount * 4
    val newStylesStart = if (styleCount > 0) newStringsStart + newStringData.size() else 0
    val newSpChunkSize = newStringsStart + newStringData.size() + stylesData.size

    val newSpHeader = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)
      .putShort(0x0001.toShort())
      .putShort(newSpHeaderSize.toShort())
      .putInt(newSpChunkSize)
      .putInt(stringCount)
      .putInt(styleCount)
      .putInt(flags)
      .putInt(newStringsStart)
      .putInt(newStylesStart)
      .array()

    val newOffsetsBuf = ByteBuffer.allocate(stringCount * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (off in newOffsets) {
      newOffsetsBuf.putInt(off)
    }

    val restOffset = 8 + spChunkSize
    val restLen = axmlData.size - restOffset
    val restOfAxml = ByteArray(restLen)
    System.arraycopy(axmlData, restOffset, restOfAxml, 0, restLen)

    val newSpChunkBytes = newSpHeader + newOffsetsBuf.array() + newStringData.toByteArray() + stylesData
    val newTotalSize = 8 + newSpChunkBytes.size + restOfAxml.size

    val newHeader = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
      .putShort(chunkType.toShort())
      .putShort(headerSize.toShort())
      .putInt(newTotalSize)
      .array()

    return newHeader + newSpChunkBytes + restOfAxml
  }
}
