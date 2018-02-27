package com.jsoniter;

import com.jsoniter.spi.OmitValue;
import junit.framework.TestCase;

import java.lang.reflect.Type;


public class TestOmitValue extends TestCase {

    public void test_default_true(){
        // Contract: test if parse in OmitValue.java steps in to default switch case.
        // Returns true iff none of the cases in OmitValue.java is true by having
        // the input parameter valueType set to a type not mentioned in previous
        // cases.

        OmitValue.Parsed parsed = new OmitValue.Parsed(int.class, "hello");

        boolean exceptionThrown = false;

        try {
            OmitValue result = parsed.parse(Type.class, "cow");

        } catch (UnsupportedOperationException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);

    }
}
