package com.example.compiler

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary modifier for resources.arsc.
 * 1. Modifies the global string pool index 0 (app_name) so the Android system displays the user's custom App Title.
 * 2. Updates the package name in RES_TABLE_PACKAGE_TYPE (chunk 0x0200) so Android PackageParser validates the package without Parse Errors.
 */
object ArscModifier {

  fun modifyArsc(arscData: ByteArray, newPkg: String, newTitle: String): ByteArray {
    if (arscData.size < 40) return arscData

    val buf = ByteBuffer.wrap(arscData).order(ByteOrder.LITTLE_ENDIAN)

    val chunkType = buf.short.toInt() and 0xFFFF
    val headerSize = buf.short.toInt() and 0xFFFF
    val fileSize = buf.int
    val packageCount = buf.int

    if (chunkType != 0x0002) return arscData

    // StringPool chunk starts at offset 12
    buf.position(12)
    val spType = buf.short.toInt() and 0xFFFF
    val spHeaderSize = buf.short.toInt() and 0xFFFF
    val spChunkSize = buf.int
    val stringCount = buf.int
    val styleCount = buf.int
    val flags = buf.int
    val stringsStart = buf.int
    val stylesStart = buf.int

    if (spType != 0x0001 || stringCount <= 0) return arscData

    // Read string offsets table
    val offsets = IntArray(stringCount)
    for (i in 0 until stringCount) {
      offsets[i] = buf.int
    }

    // String 0 is app_name ("HTML to APK")
    val off0 = 12 + stringsStart + offsets[0]
    if (off0 >= arscData.size) return arscData

    buf.position(off0)
    val u16_b1 = buf.get().toInt() and 0xFF
    if (u16_b1 and 0x80 != 0) buf.get()
    val u8_b1 = buf.get().toInt() and 0xFF
    val oldStr0Len = if (u8_b1 and 0x80 != 0) {
      val u8_b2 = buf.get().toInt() and 0xFF
      ((u8_b1 and 0x7F) shl 8) or u8_b2
    } else u8_b1

    val oldStr0TotalBytes = (buf.position() + oldStr0Len + 1) - off0

    // Encode newTitle in UTF-8
    val encTitle = newTitle.toByteArray(Charsets.UTF_8)
    val newTitleData = ByteArrayOutputStream()
    if (newTitle.length > 127) {
      newTitleData.write(0x80 or ((newTitle.length shr 8) and 0x7F))
      newTitleData.write(newTitle.length and 0xFF)
    } else {
      newTitleData.write(newTitle.length)
    }

    if (encTitle.size > 127) {
      newTitleData.write(0x80 or ((encTitle.size shr 8) and 0x7F))
      newTitleData.write(encTitle.size and 0xFF)
    } else {
      newTitleData.write(encTitle.size)
    }
    newTitleData.write(encTitle)
    newTitleData.write(0) // null terminator

    val newTitleBytes = newTitleData.toByteArray()
    val delta = newTitleBytes.size - oldStr0TotalBytes

    // Extract rest of strings data (strings 1 through stringCount - 1)
    val restStringsStart = off0 + oldStr0TotalBytes
    val oldStringsEnd = 12 + (if (styleCount > 0 && stylesStart > 0) stylesStart else spChunkSize)
    val restStringsLen = (oldStringsEnd - restStringsStart).coerceAtLeast(0)
    val restStringsData = ByteArray(restStringsLen)
    if (restStringsLen > 0) {
      System.arraycopy(arscData, restStringsStart, restStringsData, 0, restStringsLen)
    }

    val newStringsBlock = ByteArrayOutputStream()
    newStringsBlock.write(newTitleBytes)
    newStringsBlock.write(restStringsData)

    // Pad string data block to 4-byte alignment
    val pad = (4 - (newStringsBlock.size() % 4)) % 4
    for (p in 0 until pad) {
      newStringsBlock.write(0)
    }
    val newStringsBlockBytes = newStringsBlock.toByteArray()

    val newOffsets = IntArray(stringCount)
    newOffsets[0] = 0
    for (i in 1 until stringCount) {
      newOffsets[i] = offsets[i] + delta
    }

    val stylesData = if (styleCount > 0 && stylesStart > 0) {
      val stylesOffset = 12 + stylesStart
      val stylesLen = (12 + spChunkSize) - stylesOffset
      if (stylesLen > 0 && stylesOffset + stylesLen <= arscData.size) {
        val b = ByteArray(stylesLen)
        System.arraycopy(arscData, stylesOffset, b, 0, stylesLen)
        b
      } else ByteArray(0)
    } else ByteArray(0)

    val newSpHeaderSize = 28
    val newStringsStart = newSpHeaderSize + stringCount * 4 + styleCount * 4
    val newStylesStart = if (styleCount > 0) newStringsStart + newStringsBlockBytes.size else 0
    val newSpChunkSize = newStringsStart + newStringsBlockBytes.size + stylesData.size

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

    // Remainder of ARSC (TypeSpec, Types, and Package headers)
    val restOffset = 12 + spChunkSize
    val restLen = (arscData.size - restOffset).coerceAtLeast(0)
    val restOfArsc = ByteArray(restLen)
    if (restLen > 0) {
      System.arraycopy(arscData, restOffset, restOfArsc, 0, restLen)
    }

    // In-place update of package name in RES_TABLE_PACKAGE_TYPE (chunk 0x0200)
    val targetOld = "jk.htmltoapk.sss".toByteArray(Charsets.UTF_16LE)
    val pkgIdx = indexOfBytes(restOfArsc, targetOld)
    if (pkgIdx != -1) {
      val newPkgBytes = newPkg.toByteArray(Charsets.UTF_16LE)
      val fixed256 = ByteArray(256)
      System.arraycopy(newPkgBytes, 0, fixed256, 0, minOf(newPkgBytes.size, 256))
      System.arraycopy(fixed256, 0, restOfArsc, pkgIdx, 256)
    }

    val newTotalSize = 12 + newSpHeader.size + newOffsetsBuf.array().size + newStringsBlockBytes.size + stylesData.size + restOfArsc.size
    val newHeader = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
      .putShort(chunkType.toShort())
      .putShort(headerSize.toShort())
      .putInt(newTotalSize)
      .putInt(packageCount)
      .array()

    return newHeader + newSpHeader + newOffsetsBuf.array() + newStringsBlockBytes + stylesData + restOfArsc
  }

  private fun indexOfBytes(source: ByteArray, target: ByteArray): Int {
    if (target.isEmpty() || source.size < target.size) return -1
    for (i in 0..source.size - target.size) {
      var match = true
      for (j in target.indices) {
        if (source[i + j] != target[j]) {
          match = false
          break
        }
      }
      if (match) return i
    }
    return -1
  }
}
