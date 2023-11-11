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

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Adam Martinu
 * @version 1.1 2021-04-27
 */
@SuppressWarnings("unused")
public class MutableString implements Appendable, CharSequence {

    protected static final int DEFAULT_LENGTH = 128;
    protected static final String NULL = "null";

    public char[] chars;
    public int index = 0;

    public MutableString(final int capacity) {
        chars = new char[capacity];
    }

    public MutableString() {
        chars = new char[DEFAULT_LENGTH];
    }

    public MutableString(CharSequence csq, final int start, final int end) throws IndexOutOfBoundsException {
        if (csq == null)
            csq = NULL;

        if (start < 0)
            throw new IndexOutOfBoundsException(start);
        if (end < 0)
            throw new IndexOutOfBoundsException(end);
        if (start > end)
            throw new IndexOutOfBoundsException(start);
        if (end > csq.length())
            throw new IndexOutOfBoundsException(end);

        final int csqSize = end - start;
        int length = DEFAULT_LENGTH;
        while (csqSize > length)
            length <<= 1;
        chars = new char[length];
        for (int i = start; i < end; i++)
            chars[index++] = csq.charAt(i);
    }

    public MutableString add(final CharSequence csq) {
        return append(csq);
    }

    public MutableString add(final CharSequence csq, final int start, final int end) throws IndexOutOfBoundsException {
        return append(csq, start, end);
    }

    public MutableString add(final char c) {
        return append(c);
    }

    public MutableString add(final char... csq) {
        return append(csq);
    }

    public MutableString add(final Object o) {
        return append(String.valueOf(o));
    }

    public MutableString append(char... csq) {
        if (csq == null)
            csq = NULL.toCharArray();
        else if (csq.length == 0)
            return this;

        ensureCapacity(index + csq.length);
        for (final char c : csq)
            chars[index++] = c;
        return this;
    }

    @Override
    public MutableString append(CharSequence csq) {
        if (csq == null)
            csq = NULL;

        ensureCapacity(index + csq.length());
        for (int i = 0; i < csq.length(); i++)
            chars[index++] = csq.charAt(i);
        return this;
    }

    @Override
    public MutableString append(CharSequence csq, final int start, final int end) throws IndexOutOfBoundsException {
        if (csq == null)
            csq = NULL;

        if (start < 0)
            throw new IndexOutOfBoundsException(start);
        if (end < 0)
            throw new IndexOutOfBoundsException(end);
        if (start > end)
            throw new IndexOutOfBoundsException(start);
        if (end > csq.length())
            throw new IndexOutOfBoundsException(end);

        final int size = end - start;
        ensureCapacity(index + size);
        for (int i = 0; i < size; i++)
            chars[index++] = csq.charAt(i + start);
        return this;
    }

    @Override
    public MutableString append(final char c) {
        ensureCapacity(index + 1);
        chars[index++] = c;
        return this;
    }

    public int available() {
        return chars.length - index;
    }

    @Override
    public char charAt(final int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= this.index)
            throw new IndexOutOfBoundsException(index);
        return chars[index];
    }

    public void clear() {
        index = 0;
    }

    public void delete(final int from) throws IndexOutOfBoundsException {
        if (from < 0 || from > index)
            throw new IndexOutOfBoundsException(from);
        index = from;
    }

    public void delete(final int from, final int to) throws IndexOutOfBoundsException {
        if (from < 0)
            throw new IndexOutOfBoundsException(from);
        if (to < 0)
            throw new IndexOutOfBoundsException(to);
        if (from > to)
            throw new IndexOutOfBoundsException(from);
        if (to > index)
            throw new IndexOutOfBoundsException(to);
        for (int i = from, k = to; i < to && k < index; i++, k++)
            chars[i] = chars[k];
        index -= (to - from);
    }

    public void ensureCapacity(final int capacity) {
        if (capacity > chars.length) {
            int newLength = chars.length << 1;
            while (capacity > newLength)
                newLength <<= 1;
            chars = Arrays.copyOf(chars, newLength);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        else if (o instanceof MutableString) {
            final MutableString ms = (MutableString) o;
            if (index != ms.index)
                return false;
            for (int i = 0; i < index; i++)
                if (chars[i] != ms.chars[i])
                    return false;
            return true;
        }
        else if (o instanceof CharSequence) {
            final CharSequence csq = (CharSequence) o;
            if (index != csq.length())
                return false;
            for (int i = 0; i < index; i++)
                if (chars[i] != csq.charAt(i))
                    return false;
            return true;
        }
        else
            return false;
    }

    public String get() {
        final String s = toString();
        clear();
        return s;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(chars);
    }

    public int indexOf(final char c) {
        for (int i = 0; i < index; i++)
            if (chars[i] == c)
                return i;
        return -1;
    }

    public int indexOf(final char c, final int fromIndex) throws IndexOutOfBoundsException {
        if (fromIndex < 0 || fromIndex > index)
            throw new IndexOutOfBoundsException(fromIndex);
        for (int i = fromIndex; i < index; i++)
            if (chars[i] == c)
                return i;
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return index == 0;
    }

    @Override
    public int length() {
        return index;
    }

    public MutableString replace(final int from, final int to, CharSequence with) throws
            IndexOutOfBoundsException {
        if (from < 0)
            throw new IndexOutOfBoundsException(from);
        if (to < 0)
            throw new IndexOutOfBoundsException(to);
        if (from > to)
            throw new IndexOutOfBoundsException(from);
        if (to > index)
            throw new IndexOutOfBoundsException(to);
        if (with == null)
            with = NULL;

        final int move = with.length() - (to - from);
        if (move != 0) {
            if (move > 0) {
                ensureCapacity(index + move);
                for (int i = index, k = move; k > 0; i--, k--)
                    chars[i] = chars[i - 1];
            }
            else if (index - to >= 0)
                System.arraycopy(chars, to, chars, to + move, index - to);
            index += move;
        }
        for (int i = to; i < from; i++)
            chars[i] = with.charAt(i - to);

        return this;
    }

    public MutableString[] split(final char c) {
        final ArrayList<MutableString> split = new ArrayList<>();
        for (int i = 0, k = 0; i <= index; ) {
            if (i == index)
                split.add(subSequence(k, i));
            else if (chars[i] == c) {
                split.add(subSequence(k, i));
                k = ++i;
            }
            else
                ++i;
        }
        return split.toArray(new MutableString[split.size()]);
    }

    public boolean startsWith(final char c) {
        return index > 0 && chars[0] == c;
    }

    @Override
    public MutableString subSequence(final int start, final int end) throws IndexOutOfBoundsException {
        return new MutableString(this, start, end);
    }

    public MutableString subSequence(final int start) throws IndexOutOfBoundsException {
        return new MutableString(this, start, index);
    }

    public String subString(final int start, final int end) throws IndexOutOfBoundsException {
        return String.copyValueOf(chars, start, end - start);
    }

    public String subString(final int start) throws IndexOutOfBoundsException {
        return String.copyValueOf(chars, start, index - start);
    }

    @Override
    public String toString() {
        return String.copyValueOf(chars, 0, index);
    }
}
