@file:Suppress("DEPRECATION")
package com.jamal2367.styx.preference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.AsyncTask
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import com.jamal2367.styx.R
import com.jamal2367.styx.locale.LocaleManager
import com.jamal2367.styx.locale.Locales.parseLocaleCode
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.text.Collator
import java.util.*

class LocaleListPreference @JvmOverloads constructor(
    context: Context?,
    attributes: AttributeSet? = null
) : ListPreference(context, attributes) {
    companion object {
        private const val LOG_TAG = "GeckoLocaleList"
        private val languageCodeToNameMap: MutableMap<String, String> = HashMap()
        private val localeToNameMap: MutableMap<String, String> = HashMap()

        init {
            // Only ICU 57 actually contains the Asturian name for Asturian, even Android 7.1 is still
            // shipping with ICU 56, so we need to override the Asturian name (otherwise displayName will
            // be the current locales version of Asturian, see:
            // https://github.com/mozilla-mobile/focus-android/issues/634#issuecomment-303886118
            languageCodeToNameMap["ast"] = "Asturianu"
            // On an Android 8.0 device those languages are not known and we need to add the names
            // manually. Loading the resources at runtime works without problems though.
            languageCodeToNameMap["cak"] = "Kaqchikel"
            languageCodeToNameMap["ia"] = "Interlingua"
            languageCodeToNameMap["meh"] = "Tu´un savi ñuu Yasi'í Yuku Iti"
            languageCodeToNameMap["mix"] = "Tu'un savi"
            languageCodeToNameMap["trs"] = "Triqui"
            languageCodeToNameMap["zam"] = "DíɁztè"
            languageCodeToNameMap["oc"] = "occitan"
            languageCodeToNameMap["an"] = "Aragonés"
            languageCodeToNameMap["tt"] = "татарча"
            languageCodeToNameMap["wo"] = "Wolof"
            languageCodeToNameMap["anp"] = "अंगिका"
            languageCodeToNameMap["ixl"] = "Ixil"
            languageCodeToNameMap["pai"] = "Paa ipai"
            languageCodeToNameMap["quy"] = "Chanka Qhichwa"
            languageCodeToNameMap["ay"] = "Aimara"
            languageCodeToNameMap["quc"] = "K'iche'"
            languageCodeToNameMap["tsz"] = "P'urhepecha"
            languageCodeToNameMap["jv"] = "Basa Jawa"
            languageCodeToNameMap["ppl"] = "Náhuat Pipil"
            languageCodeToNameMap["su"] = "Basa Sunda"
            languageCodeToNameMap["hus"] = "Tének"
            languageCodeToNameMap["co"] = "Corsu"
            languageCodeToNameMap["sn"] = "ChiShona"
        }

        init {
            // Override the native name for certain locale regions based on language community needs.
            localeToNameMap["zh-CN"] = "中文 (中国大陆)"
        }
    }

    /**
     * With thanks to <http:></http:>//stackoverflow.com/a/22679283/22003> for the
     * initial solution.
     *
     *
     * This class encapsulates an approach to checking whether a script
     * is usable on a device. We attempt to draw a character from the
     * script (e.g., ব). If the fonts on the device don't have the correct
     * glyph, Android typically renders whitespace (rather than .notdef).
     *
     *
     * Pass in part of the name of the locale in its local representation,
     * and a whitespace character; this class performs the graphical comparison.
     *
     *
     * See Bug 1023451 Comment 24 for extensive explanation.
     */
    class CharacterValidator(missing: String) {
        private val paint = Paint()
        private val missingCharacter: ByteArray
        private fun drawBitmap(text: String): Bitmap {
            val b = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ALPHA_8)
            val c = Canvas(b)
            c.drawText(text, 0f, (BITMAP_HEIGHT / 2).toFloat(), paint)
            return b
        }

        fun characterIsMissingInFont(ch: String): Boolean {
            val rendered = getPixels(drawBitmap(ch))
            return rendered.contentEquals(missingCharacter)
        }

        companion object {
            private const val BITMAP_WIDTH = 32
            private const val BITMAP_HEIGHT = 48
            private fun getPixels(b: Bitmap): ByteArray {
                val byteCount = b.allocationByteCount
                val buffer = ByteBuffer.allocate(byteCount)
                try {
                    b.copyPixelsToBuffer(buffer)
                } catch (e: RuntimeException) {
                    // Android throws this if there's not enough space in the buffer.
                    // This should never occur, but if it does, we don't
                    // really care -- we probably don't need the entire image.
                    // This is awful. I apologize.
                    if ("Buffer not large enough for pixels" == e.message) {
                        return buffer.array()
                    }
                    throw e
                }
                return buffer.array()
            }
        }

        init {
            missingCharacter = getPixels(drawBitmap(missing))
        }
    }

    @Volatile
    private var entriesLocale: Locale? = null
    private var characterValidator: CharacterValidator? = null
    private var buildLocaleListTask: BuildLocaleListTask? = null

    interface EntriesListener {
        fun onEntriesSet()
    }

    private var entriesListener: EntriesListener? = null
    fun setEntriesListener(entriesListener: EntriesListener) {
        if (entryValues != null) {
            entriesListener.onEntriesSet()
        } else {
            this.entriesListener = entriesListener
        }
    }

    override fun onAttached() {
        super.onAttached()
        // Thus far, missing glyphs are replaced by whitespace, not a box
        // or other Unicode codepoint.
        characterValidator = CharacterValidator(" ")
        initializeLocaleList()
    }

    @Suppress("DEPRECATION")
    private fun initializeLocaleList() {
        val currentLocale = Locale.getDefault()
        Log.d(LOG_TAG, "Building locales list. Current locale: $currentLocale")
        if (currentLocale == entriesLocale && entries != null) {
            Log.v(LOG_TAG, "No need to build list.")
            return
        }
        entriesLocale = currentLocale
        val defaultLanguage = context.getString(R.string.language_system_default)
        buildLocaleListTask = BuildLocaleListTask(
            this, defaultLanguage,
            characterValidator, LocaleManager.getPackagedLocaleTags()
        )
        buildLocaleListTask!!.execute()
    }

    @Suppress("DEPRECATION")
    override fun onPrepareForRemoval() {
        super.onPrepareForRemoval()
        if (buildLocaleListTask != null) {
            buildLocaleListTask!!.cancel(true)
        }
        if (entriesListener != null) {
            entriesListener = null
        }
    }

    private class LocaleDescriptor(locale: Locale, val tag: String) : Comparable<LocaleDescriptor> {
        var displayName: String

        constructor(tag: String) : this(parseLocaleCode(tag), tag)

        override fun toString(): String {
            return displayName
        }

        override fun equals(other: Any?): Boolean {
            return other is LocaleDescriptor && compareTo(other) == 0
        }

        override fun hashCode(): Int {
            return tag.hashCode()
        }

        override fun compareTo(other: LocaleDescriptor): Int {
            // We sort by name, so we use Collator.
            return COLLATOR.compare(displayName, other.displayName)
        }

        /**
         * See Bug 1023451 Comment 10 for the research that led to
         * this method.
         *
         * @return true if this locale can be used for displaying UI
         * on this device without known issues.
         */
        fun isUsable(validator: CharacterValidator?): Boolean {
            // Oh, for Java 7 switch statements.
            if (tag == "bn-IN") {
                // Bengali sometimes has an English label if the Bengali script
                // is missing. This prevents us from simply checking character
                // rendering for bn-IN; we'll get a false positive for "B", not "ব".
                //
                // This doesn't seem to affect other Bengali-script locales
                // (below), which always have a label in native script.
                if (!displayName.startsWith("বাংলা")) {
                    // We're on an Android version that doesn't even have
                    // characters to say বাংলা. Definite failure.
                    return false
                }
            }

            // These locales use a script that is often unavailable
            // on common Android devices. Make sure we can show them.
            // See documentation for CharacterValidator.
            // Note that bn-IN is checked here even if it passed above.
            return tag != "or" &&
                    tag != "my" &&
                    tag != "pa-IN" &&
                    tag != "gu-IN" &&
                    tag != "bn-IN" || !validator!!.characterIsMissingInFont(
                displayName.substring(
                    0,
                    1
                )
            )
        }

        companion object {
            // We use Locale.US here to ensure a stable ordering of entries.
            private val COLLATOR = Collator.getInstance(Locale.US)
        }

        init {
            val displayName: String?
            displayName =
                when {
                    languageCodeToNameMap.containsKey(locale.language) -> {
                        languageCodeToNameMap[locale.language]
                    }
                    localeToNameMap.containsKey(locale.toLanguageTag()) -> {
                        localeToNameMap[locale.toLanguageTag()]
                    }
                    else -> {
                        locale.getDisplayName(locale)
                    }
                }
            if (TextUtils.isEmpty(displayName)) {
                // There's nothing sane we can do.
                Log.w(LOG_TAG, "Display name is empty. Using $locale")
                this.displayName = locale.toString()
            }

            // For now, uppercase the first character of LTR locale names.
            // This is pretty much what Android does. This is a reasonable hack
            // for Bug 1014602, but it won't generalize to all locales.
            val directionality = Character.getDirectionality(displayName!![0])
            if (directionality == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                var firstLetter = displayName.substring(0, 1)

                // Android OS creates an instance of Transliterator to convert the first letter
                // of the Greek locale. See CaseMapper.toUpperCase(Locale locale, String s, int count)
                // Since it's already in upper case, we don't need it
                if (!Character.isUpperCase(firstLetter[0])) {
                    firstLetter = firstLetter.uppercase(locale)
                }
                this.displayName = firstLetter + displayName.substring(1)
            }
            this.displayName = displayName
        }
    }

    override fun onClick() {
        super.onClick()

        // Use this hook to try to fix up the environment ASAP.
        // Do this so that the redisplayed fragment is inflated
        // with the right locale.
        val selectedLocale = selectedLocale
        val context = context
        LocaleManager.getInstance().updateConfiguration(context, selectedLocale)
    }

    private val selectedLocale: Locale
        get() {
            val tag = value
            return if (tag == null || tag == "") {
                Locale.getDefault()
            } else parseLocaleCode(tag)
        }

    override fun getSummary(): CharSequence {
        val value = value
        return if (TextUtils.isEmpty(value)) {
            context.getString(R.string.language_system_default)
        } else LocaleDescriptor(value).displayName

        // We can't trust super.getSummary() across locale changes,
        // apparently, so let's do the same work.
    }

    @Suppress("DEPRECATION")
    internal class BuildLocaleListTask(
        listPreference: LocaleListPreference,
        private val systemDefaultLanguage: String,
        private val characterValidator: CharacterValidator?,
        private val shippingLocales: Collection<String>
    ) : AsyncTask<Void?, Void?, Pair<Array<String?>, Array<String?>>>() {
        private val weakListPreference: WeakReference<LocaleListPreference> = WeakReference(listPreference)
        override fun doInBackground(vararg params: Void?): Pair<Array<String?>, Array<String?>> {
            val descriptors = usableLocales
            val count = descriptors.size

            // We leave room for "System default".
            val entries = arrayOfNulls<String>(count + 1)
            val values = arrayOfNulls<String>(count + 1)
            entries[0] = systemDefaultLanguage
            values[0] = ""
            for (i in 0 until count) {
                val displayName = descriptors[i].displayName
                val tag = descriptors[i].tag
                Log.v(LOG_TAG, "$displayName => $tag")
                entries[i + 1] = displayName
                values[i + 1] = tag
            }
            return Pair(entries, values)
        }

        /**
         * Not every locale we ship can be used on every device, due to
         * font or rendering constraints.
         *
         *
         * This method filters down the list before generating the descriptor array.
         */
        private val usableLocales: Array<LocaleDescriptor>
            get() {
                val initialCount = shippingLocales.size
                val locales: MutableSet<LocaleDescriptor> = HashSet(initialCount)
                for (tag in shippingLocales) {
                    val descriptor = LocaleDescriptor(tag)
                    if (!descriptor.isUsable(characterValidator)) {
                        Log.w(LOG_TAG, "Skipping locale $tag on this device.")
                        continue
                    }
                    locales.add(descriptor)
                }
                val usableCount = locales.size
                val descriptors = locales.toTypedArray()
                Arrays.sort(descriptors, 0, usableCount)
                return descriptors
            }

        @Suppress("DEPRECATION")
        override fun onPostExecute(pair: Pair<Array<String?>, Array<String?>>) {
            if (isCancelled) {
                return
            }
            val preference = weakListPreference.get()
            if (preference != null) {
                preference.entries = pair.first
                preference.entryValues = pair.second
                if (preference.entriesListener != null) {
                    preference.entriesListener!!.onEntriesSet()
                }
            }
        }

    }
}