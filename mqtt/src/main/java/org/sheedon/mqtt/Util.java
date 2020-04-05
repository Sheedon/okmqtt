package org.sheedon.mqtt;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * 基本工具类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/11 21:00
 */
public final class Util {

    public static <T> T checkNotNull(@Nullable T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }

        if (object instanceof String && ((String) object).isEmpty()) {
            throw new NullPointerException(message);
        }
        return object;
    }

    /**
     * Returns a {@link Locale#US} formatted {@link String}.
     */
    public static String format(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }

}
