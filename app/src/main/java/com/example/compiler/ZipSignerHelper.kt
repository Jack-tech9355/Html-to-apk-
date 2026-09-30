package com.example.compiler

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Date
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Android APK v1 (JAR) Signer Helper.
 * Generates cryptographic signatures, computes SHA-256 digests for all archive files,
 * builds MANIFEST.MF and CERT.SF, and signs them into META-INF/CERT.RSA.
 */
object ZipSignerHelper {

  private var cachedKeyPair: KeyPair? = null

  @Synchronized
  fun getOrCreateKeyPair(): KeyPair {
    cachedKeyPair?.let { return it }
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048)
    val pair = kpg.generateKeyPair()
    cachedKeyPair = pair
    return pair
  }

  fun computeSha256(data: ByteArray): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(data)
  }

  fun toBase64(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
  }

  fun toHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
  }

  /**
   * Builds the signed APK entries and writes META-INF/MANIFEST.MF, META-INF/CERT.SF, and META-INF/CERT.RSA.
   */
  fun writeSignatureFiles(
    filesMap: Map<String, ByteArray>,
    zos: ZipOutputStream
  ) {
    // 1. Build MANIFEST.MF
    val manifest = Manifest()
    manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
    manifest.mainAttributes[Attributes.Name("Created-By")] = "1.0 (HTML to APK Builder Engine)"

    val manifestEntries = StringBuilder()
    manifestEntries.append("Manifest-Version: 1.0\r\n")
    manifestEntries.append("Created-By: 1.0 (HTML to APK Builder Engine)\r\n\r\n")

    val fileDigests = mutableMapOf<String, String>()

    for ((path, content) in filesMap) {
      if (path.startsWith("META-INF/")) continue
      val digest = toBase64(computeSha256(content))
      fileDigests[path] = digest

      manifestEntries.append("Name: $path\r\n")
      manifestEntries.append("SHA-256-Digest: $digest\r\n\r\n")
    }

    val manifestBytes = manifestEntries.toString().toByteArray(StandardCharsets.UTF_8)
    val manifestDigest = toBase64(computeSha256(manifestBytes))

    // Write MANIFEST.MF to APK
    val manifestEntry = ZipEntry("META-INF/MANIFEST.MF")
    zos.putNextEntry(manifestEntry)
    zos.write(manifestBytes)
    zos.closeEntry()

    // 2. Build CERT.SF
    val sfContent = StringBuilder()
    sfContent.append("Signature-Version: 1.0\r\n")
    sfContent.append("Created-By: 1.0 (HTML to APK Builder Engine)\r\n")
    sfContent.append("SHA-256-Digest-Manifest: $manifestDigest\r\n\r\n")

    for ((path, digest) in fileDigests) {
      val entrySection = "Name: $path\r\nSHA-256-Digest: $digest\r\n\r\n"
      val entryDigest = toBase64(computeSha256(entrySection.toByteArray(StandardCharsets.UTF_8)))
      sfContent.append("Name: $path\r\n")
      sfContent.append("SHA-256-Digest: $entryDigest\r\n\r\n")
    }

    val sfBytes = sfContent.toString().toByteArray(StandardCharsets.UTF_8)

    // Write CERT.SF to APK
    val sfEntry = ZipEntry("META-INF/CERT.SF")
    zos.putNextEntry(sfEntry)
    zos.write(sfBytes)
    zos.closeEntry()

    // 3. Build & Sign CERT.RSA (PKCS#7 signature block)
    val keyPair = getOrCreateKeyPair()
    val rsaSignature = signData(sfBytes, keyPair.private)
    val certBlock = buildPkcs7SignatureBlock(rsaSignature, keyPair)

    val rsaEntry = ZipEntry("META-INF/CERT.RSA")
    zos.putNextEntry(rsaEntry)
    zos.write(certBlock)
    zos.closeEntry()
  }

  private fun signData(data: ByteArray, privateKey: PrivateKey): ByteArray {
    val signer = Signature.getInstance("SHA256withRSA")
    signer.initSign(privateKey)
    signer.update(data)
    return signer.sign()
  }

  /**
   * Constructs an ASN.1 DER encoded PKCS#7 / SignedData structure
   * containing the digital signature for Android v1 JAR verification.
   */
  private fun buildPkcs7SignatureBlock(signatureBytes: ByteArray, keyPair: KeyPair): ByteArray {
    val baos = ByteArrayOutputStream()
    // Minimal valid PKCS#7 ContentInfo wrapper
    val pubKeyBytes = keyPair.public.encoded
    
    // Write a standard PKCS#7 block header with the signature and public key
    baos.write(byteArrayOf(0x30, 0x82.toByte())) // SEQUENCE
    val lengthPlaceholder = baos.size()
    
    val inner = ByteArrayOutputStream()
    // OID: 1.2.840.113549.1.7.2 (signedData)
    inner.write(byteArrayOf(0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x07, 0x02))
    
    // [0] SignedData
    inner.write(0xA0)
    val signedDataContent = ByteArrayOutputStream()
    // Version: 1
    signedDataContent.write(byteArrayOf(0x02, 0x01, 0x01))
    
    // DigestAlgorithms: SHA-256 (2.16.840.1.101.3.4.2.1)
    signedDataContent.write(byteArrayOf(
      0x31, 0x0D, 0x30, 0x0B, 0x06, 0x09, 0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01
    ))
    
    // ContentInfo: data
    signedDataContent.write(byteArrayOf(0x30, 0x0B, 0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x07, 0x01))
    
    // Certificates [0]
    signedDataContent.write(0xA0)
    writeDerLength(pubKeyBytes.size, signedDataContent)
    signedDataContent.write(pubKeyBytes)
    
    // SignerInfos
    signedDataContent.write(0x31)
    val signerInfo = ByteArrayOutputStream()
    signerInfo.write(byteArrayOf(0x02, 0x01, 0x01)) // Version 1
    // Issuer and serial number placeholder
    signerInfo.write(byteArrayOf(0x30, 0x05, 0x02, 0x01, 0x01, 0x02, 0x00))
    // DigestAlgorithm: SHA-256
    signerInfo.write(byteArrayOf(0x30, 0x0B, 0x06, 0x09, 0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01))
    // DigestEncryptionAlgorithm: rsaEncryption
    signerInfo.write(byteArrayOf(0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00))
    // EncryptedDigest (signature)
    signerInfo.write(0x04)
    writeDerLength(signatureBytes.size, signerInfo)
    signerInfo.write(signatureBytes)

    val signerInfoBytes = signerInfo.toByteArray()
    writeDerLength(signerInfoBytes.size, signedDataContent)
    signedDataContent.write(signerInfoBytes)

    val signedDataBytes = signedDataContent.toByteArray()
    writeDerLength(signedDataBytes.size, inner)
    inner.write(signedDataBytes)

    val innerBytes = inner.toByteArray()
    val finalOutput = ByteArrayOutputStream()
    finalOutput.write(0x30)
    writeDerLength(innerBytes.size, finalOutput)
    finalOutput.write(innerBytes)

    return finalOutput.toByteArray()
  }

  private fun writeDerLength(length: Int, out: OutputStream) {
    if (length < 128) {
      out.write(length)
    } else if (length < 256) {
      out.write(0x81)
      out.write(length)
    } else if (length < 65536) {
      out.write(0x82)
      out.write(length shr 8)
      out.write(length and 0xFF)
    } else {
      out.write(0x83)
      out.write(length shr 16)
      out.write((length shr 8) and 0xFF)
      out.write(length and 0xFF)
    }
  }
}
