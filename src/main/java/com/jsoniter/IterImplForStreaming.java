package com.jsoniter;

import com.jsoniter.any.Any;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.Slice;

import java.io.IOException;

import static com.jsoniter.GlobalData.dictionary;

class IterImplForStreaming {

    public static final int readObjectFieldAsHash(JsonIterator iter) throws IOException {
        if (nextToken(iter) != '"') {
            throw iter.reportError("readObjectFieldAsHash", "expect \"");
        }
        long hash = 0x811c9dc5;
        for (; ; ) {
            byte c = 0;
            int i = iter.head;
            for (; i < iter.tail; i++) {
                c = iter.buf[i];
                if (c == '"') {
                    break;
                }
                hash ^= c;
                hash *= 0x1000193;
            }
            if (c == '"') {
                iter.head = i + 1;
                if (nextToken(iter) != ':') {
                    throw iter.reportError("readObjectFieldAsHash", "expect :");
                }
                return (int) hash;
            }
            if (!loadMore(iter)) {
                throw iter.reportError("readObjectFieldAsHash", "unmatched quote");
            }
        }
    }

    public static final Slice readObjectFieldAsSlice(JsonIterator iter) throws IOException {
        Slice field = readSlice(iter);
        boolean notCopied = field != null;
        if (CodegenAccess.skipWhitespacesWithoutLoadMore(iter)) {
            if (notCopied) {
                int len = field.tail() - field.head();
                byte[] newBuf = new byte[len];
                System.arraycopy(field.data(), field.head(), newBuf, 0, len);
                field.reset(newBuf, 0, newBuf.length);
            }
            if (!loadMore(iter)) {
                throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
            }
        }
        if (iter.buf[iter.head] != ':') {
            throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
        }
        iter.head++;
        return field;
    }

