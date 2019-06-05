package com.patatos.sac.anoca.cards

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.text.Html
import android.text.Spanned
import android.text.SpannedString

import com.patatos.sac.anoca.MainActivity
import com.patatos.sac.anoca.R

import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.min

import kotlin.random.Random

class Content(private val activity: MainActivity, private val raw: List<String>, val categoryName: String) {

    private fun isOnline(id: Int): Boolean {
        return WEB_PRF.any { prf -> this.raw[id].startsWith(prf, true) }
    }

    private fun <T> getOnline(id: Int, processor: (InputStream) -> T): T? {
        var r: T? = null

        val ex = Executors.newSingleThreadExecutor()
        ex.execute {
            r = processor(URL(this.raw[id]).openConnection().inputStream)
            ex.shutdown()
        }
        ex.awaitTermination(20, TimeUnit.SECONDS)

        return r
    }

    fun isImage(id: Int): Boolean {
        return IMG_SUF.any { suf -> this.raw[id].endsWith(suf, true) }
    }

    fun isText(id: Int): Boolean {
        return !this.isImage(id)
    }

    private fun getRaw(id: Int): String {
        return if (this.isOnline(id)) {
            this.getOnline(id) {
                    stream -> stream.reader().use { it.readText() }
            } ?: "Could not get online resource..."
        } else this.raw[id]
    }

    // TODO: ratio `r` not accounted for
    private fun cropBitmapToDrawable(b: Bitmap?, r: Float = 1f): Drawable? {
        if (b == null)
            return null

        val size = min(b.width, b.height)
        val xOff = (b.width - size) / 2
        val yOff = (b.height - size) / 2

        return BitmapDrawable(this.activity.resources, Bitmap.createBitmap(b, xOff, yOff, size, size))
    }

    fun getImage(id: Int): Drawable? {
        return if (this.isOnline(id)) {
            this.getOnline(id) {
                    stream -> this.cropBitmapToDrawable(BitmapFactory.decodeStream(stream))
            } ?: ContextCompat.getDrawable(this.activity, R.drawable.no_resource)
        } else this.cropBitmapToDrawable(BitmapFactory.decodeFile(this.raw[id]))
            ?: ContextCompat.getDrawable(this.activity, R.drawable.no_resource)
    }

    fun getText(id: Int): String {
        return CustomCardParse(this.getRaw(id), true).toString()
    }

    fun getSpanned(id: Int): Spanned {
        return this.getRaw(id).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(CustomCardParse(it).toMarkup(), 0)
            else SpannedString(CustomCardParse(it, true).toString())
        }
    }

    fun getActivity(): MainActivity {
        return this.activity
    }

    fun count(): Int {
        return this.raw.count()
    }

    fun random(): Int {
        return Random.nextInt(this.raw.count())
    }

    fun saveRaw(): Array<String> {
        return this.raw.toTypedArray()
    }

    companion object {

        val WEB_PRF = arrayOf("http:", "https:")
        val IMG_SUF = arrayOf(".png", ".jpg", ".jpeg", ".gif")

    }

}
