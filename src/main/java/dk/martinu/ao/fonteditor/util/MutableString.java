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

import java.util.Arrays;

/**
 * @author Adam Martinu
 * @version 1.1 2023-12-31
 */
public class MutableString implements Appendable, CharSequence {

    private static final int DEFAULT_LENGTH = 128;
    private static final String NULL = "null";

    public char[] chars;
    public int index = 0;

    @Contract(pure = true)
    public MutableString(int capacity) {
        chars = new char[capacity];
    }

    @Contract(pure = true)
    public MutableString() {
        chars = new char[DEFAULT_LENGTH];
    }

    @Contract(pure = true)
    public MutableString(@Nullable CharSequence csq, int start, int end) throws IndexOutOfBoundsException {
        if (csq == null) {
            csq = NULL;
        }
        if (end < 0 || end > csq.length()) {
            throw new IndexOutOfBoundsException("end is out of bounds {" + start + "}");
        }
        if (start < 0 || start > end) {
            throw new IndexOutOfBoundsException("start is out of bounds {" + start + "}");
        }
        int csqLen = end - start;
        int len = DEFAULT_LENGTH;
        while (csqLen > len) {
            len <<= 1;
        }
        chars = new char[len];
        for (int i = start; i < end; i++) {
            chars[index++] = csq.charAt(i);
        }
    }

    @Contract(value = "_ -> this", mutates = "this")
    @NotNull
    public MutableString add(@Nullable CharSequence csq) {
        return append(csq);
    }

//    public MutableString add(CharSequence csq, int start, int end) throws IndexOutOfBoundsException {
//        return append(csq, start, end);
//    }

//    public MutableString add(char c) {
//        return append(c);
//    }

//    public MutableString add(char... csq) {
//        return append(csq);
//    }

    @Contract(value = "_ -> this", mutates = "this")
    @NotNull
    public MutableString add(@Nullable Object o) {
        return append(String.valueOf(o));
    }

//    @Contract(value = "_ -> this", mutates = "this")
//    @NotNull
//    public MutableString append(char... csq) {
//        if (csq == null) {
//            csq = NULL.toCharArray();
//        }
//        ensureCapacity(index + csq.length);
//        for (char c : csq) {
//            chars[index++] = c;
//        }
//        return this;
//    }

    @Contract(value = "_ -> this", mutates = "this")
    @Override
    @NotNull
    public MutableString append(@Nullable CharSequence csq) {
        if (csq == null) {
            csq = NULL;
        }
        int len = csq.length();
        ensureCapacity(index + len);
        for (int i = 0; i < len; i++) {
            chars[index++] = csq.charAt(i);
        }
        return this;
    }

    @Contract(value = "_, _, _ -> this", mutates = "this")
    @Override
    @NotNull
    public MutableString append(@Nullable CharSequence csq, int start, int end) throws IndexOutOfBoundsException {
        if (csq == null) {
            csq = NULL;
        }
        if (end < 0 || end > csq.length()) {
            throw new IndexOutOfBoundsException("end is out of bounds {" + start + "}");
        }
        if (start < 0 || start > end) {
            throw new IndexOutOfBoundsException("start is out of bounds {" + start + "}");
        }
        int size = end - start;
        ensureCapacity(index + size);
        for (int i = 0; i < size; i++) {
            chars[index++] = csq.charAt(i + start);
        }
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    @Override
    @NotNull
    public MutableString append(char c) {
        ensureCapacity(index + 1);
        chars[index++] = c;
        return this;
    }

//    public int available() {
//        return chars.length - index;
//    }

    @Override
    public char charAt(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= this.index) {
            throw new IndexOutOfBoundsException(index);
        }
        return chars[index];
    }

