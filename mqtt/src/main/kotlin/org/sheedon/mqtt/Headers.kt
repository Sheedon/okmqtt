package org.sheedon.mqtt

import android.annotation.SuppressLint
import java.lang.String.format
import java.util.*

/**
 * mqtt调度库的头文件
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/7 10:52 下午
 */
class Headers private constructor(
    private val namesAndValues: Array<String>
) : Iterable<Pair<String, String>>{

    /** Returns the last value corresponding to the specified field, or null. */
    operator fun get(name: String): String? = get(namesAndValues, name)

    @get:JvmName("size") val size: Int
        get() = namesAndValues.size / 2

    @JvmName("-deprecated_size")
    @Deprecated(
        message = "moved to val",
        replaceWith = ReplaceWith(expression = "size"),
        level = DeprecationLevel.ERROR)
    fun size(): Int = size

    /** Returns the field at `position`. */
    fun name(index: Int): String = namesAndValues[index * 2]

    /** Returns the value at `index`. */
    fun value(index: Int): String = namesAndValues[index * 2 + 1]


    /** Returns an immutable case-insensitive set of header names. */
    fun names(): Set<String> {
        val result = TreeSet(String.CASE_INSENSITIVE_ORDER)
        for (i in 0 until size) {
            result.add(name(i))
        }
        return Collections.unmodifiableSet(result)
    }

    /** Returns an immutable list of the header values for `name`. */
    fun values(name: String): List<String> {
        var result: MutableList<String>? = null
        for (i in 0 until size) {
            if (name.equals(name(i), ignoreCase = true)) {
                if (result == null) result = ArrayList(2)
                result.add(value(i))
            }
        }
        return if (result != null) {
            Collections.unmodifiableList(result)
        } else {
            emptyList()
        }
    }

    override operator fun iterator(): Iterator<Pair<String, String>> {
        return Array(size) { name(it) to value(it) }.iterator()
    }

    fun newBuilder(): Builder {
        val result = Builder()
        result.namesAndValues += namesAndValues
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is Headers && namesAndValues.contentEquals(other.namesAndValues)
    }

    override fun toString(): String {
        return buildString {
            for (i in 0 until size) {
                append(name(i))
                append(": ")
                append(value(i))
                append("\n")
            }
        }
    }

    fun toMultimap(): Map<String, List<String>> {
        val result = TreeMap<String, MutableList<String>>(String.CASE_INSENSITIVE_ORDER)
        for (i in 0 until size) {
            val name = name(i).lowercase(Locale.US)
            var values: MutableList<String>? = result[name]
            if (values == null) {
                values = ArrayList(2)
                result[name] = values
            }
            values.add(value(i))
        }
        return result
    }

    class Builder{
        internal val namesAndValues: MutableList<String> = ArrayList(20)

        /** Add an header line containing a field name, a literal colon, and a value. */
        fun add(line: String) = apply {
            val index = line.indexOf(':')
            require(index != -1) { "Unexpected header: $line" }
            add(line.substring(0, index).trim(), line.substring(index + 1))
        }

        /**
         * Add a header with the specified name and value. Does validation of header names and values.
         */
        fun add(name: String, value: String) = apply {
            checkName(name)
            checkValue(value, name)
            addLenient(name, value)
        }

        /**
         * Add a header with the specified name and value. Does validation of header names, allowing
         * non-ASCII values.
         */
        fun addUnsafeNonAscii(name: String, value: String) = apply {
            checkName(name)
            addLenient(name, value)
        }

        /**
         * Adds all headers from an existing collection.
         */
        fun addAll(headers: Headers) = apply {
            for (i in 0 until headers.size) {
                addLenient(headers.name(i), headers.value(i))
            }
        }

        /**
         * Add a field with the specified value without any validation. Only appropriate for headers
         * from the remote peer or cache.
         */
        internal fun addLenient(name: String, value: String) = apply {
            namesAndValues.add(name)
            namesAndValues.add(value.trim())
        }

        fun removeAll(name: String) = apply {
            var i = 0
            while (i < namesAndValues.size) {
                if (name.equals(namesAndValues[i], ignoreCase = true)) {
                    namesAndValues.removeAt(i) // name
                    namesAndValues.removeAt(i) // value
                    i -= 2
                }
                i += 2
            }
        }

        /**
         * Set a field with the specified value. If the field is not found, it is added. If the field is
         * found, the existing values are replaced.
         */
        operator fun set(name: String, value: String) = apply {
            checkName(name)
            checkValue(value, name)
            removeAll(name)
            addLenient(name, value)
        }

        /** Equivalent to `build().get(name)`, but potentially faster. */
        operator fun get(name: String): String? {
            for (i in namesAndValues.size - 2 downTo 0 step 2) {
                if (name.equals(namesAndValues[i], ignoreCase = true)) {
                    return namesAndValues[i + 1]
                }
            }
            return null
        }

        fun build(): Headers = Headers(namesAndValues.toTypedArray())
    }

    companion object{
        private fun get(namesAndValues: Array<String>, name: String): String? {
            for (i in namesAndValues.size - 2 downTo 0 step 2) {
                if (name.equals(namesAndValues[i], ignoreCase = true)) {
                    return namesAndValues[i + 1]
                }
            }
            return null
        }


        /**
         * Returns headers for the alternating header names and values. There must be an even number of
         * arguments, and they must alternate between header names and values.
         */
        @JvmStatic
        @JvmName("of")
        fun headersOf(vararg namesAndValues: String): Headers {
            require(namesAndValues.size % 2 == 0) { "Expected alternating header names and values" }

            // Make a defensive copy and clean it up.
            val namesAndValues: Array<String> = namesAndValues.clone() as Array<String>
            for (i in namesAndValues.indices) {
                require(namesAndValues[i] != null) { "Headers cannot be null" }
                namesAndValues[i] = namesAndValues[i].trim()
            }

            // Check for malformed headers.
            for (i in namesAndValues.indices step 2) {
                val name = namesAndValues[i]
                val value = namesAndValues[i + 1]
                checkName(name)
                checkValue(value, name)
            }

            return Headers(namesAndValues)
        }

        @JvmName("-deprecated_of")
        @Deprecated(
            message = "function name changed",
            replaceWith = ReplaceWith(expression = "headersOf(*namesAndValues)"),
            level = DeprecationLevel.ERROR)
        fun of(vararg namesAndValues: String): Headers {
            return headersOf(*namesAndValues)
        }

        /** Returns headers for the header names and values in the [Map]. */
        @JvmStatic
        @JvmName("of")
        fun Map<String, String>.toHeaders(): Headers {
            // Make a defensive copy and clean it up.
            val namesAndValues = arrayOfNulls<String>(size * 2)
            var i = 0
            for ((k, v) in this) {
                val name = k.trim()
                val value = v.trim()
                checkName(name)
                checkValue(value, name)
                namesAndValues[i] = name
                namesAndValues[i + 1] = value
                i += 2
            }

            return Headers(namesAndValues as Array<String>)
        }

        @JvmName("-deprecated_of")
        @Deprecated(
            message = "function moved to extension",
            replaceWith = ReplaceWith(expression = "headers.toHeaders()"),
            level = DeprecationLevel.ERROR)
        fun of(headers: Map<String, String>): Headers {
            return headers.toHeaders()
        }

        @SuppressLint("DefaultLocale")
        private fun checkName(name: String) {
            require(name.isNotEmpty()) { "name is empty" }
            for (i in name.indices) {
                val c = name[i]
                require(c in '\u0021'..'\u007e') {
                    format("Unexpected char %#04x at %d in header name: %s", c.code, i, name)
                }
            }
        }

        @SuppressLint("DefaultLocale")
        private fun checkValue(value: String, name: String) {
            for (i in value.indices) {
                val c = value[i]
                require(c == '\t' || c in '\u0020'..'\u007e') {
                    format("Unexpected char %#04x at %d in %s value: %s", c.code, i, name, value)
                }
            }
        }
    }
}