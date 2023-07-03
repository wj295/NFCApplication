package com.example.nfcapplication.util

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.util.Log
import android.util.SparseArray
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and


class NfcReadUtility {

    /**
     * {@inheritDoc}
     */
    fun readFromTagWithSparseArray(nfcDataIntent: Intent?): SparseArray<String?>? {
        val messages = nfcDataIntent?.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        val resultMap: SparseArray<String?> =
            if (messages != null) SparseArray(messages.size) else SparseArray(0)
        if (messages == null) {
            return resultMap
        }
        messages.forEach {
            (it as NdefMessage).records.forEach { record ->
                val type = retrieveTypeByte(record.payload).toInt()
                if (resultMap[type] == null) {
                    resultMap.put(type, parseAccordingToType(record))
                }
            }
        }
        return resultMap
    }

    /**
     * {@inheritDoc}
     */
    fun readFromTagWithMap(nfcDataIntent: Intent?) =
        mutableMapOf<Byte?, String?>().also { map ->
            readFromTagWithSparseArray(nfcDataIntent)?.let {
                for (i in 0 until it.size()) {
                    map[it.keyAt(i).toByte()] = it.valueAt(i)
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    fun retrieveMessageTypes(record: NdefMessage?) = mutableListOf<Byte>().also {
        record?.records?.forEach { r -> it.add(retrieveTypeByte(r.payload)) }
    }.iterator()

    /**
     * {@inheritDoc}
     */
    fun retrieveMessage(message: NdefMessage?) =
        message?.records?.get(0)?.let { parseAccordingToHeader(it.payload) }

    private fun retrieveTypeByte(payload: ByteArray) = if (payload.isNotEmpty()) payload[0] else -1

    private fun parseAccordingToHeader(payload: ByteArray) = if (payload.isNotEmpty()) String(
        payload,
        1,
        payload.size - 1,
        Charset.forName("US-ASCII")
    ).trim { it <= ' ' } else ""

    private fun parseAccordingToType(obj: NdefRecord): String {
        if (Arrays.equals(obj.type, "application/vnd.bluetooth.ep.oob".toByteArray())) {
            val toConvert = obj.payload
            val result = StringBuilder()
            for (i in toConvert.size - 1 downTo 2) {
                val temp = toConvert[i]
                val tempString =
                    if (temp < 0) Integer.toHexString(temp + Byte.MAX_VALUE)
                    else Integer.toHexString(temp.toInt())
                result.append(if (tempString.length < 2) "0$tempString" else tempString)
                result.append(":")
            }
            return if (result.isNotEmpty()) result.substring(
                0,
                result.length - 1
            ) else result.toString()
        }
        return Uri.parse(parseAccordingToHeader(obj.payload)).toString()
    }

    companion object {
        private val TAG = NfcReadUtility::class.java.canonicalName
    }


    /*************************************/
    open fun detectTagData(tag: Tag): String? {
        val sb = java.lang.StringBuilder()
        val id: ByteArray = tag.getId()
        sb.append("ID (hex): ").append(toHex(id)).append('\n')
        sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n')
        sb.append("ID (dec): ").append(toDec(id)).append('\n')
        sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n')
        val prefix = "android.nfc.tech."
        sb.append("Technologies: ")
        for (tech in tag.getTechList()) {
            sb.append(tech.substring(prefix.length))
            sb.append(", ")
        }
        sb.delete(sb.length - 2, sb.length)
        for (tech in tag.getTechList()) {
            if (tech == MifareClassic::class.java.name) {
                sb.append('\n')
                var type = "Unknown"
                try {
                    val mifareTag = MifareClassic.get(tag)
                    when (mifareTag.type) {
                        MifareClassic.TYPE_CLASSIC -> type = "Classic"
                        MifareClassic.TYPE_PLUS -> type = "Plus"
                        MifareClassic.TYPE_PRO -> type = "Pro"
                    }
                    sb.append("Mifare Classic type: ")
                    sb.append(type)
                    sb.append('\n')
                    sb.append("Mifare size: ")
                    sb.append(mifareTag.size.toString() + " bytes")
                    sb.append('\n')
                    sb.append("Mifare sectors: ")
                    sb.append(mifareTag.sectorCount)
                    sb.append('\n')
                    sb.append("Mifare blocks: ")
                    sb.append(mifareTag.blockCount)
                } catch (e: Exception) {
                    sb.append("Mifare classic error: " + e.message)
                }
            }
            if (tech == MifareUltralight::class.java.name) {
                sb.append('\n')
                val mifareUlTag = MifareUltralight.get(tag)
                var type = "Unknown"
                when (mifareUlTag.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> type = "Ultralight"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> type = "Ultralight C"
                }
                sb.append("Mifare Ultralight type: ")
                sb.append(type)
            }
        }
        Log.v("test", sb.toString())
        return sb.toString()
    }

    private fun toHex(bytes: ByteArray): String? {
        val sb = java.lang.StringBuilder()
        for (i in bytes.indices.reversed()) {
            val b: Int = (bytes[i] and 0xff.toByte()).toInt()
            if (b < 0x10) sb.append('0')
            sb.append(Integer.toHexString(b))
            if (i > 0) {
                sb.append(" ")
            }
        }
        return sb.toString()
    }

    private fun toReversedHex(bytes: ByteArray): String? {
        val sb = java.lang.StringBuilder()
        for (i in bytes.indices) {
            if (i > 0) {
                sb.append(" ")
            }
            val b: Int = (bytes[i] and 0xff.toByte()).toInt()
            if (b < 0x10) sb.append('0')
            sb.append(Integer.toHexString(b))
        }
        return sb.toString()
    }

    private fun toDec(bytes: ByteArray): Long {
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.indices) {
            val value: Long = (bytes[i] and 0xffL.toByte()).toLong()
            result += value * factor
            factor *= 256L
        }
        return result
    }

    private fun toReversedDec(bytes: ByteArray): Long {
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.indices.reversed()) {
            val value: Long = (bytes[i] and 0xffL.toByte()).toLong()
            result += value * factor
            factor *= 256L
        }
        return result
    }
}