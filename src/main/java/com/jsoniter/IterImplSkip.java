package com.jsoniter;

import java.io.IOException;

import static com.jsoniter.GlobalData.dictionary;

class IterImplSkip {

    static final boolean[] breaks = new boolean[127];

    static {
        breaks[' '] = true;
        breaks['\t'] = true;
        breaks['\n'] = true;
        breaks['\r'] = true;
        breaks[','] = true;
        breaks['}'] = true;
        breaks[']'] = true;
    }

    public static final void skip(JsonIterator iter) throws IOException {
        MethodData methodData =  new MethodData(19);
        dictionary.put("IterImplSkip - skip", methodData);
        methodData.branchReached[18] = true;
        byte c = IterImpl.nextToken(iter);
        switch (c) {
            case '"':
                methodData.branchReached[0] = true;
                IterImpl.skipString(iter);
                return;
            case '-':
                methodData.branchReached[1] = true;
            case '0':
                methodData.branchReached[2] = true;
            case '1':
                methodData.branchReached[3] = true;
            case '2':
                methodData.branchReached[4] = true;
            case '3':
                methodData.branchReached[5] = true;
            case '4':
                methodData.branchReached[6] = true;
            case '5':
                methodData.branchReached[7] = true;
            case '6':
                methodData.branchReached[8] = true;
            case '7':
                methodData.branchReached[9] = true;
            case '8':
                methodData.branchReached[10] = true;
            case '9':
                methodData.branchReached[11] = true;
                IterImpl.skipUntilBreak(iter);
                return;
            case 't':
                methodData.branchReached[12] = true;
            case 'n':
                methodData.branchReached[13] = true;
                IterImpl.skipFixedBytes(iter, 3); // true or null
                return;
            case 'f':
                methodData.branchReached[14] = true;
                IterImpl.skipFixedBytes(iter, 4); // false
                return;
            case '[':
                methodData.branchReached[15] = true;
                IterImpl.skipArray(iter);
                return;
            case '{':
                methodData.branchReached[16] = true;
                IterImpl.skipObject(iter);
                return;
            default:
                methodData.branchReached[17] = true;
                throw iter.reportError("IterImplSkip", "do not know how to skip: " + c);
        }
    }

    // adapted from: https://github.com/buger/jsonparser/blob/master/parser.go
    // Tries to find the end of string
    // Support if string contains escaped quote symbols.
    final static int findStringEnd(JsonIterator iter) {
        boolean escaped = false;
        for (int i = iter.head; i < iter.tail; i++) {
            byte c = iter.buf[i];
            if (c == '"') {
                if (!escaped) {
                    return i + 1;
                } else {
                    int j = i - 1;
                    for (; ; ) {
                        if (j < iter.head || iter.buf[j] != '\\') {
                            // even number of backslashes
                            // either end of buffer, or " found
                            return i + 1;
                        }
                        j--;
                        if (j < iter.head || iter.buf[j] != '\\') {
                            // odd number of backslashes
                            // it is \" or \\\"
                            break;
                        }
                        j--;
                    }
                }
            } else if (c == '\\') {
                escaped = true;
            }
        }
        return -1;
    }
}
