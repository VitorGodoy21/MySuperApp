package com.vfdeginformatica.mysuperapp.nfc.data

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationError
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationResult
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcTagContent
import com.vfdeginformatica.mysuperapp.nfc.domain.repository.NfcTagRepository
import java.io.IOException
import javax.inject.Inject

/**
 * Implementação de [NfcTagRepository] baseada em `android.nfc.tech.Ndef`.
 * Grava e lê sempre um único [NdefRecord] do tipo URI, compatível com
 * tags NFC Forum Type 2 (ex.: NTAG213/215/216).
 */
class NdefTagReaderWriter @Inject constructor() : NfcTagRepository {

    override suspend fun read(tag: Tag): NfcOperationResult<NfcTagContent> {
        val ndef = Ndef.get(tag)
            ?: return NfcOperationResult.Failure(NfcOperationError.TagNotNdef)

        return try {
            ndef.connect()

            val message = ndef.ndefMessage
                ?: return NfcOperationResult.Failure(NfcOperationError.EmptyTag)

            val url = message.records.firstOrNull()?.let(::extractUri)
                ?: return NfcOperationResult.Failure(NfcOperationError.EmptyTag)

            NfcOperationResult.Success(
                NfcTagContent(
                    url = url,
                    isWritable = ndef.isWritable,
                    maxSizeBytes = ndef.maxSize,
                    usedSizeBytes = message.toByteArray().size
                )
            )
        } catch (error: TagLostException) {
            NfcOperationResult.Failure(NfcOperationError.TagLost)
        } catch (error: IOException) {
            NfcOperationResult.Failure(NfcOperationError.Unknown(error.message ?: "Falha de E/S"))
        } finally {
            runCatching { ndef.close() }
        }
    }

    override suspend fun write(tag: Tag, url: String): NfcOperationResult<Unit> {
        val message = NdefMessage(arrayOf(NdefRecord.createUri(url)))
        val payloadSize = message.toByteArray().size

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return writeToNdefTag(ndef, message, payloadSize)
        }

        val formatable = NdefFormatable.get(tag)
            ?: return NfcOperationResult.Failure(NfcOperationError.TagNotNdef)

        return formatTagAndWrite(formatable, message)
    }

    override suspend fun lock(tag: Tag): NfcOperationResult<Unit> {
        val ndef = Ndef.get(tag)
            ?: return NfcOperationResult.Failure(NfcOperationError.TagNotNdef)

        return try {
            ndef.connect()

            if (!ndef.isWritable) {
                return NfcOperationResult.Failure(NfcOperationError.TagReadOnly)
            }

            if (ndef.makeReadOnly()) {
                NfcOperationResult.Success(Unit)
            } else {
                NfcOperationResult.Failure(
                    NfcOperationError.Unknown("O bloqueio da tag não foi confirmado pelo hardware.")
                )
            }
        } catch (error: TagLostException) {
            NfcOperationResult.Failure(NfcOperationError.TagLost)
        } catch (error: IOException) {
            NfcOperationResult.Failure(NfcOperationError.Unknown(error.message ?: "Falha de E/S"))
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun writeToNdefTag(
        ndef: Ndef,
        message: NdefMessage,
        payloadSize: Int
    ): NfcOperationResult<Unit> {
        return try {
            ndef.connect()

            if (!ndef.isWritable) {
                return NfcOperationResult.Failure(NfcOperationError.TagReadOnly)
            }

            if (payloadSize > ndef.maxSize) {
                return NfcOperationResult.Failure(
                    NfcOperationError.TagTooSmall(payloadSize, ndef.maxSize)
                )
            }

            ndef.writeNdefMessage(message)
            NfcOperationResult.Success(Unit)
        } catch (error: TagLostException) {
            NfcOperationResult.Failure(NfcOperationError.TagLost)
        } catch (error: FormatException) {
            NfcOperationResult.Failure(NfcOperationError.TagNotNdef)
        } catch (error: IOException) {
            NfcOperationResult.Failure(NfcOperationError.Unknown(error.message ?: "Falha de E/S"))
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun formatTagAndWrite(
        formatable: NdefFormatable,
        message: NdefMessage
    ): NfcOperationResult<Unit> {
        return try {
            formatable.connect()
            formatable.format(message)
            NfcOperationResult.Success(Unit)
        } catch (error: TagLostException) {
            NfcOperationResult.Failure(NfcOperationError.TagLost)
        } catch (error: FormatException) {
            NfcOperationResult.Failure(NfcOperationError.TagNotNdef)
        } catch (error: IOException) {
            NfcOperationResult.Failure(NfcOperationError.Unknown(error.message ?: "Falha de E/S"))
        } finally {
            runCatching { formatable.close() }
        }
    }

    private fun extractUri(record: NdefRecord): String? = try {
        record.toUri()?.toString()
    } catch (error: Exception) {
        null
    }
}
