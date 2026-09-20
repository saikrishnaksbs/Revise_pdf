package com.revisepdf.app.data.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File

class PageRenderer {

    fun renderPageToJpeg(file: File, pageIndex: Int, targetWidth: Int = TARGET_WIDTH): ByteArray? {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (pageIndex !in 0 until renderer.pageCount) return null
                renderer.openPage(pageIndex).use { page ->
                    val height = (page.height * (targetWidth.toFloat() / page.width)).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                    // PdfRenderer leaves untouched areas transparent, which JPEG flattens to black.
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                    bitmap.recycle()
                    return output.toByteArray()
                }
            }
        }
    }

    private companion object {
        const val TARGET_WIDTH = 1024
        const val JPEG_QUALITY = 85
    }
}
