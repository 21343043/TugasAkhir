package com.fadhil.financereceipt.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Semua operasi citra besar dipanggil pada Dispatchers.IO. */
class ReceiptImageStore(private val context: Context) {
    private val drafts get() = File(context.cacheDir, "receipt_drafts").apply { mkdirs() }
    private val camera get() = File(context.cacheDir, "receipt_camera").apply { mkdirs() }
    private val receipts get() = File(context.filesDir, "receipts").apply { mkdirs() }

    fun createCameraUri(): Uri = FileProvider.getUriForFile(
        context, "${context.packageName}.receipt.fileprovider",
        File.createTempFile("capture_", ".jpg", camera)
    )

    fun deleteCamera(uri: Uri) {
        if (uri.authority == "${context.packageName}.receipt.fileprovider")
            runCatching { context.contentResolver.delete(uri, null, null) }
    }

    fun importImage(uri: Uri): String {
        val source = File.createTempFile("source_", ".img", drafts)
        val output = File(drafts, "${UUID.randomUUID()}.jpg")
        try {
            val input = requireNotNull(context.contentResolver.openInputStream(uri)) { "Gambar tidak dapat dibuka." }
            input.use { stream -> source.outputStream().use { target ->
                val buffer = ByteArray(8192)
                var copied = 0L
                while (true) {
                    val count = stream.read(buffer)
                    if (count == -1) break
                    copied += count
                    require(copied <= 25L * 1024 * 1024) { "Ukuran gambar maksimal 25 MB." }
                    target.write(buffer, 0, count)
                }
            } }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Format gambar tidak didukung atau berkas rusak." }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2600 ||
                bounds.outWidth.toLong() / sample * (bounds.outHeight / sample) > 6_000_000L) sample *= 2
            val bitmap = requireNotNull(BitmapFactory.decodeFile(source.path,
                BitmapFactory.Options().apply { inSampleSize = sample })) { "Gambar gagal dibaca." }
            try {
                val exif = ExifInterface(source)
                val matrix = Matrix().apply {
                    if (exif.isFlipped) postScale(-1f, 1f)
                    postRotate(exif.rotationDegrees.toFloat())
                }
                val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                try {
                    output.outputStream().use {
                        check(upright.compress(Bitmap.CompressFormat.JPEG, 95, it)) { "Gambar gagal disiapkan." }
                    }
                } finally { if (upright !== bitmap) upright.recycle() }
            } finally { bitmap.recycle() }
            return output.absolutePath
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally { source.delete() }
    }

    fun rotate(path: String): String {
        require(File(path).canonicalFile.parentFile == drafts.canonicalFile)
        val source = requireNotNull(BitmapFactory.decodeFile(path)) { "Pratinjau tidak tersedia. Pilih ulang gambar." }
        val output = File(drafts, "${UUID.randomUUID()}.jpg")
        try {
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height,
                Matrix().apply { postRotate(90f) }, true)
            try { output.outputStream().use { check(rotated.compress(Bitmap.CompressFormat.JPEG, 95, it)) } }
            finally { if (rotated !== source) rotated.recycle() }
            return output.absolutePath
        } catch (error: Throwable) { output.delete(); throw error }
        finally { source.recycle() }
    }

    fun persist(draftPath: String): String {
        require(File(draftPath).canonicalFile.parentFile == drafts.canonicalFile)
        val name = "${UUID.randomUUID()}.jpg"
        val output = File(receipts, name)
        try { File(draftPath).copyTo(output) }
        catch (error: Throwable) { output.delete(); throw error }
        // Relatif terhadap filesDir, tetap valid setelah backup/restore perangkat.
        return "receipts/$name"
    }

    fun deletePersisted(relativePath: String) {
        val file = File(context.filesDir, relativePath).canonicalFile
        if (file.parentFile == receipts.canonicalFile) file.delete()
    }

    /** Baca salinan tersimpan, bukan draf scan atau URI galeri. Panggil pada Dispatchers.IO. */
    fun readPersistedBitmap(relativePath: String): Bitmap? {
        val file = File(context.filesDir, relativePath).canonicalFile
        if (file.parentFile != receipts.canonicalFile || !file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2600 ||
            bounds.outWidth.toLong() / sample * (bounds.outHeight / sample) > 6_000_000L) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = sample
        })
    }

    fun deleteDraft(path: String?) {
        if (path == null) return
        val file = File(path).canonicalFile
        if (file.parentFile == drafts.canonicalFile) file.delete()
    }

    fun cleanOrphans(referencedPaths: List<String>) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        receipts.listFiles()?.filter {
            it.lastModified() < cutoff && "receipts/${it.name}" !in referencedPaths
        }?.forEach { it.delete() }
        // Draf lama dibersihkan setelah satu hari; draf aktif dapat dipilih ulang jika proses mati.
        listOf(drafts, camera).forEach { directory ->
            directory.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
        }
    }
}