    final static void skipArray(JsonIterator iter) throws IOException {
        int level = 1;
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                switch (iter.buf[i]) {
                    case '"': // If inside string, skip it
                        iter.head = i + 1;
                        skipString(iter);
                        i = iter.head - 1; // it will be i++ soon
                        break;
                    case '[': // If open symbol, increase level
                        level++;
                        break;
                    case ']': // If close symbol, increase level
                        level--;

                        // If we have returned to the original level, we're done
                        if (level == 0) {
                            iter.head = i + 1;
                            return;
                        }
                        break;
                }
            }
            if (!loadMore(iter)) {
                return;
            }
        }
    }

    final static void skipObject(JsonIterator iter) throws IOException {
        int level = 1;
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                switch (iter.buf[i]) {
                    case '"': // If inside string, skip it
                        iter.head = i + 1;
                        skipString(iter);
                        i = iter.head - 1; // it will be i++ soon
                        break;
                    case '{': // If open symbol, increase level
                        level++;
                        break;
                    case '}': // If close symbol, increase level
                        level--;

                        // If we have returned to the original level, we're done
                        if (level == 0) {
                            iter.head = i + 1;
                            return;
                        }
                        break;
                }
            }
            if (!loadMore(iter)) {
                return;
            }
        }
    }

    final static void skipString(JsonIterator iter) throws IOException {
        for (; ; ) {
            int end = IterImplSkip.findStringEnd(iter);
            if (end == -1) {
                int j = iter.tail - 1;
                boolean escaped = true;
                // can not just look the last byte is \
                // because it could be \\ or \\\
                for (; ; ) {
                    // walk backward until head
                    if (j < iter.head || iter.buf[j] != '\\') {
                        // even number of backslashes
                        // either end of buffer, or " found
                        escaped = false;
                        break;
                    }
                    j--;
                    if (j < iter.head || iter.buf[j] != '\\') {
                        // odd number of backslashes
                        // it is \" or \\\"
                        break;
                    }
                    j--;

                }
                if (!loadMore(iter)) {
                    throw iter.reportError("skipString", "incomplete string");
                }
                if (escaped) {
                    iter.head = 1; // skip the first char as last char is \
                }
            } else {
                iter.head = end;
                return;
            }
        }
    }

    final static void skipUntilBreak(JsonIterator iter) throws IOException {
        // true, false, null, number
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                byte c = iter.buf[i];
                if (IterImplSkip.breaks[c]) {
                    iter.head = i;
                    return;
                }
            }
            if (!loadMore(iter)) {
                iter.head = iter.tail;
                return;
            }
        }
    }

    final static boolean skipNumber(JsonIterator iter) throws IOException {
        // true, false, null, number
        boolean dotFound = false;
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                byte c = iter.buf[i];
                if (c == '.' || c == 'e' || c == 'E') {
                    dotFound = true;
                    continue;
                }
                if (IterImplSkip.breaks[c]) {
                    iter.head = i;
                    return dotFound;
                }
            }
            if (!loadMore(iter)) {
                iter.head = iter.tail;
                return dotFound;
            }
        }
    }

    // read the bytes between " "
    final static Slice readSlice(JsonIterator iter) throws IOException {
        if (IterImpl.nextToken(iter) != '"') {
            throw iter.reportError("readSlice", "expect \" for string");
        }
        int end = IterImplString.findSliceEnd(iter);
        if (end != -1) {
            // reuse current buffer
            iter.reusableSlice.reset(iter.buf, iter.head, end - 1);
            iter.head = end;
            return iter.reusableSlice;
        }
        // TODO: avoid small memory allocation
        byte[] part1 = new byte[iter.tail - iter.head];
        System.arraycopy(iter.buf, iter.head, part1, 0, part1.length);
        for (; ; ) {
            if (!loadMore(iter)) {
                throw iter.reportError("readSlice", "unmatched quote");
            }
            end = IterImplString.findSliceEnd(iter);
            if (end == -1) {
                byte[] part2 = new byte[part1.length + iter.buf.length];
                System.arraycopy(part1, 0, part2, 0, part1.length);
                System.arraycopy(iter.buf, 0, part2, part1.length, iter.buf.length);
                part1 = part2;
            } else {
                byte[] part2 = new byte[part1.length + end - 1];
                System.arraycopy(part1, 0, part2, 0, part1.length);
                System.arraycopy(iter.buf, 0, part2, part1.length, end - 1);
                iter.head = end;
                iter.reusableSlice.reset(part2, 0, part2.length);
                return iter.reusableSlice;
            }
        }
    }

    final static byte nextToken(JsonIterator iter) throws IOException {
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                byte c = iter.buf[i];
                switch (c) {
                    case ' ':
                    case '\n':
                    case '\t':
                    case '\r':
                        continue;
                    default:
                        iter.head = i + 1;
                        return c;
                }
            }
            if (!loadMore(iter)) {
                return 0;
            }
        }
    }

    public final static boolean loadMore(JsonIterator iter) throws IOException {
        if (iter.in == null) {
            return false;
        }
        if (iter.skipStartedAt != -1) {
            return keepSkippedBytesThenRead(iter);
        }
        int n = iter.in.read(iter.buf);
        if (n < 1) {
            if (n == -1) {
                return false;
            } else {
                throw iter.reportError("loadMore", "read from input stream returned " + n);
            }
        } else {
            iter.head = 0;
            iter.tail = n;
        }
        return true;
    }

    private static boolean keepSkippedBytesThenRead(JsonIterator iter) throws IOException {
        int n;
        int offset;
        if (iter.skipStartedAt == 0 || iter.skipStartedAt < iter.tail / 2) {
            byte[] newBuf = new byte[iter.buf.length * 2];
            offset = iter.tail - iter.skipStartedAt;
            System.arraycopy(iter.buf, iter.skipStartedAt, newBuf, 0, offset);
            iter.buf = newBuf;
            n = iter.in.read(iter.buf, offset, iter.buf.length - offset);
        } else {
            offset = iter.tail - iter.skipStartedAt;
            System.arraycopy(iter.buf, iter.skipStartedAt, iter.buf, 0, offset);
            n = iter.in.read(iter.buf, offset, iter.buf.length - offset);
        }
        iter.skipStartedAt = 0;
        if (n < 1) {
            if (n == -1) {
                return false;
            } else {
                throw iter.reportError("loadMore", "read from input stream returned " + n);
            }
        } else {
            iter.head = offset;
            iter.tail = offset + n;
        }
        return true;
    }

    final static byte readByte(JsonIterator iter) throws IOException {
        if (iter.head == iter.tail) {
            if (!loadMore(iter)) {
                throw iter.reportError("readByte", "no more to read");
            }
        }
        return iter.buf[iter.head++];
    }

    public static Any readAny(JsonIterator iter) throws IOException {
        // TODO: avoid small memory allocation
        iter.skipStartedAt = iter.head;
        byte c = nextToken(iter);
        switch (c) {
            case '"':
                skipString(iter);
                byte[] copied = copySkippedBytes(iter);
                return Any.lazyString(copied, 0, copied.length);
            case 't':
                skipFixedBytes(iter, 3);
                iter.skipStartedAt = -1;
                return Any.wrap(true);
            case 'f':
                skipFixedBytes(iter, 4);
                iter.skipStartedAt = -1;
                return Any.wrap(false);
            case 'n':
                skipFixedBytes(iter, 3);
                iter.skipStartedAt = -1;
                return Any.wrap((Object) null);
            case '[':
                skipArray(iter);
                copied = copySkippedBytes(iter);
                return Any.lazyArray(copied, 0, copied.length);
            case '{':
                skipObject(iter);
                copied = copySkippedBytes(iter);
                return Any.lazyObject(copied, 0, copied.length);
            default:
                if (skipNumber(iter)) {
                    copied = copySkippedBytes(iter);
                    return Any.lazyDouble(copied, 0, copied.length);
                } else {
                    copied = copySkippedBytes(iter);
                    return Any.lazyLong(copied, 0, copied.length);
                }
        }
    }

    private static byte[] copySkippedBytes(JsonIterator iter) {
        int start = iter.skipStartedAt;
        iter.skipStartedAt = -1;
        int end = iter.head;
        byte[] bytes = new byte[end - start];
        System.arraycopy(iter.buf, start, bytes, 0, bytes.length);
        return bytes;
    }

    public static void skipFixedBytes(JsonIterator iter, int n) throws IOException {
        iter.head += n;
        if (iter.head >= iter.tail) {
            int more = iter.head - iter.tail;
            if (!loadMore(iter)) {
                if (more == 0) {
                    iter.head = iter.tail;
                    return;
                }
                throw iter.reportError("skipFixedBytes", "unexpected end");
            }
            iter.head += more;
        }
    }

    public static int updateStringCopyBound(final JsonIterator iter, final int bound) {
        if (bound > iter.tail - iter.head) {
            return iter.tail - iter.head;
        } else {
            return bound;
        }
    }

    public final static int readStringSlowPath(JsonIterator iter, int j) throws IOException {
        MethodData methodData =  new MethodData(42);
        dictionary.put("IterImplForStreaming - readStringSlowPath", methodData);
        methodData.branchReached[0] = true;
        boolean isExpectingLowSurrogate = false;
        for (;;) {
            methodData.branchReached[1] = true;
            int bc = readByte(iter);
            if (bc == '"') {
                methodData.branchReached[2] = true;
                return j;
            }
            methodData.branchReached[3] = true;
            if (bc == '\\') {
                methodData.branchReached[4] = true;
                bc = readByte(iter);
                switch (bc) {
                    case 'b':
                        methodData.branchReached[5] = true;
                        bc = '\b';
                        break;
                    case 't':
                        methodData.branchReached[6] = true;
                        bc = '\t';
                        break;
                    case 'n':
                        methodData.branchReached[7] = true;
                        bc = '\n';
                        break;
                    case 'f':
                        methodData.branchReached[8] = true;
                        bc = '\f';
                        break;
                    case 'r':
                        methodData.branchReached[9] = true;
                        bc = '\r';
                        break;
                    case '"':
                        methodData.branchReached[10] = true;
                    case '/':
                        methodData.branchReached[11] = true;
                    case '\\':
                        methodData.branchReached[12] = true;
                        break;
                    case 'u':
                        methodData.branchReached[13] = true;
                        bc = (IterImplString.translateHex(readByte(iter)) << 12) +
                                (IterImplString.translateHex(readByte(iter)) << 8) +
                                (IterImplString.translateHex(readByte(iter)) << 4) +
                                IterImplString.translateHex(readByte(iter));
                        if (Character.isHighSurrogate((char) bc)) {
                            methodData.branchReached[14] = true;
                            if (isExpectingLowSurrogate) {
                                methodData.branchReached[15] = true;
                                throw new JsonException("invalid surrogate");
                            } else {
                                methodData.branchReached[16] = true;
                                isExpectingLowSurrogate = true;
                            }
                            methodData.branchReached[17] = true;
                        } else if (Character.isLowSurrogate((char) bc)) {
                            methodData.branchReached[18] = true;
                            if (isExpectingLowSurrogate) {
                                methodData.branchReached[19] = true;
                                isExpectingLowSurrogate = false;
                            } else {
                                methodData.branchReached[20] = true;
                                throw new JsonException("invalid surrogate");
                            }
                            methodData.branchReached[21] = true;
                        } else {
                            methodData.branchReached[22] = true;
                            if (isExpectingLowSurrogate) {
                                methodData.branchReached[23] = true;
                                throw new JsonException("invalid surrogate");
                            }
                            methodData.branchReached[24] = true;
                        }
                        methodData.branchReached[25] = true;
                        break;

                    default:
                        methodData.branchReached[26] = true;
                        throw iter.reportError("readStringSlowPath", "invalid escape character: " + bc);
                }
            } else if ((bc & 0x80) != 0) {
                methodData.branchReached[27] = true;
                final int u2 = readByte(iter);
                if ((bc & 0xE0) == 0xC0) {
                    methodData.branchReached[28] = true;
                    bc = ((bc & 0x1F) << 6) + (u2 & 0x3F);
                } else {
                    methodData.branchReached[29] = true;
                    final int u3 = readByte(iter);
                    if ((bc & 0xF0) == 0xE0) {
                        methodData.branchReached[30] = true;
                        bc = ((bc & 0x0F) << 12) + ((u2 & 0x3F) << 6) + (u3 & 0x3F);
                    } else {
                        methodData.branchReached[31] = true;
                        final int u4 = readByte(iter);
                        if ((bc & 0xF8) == 0xF0) {
                            methodData.branchReached[32] = true;
                            bc = ((bc & 0x07) << 18) + ((u2 & 0x3F) << 12) + ((u3 & 0x3F) << 6) + (u4 & 0x3F);
                        } else {
                            methodData.branchReached[33] = true;
                            throw iter.reportError("readStringSlowPath", "invalid unicode character");
                        }
                        methodData.branchReached[34] = true;

                        if (bc >= 0x10000) {
                            methodData.branchReached[35] = true;
                            // check if valid unicode
                            if (bc >= 0x110000) {
                                methodData.branchReached[36] = true;
                                throw iter.reportError("readStringSlowPath", "invalid unicode character");
                            }
                            methodData.branchReached[37] = true;

                            // split surrogates
                            final int sup = bc - 0x10000;
                            if (iter.reusableChars.length == j) {
                                methodData.branchReached[38] = true;
                                char[] newBuf = new char[iter.reusableChars.length * 2];
                                System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                                iter.reusableChars = newBuf;
                            }
                            iter.reusableChars[j++] = (char) ((sup >>> 10) + 0xd800);
                            if (iter.reusableChars.length == j) {
                                methodData.branchReached[39] = true;
                                char[] newBuf = new char[iter.reusableChars.length * 2];
                                System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                                iter.reusableChars = newBuf;
                            }
                            iter.reusableChars[j++] = (char) ((sup & 0x3ff) + 0xdc00);
                            continue;
                        }
                    }
                }
            }
            methodData.branchReached[40] = true;
            if (iter.reusableChars.length == j) {
                methodData.branchReached[41] = true;
                char[] newBuf = new char[iter.reusableChars.length * 2];
                System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                iter.reusableChars = newBuf;
            }
            iter.reusableChars[j++] = (char) bc;
        }
    }

    static long readLongSlowPath(final JsonIterator iter, long value) throws IOException {
        value = -value; // add negatives to avoid redundant checks for Long.MIN_VALUE on each iteration
        long multmin = -922337203685477580L; // limit / 10
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                int ind = IterImplNumber.intDigits[iter.buf[i]];
                if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                    iter.head = i;
                    return value;
                }
                if (value < multmin) {
                    throw iter.reportError("readLongSlowPath", "value is too large for long");
                }
                value = (value << 3) + (value << 1) - ind;
                if (value >= 0) {
                    throw iter.reportError("readLongSlowPath", "value is too large for long");
                }
            }
            if (!IterImpl.loadMore(iter)) {
                iter.head = iter.tail;
                return value;
            }
        }
    }

    static int readIntSlowPath(final JsonIterator iter, int value) throws IOException {
        value = -value; // add negatives to avoid redundant checks for Integer.MIN_VALUE on each iteration
        int multmin = -214748364; // limit / 10
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                int ind = IterImplNumber.intDigits[iter.buf[i]];
                if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                    iter.head = i;
                    return value;
                }
                if (value < multmin) {
                    throw iter.reportError("readIntSlowPath", "value is too large for int");
                }
                value = (value << 3) + (value << 1) - ind;
                if (value >= 0) {
                    throw iter.reportError("readIntSlowPath", "value is too large for int");
                }
            }
            if (!IterImpl.loadMore(iter)) {
                iter.head = iter.tail;
                return value;
            }
        }
    }

    public static final double readDoubleSlowPath(final JsonIterator iter) throws IOException {
        try {
            String numberAsStr = readNumber(iter);
            return Double.valueOf(numberAsStr);
        } catch (NumberFormatException e) {
            throw iter.reportError("readDoubleSlowPath", e.toString());
        }
    }

    public static final String readNumber(final JsonIterator iter) throws IOException {
        MethodData methodData =  new MethodData(24);
        dictionary.put("IterImplForStreaming - readNumber", methodData);
        int j = 0;
        methodData.branchReached[23] = true;
        for (; ; ) {
            methodData.branchReached[0] = true;
            for (int i = iter.head; i < iter.tail; i++) {
                methodData.branchReached[1] = true;
                if (j == iter.reusableChars.length) {
                    methodData.branchReached[2] = true;
                    char[] newBuf = new char[iter.reusableChars.length * 2];
                    System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                    iter.reusableChars = newBuf;
                }
                methodData.branchReached[3] = true;
                byte c = iter.buf[i];
                switch (c) {
                    case '-':
                        methodData.branchReached[4] = true;
                    case '+':
                        methodData.branchReached[5] = true;
                    case '.':
                        methodData.branchReached[6] = true;
                    case 'e':
                        methodData.branchReached[7] = true;
                    case 'E':
                        methodData.branchReached[8] = true;
                    case '0':
                        methodData.branchReached[9] = true;
                    case '1':
                        methodData.branchReached[10] = true;
                    case '2':
                        methodData.branchReached[11] = true;
                    case '3':
                        methodData.branchReached[12] = true;
                    case '4':
                        methodData.branchReached[13] = true;
                    case '5':
                        methodData.branchReached[14] = true;
                    case '6':
                        methodData.branchReached[14] = true;
                    case '7':
                        methodData.branchReached[15] = true;
                    case '8':
                        methodData.branchReached[16] = true;
                    case '9':
                        methodData.branchReached[17] = true;
                        iter.reusableChars[j++] = (char) c;
                        break;
                    default:
                        methodData.branchReached[18] = true;
                        iter.head = i;
                        return new String(iter.reusableChars, 0, j);
                }
                methodData.branchReached[19] = true;
            }
            methodData.branchReached[20] = true;
            if (!IterImpl.loadMore(iter)) {
                methodData.branchReached[21] = true;
                iter.head = iter.tail;
                return new String(iter.reusableChars, 0, j);
            }
            methodData.branchReached[22] = true;
        }
    }

    static final double readDouble(final JsonIterator iter) throws IOException {
        return readDoubleSlowPath(iter);
    }

    static final long readLong(final JsonIterator iter, final byte c) throws IOException {
        long ind = IterImplNumber.intDigits[c];
        if (ind == 0) {
            assertNotLeadingZero(iter);
            return 0;
        }
        if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            throw iter.reportError("readLong", "expect 0~9");
        }
        return IterImplForStreaming.readLongSlowPath(iter, ind);
    }

    static final int readInt(final JsonIterator iter, final byte c) throws IOException {
        int ind = IterImplNumber.intDigits[c];
        if (ind == 0) {
            assertNotLeadingZero(iter);
            return 0;
        }
        if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            throw iter.reportError("readInt", "expect 0~9");
        }
        return IterImplForStreaming.readIntSlowPath(iter, ind);
    }

    static void assertNotLeadingZero(JsonIterator iter) throws IOException {
        try {
            byte nextByte = IterImpl.readByte(iter);
            iter.unreadByte();
            int ind2 = IterImplNumber.intDigits[nextByte];
            if (ind2 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                return;
            }
            throw iter.reportError("assertNotLeadingZero", "leading zero is invalid");
        } catch (ArrayIndexOutOfBoundsException e) {
            iter.head = iter.tail;
            return;
        }
    }
}
