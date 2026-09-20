package com.revisepdf.app.data.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

data class PageProgress(val pageIndex: Int, val totalPages: Int)

class PdfProcessor(context: Context) {
    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun extractPages(file: File, onProgress: (PageProgress) -> Unit): List<String> {
        val document = PDDocument.load(file)
        try {
            val totalPages = document.numberOfPages
            val stripper = PDFTextStripper()
            val pages = ArrayList<String>(totalPages)
            for (pageNumber in 1..totalPages) {
                stripper.startPage = pageNumber
                stripper.endPage = pageNumber
                pages.add(stripper.getText(document))
                onProgress(PageProgress(pageNumber - 1, totalPages))
            }
            return pages
        } finally {
            document.close()
        }
    }
}
