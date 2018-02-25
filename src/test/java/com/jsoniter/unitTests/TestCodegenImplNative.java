package com.jsoniter;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class TestCodegenImplNative extends TestCase {
    
	public void testGenReadOpBool() throws Exception {
        Object codegenImplNative = Class.forName("com.jsoniter.CodegenImplNative").newInstance();
        Method genReadOp = codegenImplNative.getClass().getDeclaredMethod("genReadOp", String.class, Type.class);
        genReadOp.setAccessible(true);
        Type type = boolean.class;
        String cacheKey = "cacheKey";
		assertEquals("iter.readBoolean()", genReadOp.invoke(codegenImplNative, cacheKey, type));
	}

    public void testGenReadOpByte() throws Exception {
        Object codegenImplNative = Class.forName("com.jsoniter.CodegenImplNative").newInstance();
        Method genReadOp = codegenImplNative.getClass().getDeclaredMethod("genReadOp", String.class, Type.class);
        genReadOp.setAccessible(true);
        Type type = byte.class;
        String cacheKey = "cacheKey";
        assertEquals("iter.readShort()", genReadOp.invoke(codegenImplNative, cacheKey, type));
    }

    public void testGenReadOpShort() throws Exception {
        Object codegenImplNative = Class.forName("com.jsoniter.CodegenImplNative").newInstance();
        Method genReadOp = codegenImplNative.getClass().getDeclaredMethod("genReadOp", String.class, Type.class);
        genReadOp.setAccessible(true);
        // byte and short should return readShort
        Type typeByte = byte.class;
        Type typeShort = short.class;
        String cacheKey = "cacheKey";
        assertEquals("iter.readShort()", genReadOp.invoke(codegenImplNative, cacheKey, typeByte));
        assertEquals("iter.readShort()", genReadOp.invoke(codegenImplNative, cacheKey, typeShort));
    }

    public void testGenReadOpInt() throws Exception {
        Object codegenImplNative = Class.forName("com.jsoniter.CodegenImplNative").newInstance();
        Method genReadOp = codegenImplNative.getClass().getDeclaredMethod("genReadOp", String.class, Type.class);
        genReadOp.setAccessible(true);
        // char and and short should return readShort
        Type typeChar = char.class;
        Type typeInt = int.class;
        String cacheKey = "cacheKey";
        assertEquals("iter.readInt()", genReadOp.invoke(codegenImplNative, cacheKey, typeChar));
        assertEquals("iter.readInt()", genReadOp.invoke(codegenImplNative, cacheKey, typeInt));
    }

    public void testGenReadOpLong() throws Exception {
        Object codegenImplNative = Class.forName("com.jsoniter.CodegenImplNative").newInstance();
        Method genReadOp = codegenImplNative.getClass().getDeclaredMethod("genReadOp", String.class, Type.class);
        genReadOp.setAccessible(true);
        Type type = long.class;
        String cacheKey = "cacheKey";
        assertEquals("iter.readLong()", genReadOp.invoke(codegenImplNative, cacheKey, type));
    }

    public void testGenReadOpFloat() throws Exception {
        Object codegenImplNative = Class.forName("com.jsoniter.CodegenImplNative").newInstance();
        Method genReadOp = codegenImplNative.getClass().getDeclaredMethod("genReadOp", String.class, Type.class);
        genReadOp.setAccessible(true);
        Type type = float.class;
        String cacheKey = "cacheKey";
        assertEquals("iter.readFloat()", genReadOp.invoke(codegenImplNative, cacheKey, type));
    }

    public void testGenReadOpDouble() throws Exception {
        Object codegenImplNative = Class.forName("com.jsoniter.CodegenImplNative").newInstance();
        Method genReadOp = codegenImplNative.getClass().getDeclaredMethod("genReadOp", String.class, Type.class);
        genReadOp.setAccessible(true);
        Type type = double.class;
        String cacheKey = "cacheKey";
        assertEquals("iter.readDouble()", genReadOp.invoke(codegenImplNative, cacheKey, type));
    }
}