    public void clear() {
        index = 0;
    }

//    public void delete(int from) throws IndexOutOfBoundsException {
//        if (from < 0 || from > index) {
//            throw new IndexOutOfBoundsException(from);
//        }
//        index = from;
//    }

//    public void delete(int from, int to) throws IndexOutOfBoundsException {
//        if (from < 0) { throw new IndexOutOfBoundsException(from); }
//        if (to < 0) { throw new IndexOutOfBoundsException(to); }
//        if (from > to) { throw new IndexOutOfBoundsException(from); }
//        if (to > index) { throw new IndexOutOfBoundsException(to); }
//        for (int i = from, k = to; i < to && k < index; i++, k++) {
//            chars[i] = chars[k];
//        }
//        index -= (to - from);
//    }

    public void ensureCapacity(int capacity) {
        if (capacity > chars.length) {
            int newLength = chars.length << 1;
            while (capacity > newLength) {
                newLength <<= 1;
            }
            chars = Arrays.copyOf(chars, newLength);
        }
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        else if (o instanceof MutableString ms) {
            if (index != ms.index) {
                return false;
            }
            for (int i = 0; i < index; i++) {
                if (chars[i] != ms.chars[i]) {
                    return false;
                }
            }
            return true;
        }
        else if (o instanceof CharSequence csq) {
            if (index != csq.length()) {
                return false;
            }
            for (int i = 0; i < index; i++) {
                if (chars[i] != csq.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    @NotNull
    public String getAndClear() {
        String s = toString();
        clear();
        return s;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(chars);
    }

//    public int indexOf(char c) {
//        for (int i = 0; i < index; i++) { if (chars[i] == c) { return i; } }
//        return -1;
//    }

//    public int indexOf(char c, int fromIndex) throws IndexOutOfBoundsException {
//        if (fromIndex < 0 || fromIndex > index) {
//            throw new IndexOutOfBoundsException(fromIndex);
//        }
//        for (int i = fromIndex; i < index; i++) {
//            if (chars[i] == c) { return i; }
//        }
//        return -1;
//    }

    @Override
    public boolean isEmpty() {
        return index == 0;
    }

    @Override
    public int length() {
        return index;
    }

//    public MutableString replace(int from, int to, CharSequence with) throws IndexOutOfBoundsException {
//        if (from < 0 || from > to) {
//            throw new IndexOutOfBoundsException("from is out of bounds {" + from + "}");
//        }
//        if (to > index) {
//            throw new IndexOutOfBoundsException("to is out of bounds {" + to + "}");
//        }
//        if (with == null) {
//            with = NULL;
//        }
//
//        int move = with.length() - (to - from);
//        if (move != 0) {
//            if (move > 0) {
//                ensureCapacity(index + move);
//                for (int i = index, k = move; k > 0; i--, k--) {
//                    chars[i] = chars[i - 1];
//                }
//            }
//            else if (index - to >= 0) {
//                System.arraycopy(chars, to, chars, to + move, index - to);
//            }
//            index += move;
//        }
//        for (int i = to; i < from; i++) {
//            chars[i] = with.charAt(i - to);
//        }
//
//        return this;
//    }

//    public MutableString[] split(char c) {
//        ArrayList<MutableString> split = new ArrayList<>();
//        for (int i = 0, k = 0; i <= index; ) {
//            if (i == index) {
//                split.add(subSequence(k, i));
//            }
//            else if (chars[i] == c) {
//                split.add(subSequence(k, i));
//                k = ++i;
//            }
//            else {
//                ++i;
//            }
//        }
//        return split.toArray(new MutableString[split.size()]);
//    }

//    public boolean startsWith(char c) {
//        return index > 0 && chars[0] == c;
//    }

    @Override
    @NotNull
    public MutableString subSequence(int start, int end) throws IndexOutOfBoundsException {
        return new MutableString(this, start, end);
    }

//    public MutableString subSequence(int start) throws IndexOutOfBoundsException {
//        return new MutableString(this, start, index);
//    }
//
//    public String subString(int start, int end) throws IndexOutOfBoundsException {
//        return String.copyValueOf(chars, start, end - start);
//    }
//
//    public String subString(int start) throws IndexOutOfBoundsException {
//        return String.copyValueOf(chars, start, index - start);
//    }

    @Override
    @NotNull
    public String toString() {
        return String.copyValueOf(chars, 0, index);
    }
}
