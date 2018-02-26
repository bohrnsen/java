package com.jsoniter;

import junit.framework.TestCase;
import org.junit.Test;

public class TestIterImpl extends TestCase {

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBadJ() throws Exception {
        JsonIterator iter = JsonIterator.parse("{'a': {'b': {'c': 'd'}}}");
        try {
            IterImpl.readStringSlowPath(iter, 1337);
        } catch (Exception e) {
        }
    }

    public void test() throws Exception {
        String json = "\"{a: {}}\"";
        JsonIterator iter = JsonIterator.parse(json);
        iter.buf[1] = '\\';
        iter.buf[2] = 'b';
        int result = IterImpl.readStringSlowPath(iter, 0);
        assertEquals(0, result);
    }
}
