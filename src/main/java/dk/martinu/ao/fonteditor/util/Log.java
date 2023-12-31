/*
 * Copyright (c) 2023, Adam Martinu. All rights reserved. Altering or
 * removing copyright notices or this file header is not allowed.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package dk.martinu.ao.fonteditor.util;

import org.jetbrains.annotations.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simple logger implementation. Can log messages with the following levels of
 * verbosity, ordered from least to most:
 * <ol>
 *     <li>{@link #e(String, Object...) Error}</li>
 *     <li>{@link #w(String, Object...) Warning}</li>
 *     <li>{@link #d(String, Object...) Debug}</li>
 *     <li>{@link #i(String, Object...) Info}</li>
 * </ol>
 * Each method is overloaded to accept an optional {@code Throwable} parameter,
 * and can also accept an array of parameters to
 * {@link Formatter#format(String, Object...) format} the message. This
 * implementation is threadsafe and can be used concurrently.
 *
 * @author Adam Martinu
 * @version 1.0, 2023-02-09
 * @since 1.0
 */
public final class Log {

    /**
     * Daemon thread to take added log records and post them.
     */
    private static final LogThread thread = new LogThread();
    /**
     * Blocking queue to hold added log records.
     */
    private static final LinkedBlockingQueue<Object> records = new LinkedBlockingQueue<>();

    static {
        // start daemon thread when Log is loaded
        thread.start();
    }

    /**
     * Logs a debug message.
     *
     * @param msg    the message
     * @param params optional parameters for formatting the message
     */
    public static void d(@NotNull String msg, @Nullable Object... params) {
        records.add(new Record(Level.DEBUG, msg, null, params));
    }

    /**
     * Logs a debug message.
     *
     * @param msg    the message
     * @param ex     the throwable related to the message, or {@code null}
     * @param params optional parameters for formatting the message
     */
    public static void d(@NotNull String msg, @Nullable Throwable ex, @Nullable Object... params) {
        records.add(new Record(Level.DEBUG, msg, ex, params));
    }

    /**
     * Logs an error message.
     *
     * @param msg    the message
     * @param params optional parameters for formatting the message
     */
    public static void e(@NotNull String msg, @NotNull Object... params) {
        records.add(new Record(Level.ERROR, msg, null, params));
    }

    /**
     * Logs an error message.
     *
     * @param msg    the message
     * @param ex     the throwable related to the message, or {@code null}
     * @param params optional parameters for formatting the message
     */
    public static void e(@NotNull String msg, @Nullable Throwable ex, @Nullable Object... params) {
        records.add(new Record(Level.ERROR, msg, ex, params));
    }

    /**
     * Logs an info message.
     *
     * @param msg    the message
     * @param params optional parameters for formatting the message
     */
    public static void i(@NotNull String msg, @NotNull Object... params) {
        records.add(new Record(Level.INFO, msg, null, params));
    }

    /**
     * Logs an info message.
     *
     * @param msg    the message
     * @param ex     the throwable related to the message, or {@code null}
     * @param params optional parameters for formatting the message
     */
    public static void i(@NotNull String msg, @Nullable Throwable ex, @Nullable Object... params) {
        records.add(new Record(Level.INFO, msg, ex, params));
    }

    /**
     * Logs a warning message.
     *
     * @param msg    the message
     * @param params optional parameters for formatting the message
     */
    public static void w(@NotNull String msg, @NotNull Object... params) {
        records.add(new Record(Level.WARNING, msg, null, params));
    }

    /**
     * Logs a warning message.
     *
     * @param msg    the message
     * @param ex     the throwable related to the message, or {@code null}
     * @param params optional parameters for formatting the message
     */
    public static void w(@NotNull String msg, @Nullable Throwable ex, @Nullable Object... params) {
        records.add(new Record(Level.WARNING, msg, ex, params));
    }

    /**
     * Enum that describes the verbosity of a log record.
     */
    private enum Level {
        DEBUG, INFO, WARNING, ERROR,
    }

    /**
     * Daemon thread implementation that takes added log records and prints
     * them to the standard output stream ({@code System.out}).
     */
    private static final class LogThread extends Thread {

        LogThread() {
            setDaemon(true);
        }

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            while (true) {
                try {
                    System.out.println(records.take());
                }
                catch (InterruptedException e) {
                    Log.e("LogThread was interrupted", e);
                }
            }
        }
    }

    /**
     * Data class for storing information about a log record. The string
     * representation of a log record is not created until {@code toString()}
     * is called.
     *
     * @param when       millisecond timestamp of when the record was created
     * @param threadName the name of the thread this record was created on
     * @param level      the level of verbosity of this record
     * @param msg        the log message
     * @param ex         a throwable object related to the log message, can be
     *                   {@code null}
     * @param params     an array of parameters for formatting the log message.
     *                   Can
     *                   be {@code null} and can also contain {@code null}
     *                   elements
     */
    private record Record(
            long when,
            @NotNull String threadName,
            @NotNull Level level,
            @NotNull String msg,
            @Nullable Throwable ex,
            @Nullable Object[] params) {

        /**
         * Default date pattern. An example date would look like
         * <pre>
         *     23-02-09T07:51:43,476+01
         * </pre>
         */
        private static final String DEFAULT_DATE_PATTERN = "yy-MM-dd'T'hh:mm:ss,SSSX";
        /**
         * Date formatter using the {@link #DEFAULT_DATE_PATTERN default} date
         * pattern.
         */
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_PATTERN);

        /**
         * Creates a new log record.
         *
         * @throws NullPointerException if {@code level} or {@code msg} is
         *                              {@code null}
         */
        Record(@NotNull Level level, @NotNull String msg, @Nullable Throwable ex, @Nullable Object... params) {
            this(
                    System.currentTimeMillis(),
                    '[' + Thread.currentThread().getName() + ']',
                    Objects.requireNonNull(level, "level is null"),
                    Objects.requireNonNull(msg, "msg is null"),
                    ex,
                    params);
        }

        /**
         * Creates and returns a string representation of this log record.
         */
        @Contract(value = "-> new", pure = true)
        @NotNull
        @Override
        public String toString() {
            MutableString string = new MutableString(256);
            string.add(dateFormat.format(when))
                    .append(' ')
                    .add(threadName)
                    .append(' ')
                    .add(level.name())
                    .append(' ');
            if (params != null) {
                string.add(new Formatter((Locale) null).format(msg, params));
            }
            else {
                string.add(msg);
            }
            if (ex != null) {
                string.add(ex.toString());
            }
            return string.toString();
        }
    }
}
