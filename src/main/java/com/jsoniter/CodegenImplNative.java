package com.jsoniter;

import com.jsoniter.any.Any;
import com.jsoniter.spi.*;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static com.jsoniter.GlobalData.dictionary;

class CodegenImplNative {
    final static Map<String, String> NATIVE_READS = new HashMap<String, String>() {{
        put("float", "iter.readFloat()");
        put("double", "iter.readDouble()");
        put("boolean", "iter.readBoolean()");
        put("byte", "iter.readShort()");
        put("short", "iter.readShort()");
        put("int", "iter.readInt()");
        put("char", "iter.readInt()");
        put("long", "iter.readLong()");
        put(Float.class.getName(), "(iter.readNull() ? null : java.lang.Float.valueOf(iter.readFloat()))");
        put(Double.class.getName(), "(iter.readNull() ? null : java.lang.Double.valueOf(iter.readDouble()))");
        put(Boolean.class.getName(), "(iter.readNull() ? null : java.lang.Boolean.valueOf(iter.readBoolean()))");
        put(Byte.class.getName(), "(iter.readNull() ? null : java.lang.Byte.valueOf((byte)iter.readShort()))");
        put(Character.class.getName(), "(iter.readNull() ? null : java.lang.Character.valueOf((char)iter.readShort()))");
        put(Short.class.getName(), "(iter.readNull() ? null : java.lang.Short.valueOf(iter.readShort()))");
        put(Integer.class.getName(), "(iter.readNull() ? null : java.lang.Integer.valueOf(iter.readInt()))");
        put(Long.class.getName(), "(iter.readNull() ? null : java.lang.Long.valueOf(iter.readLong()))");
        put(BigDecimal.class.getName(), "iter.readBigDecimal()");
        put(BigInteger.class.getName(), "iter.readBigInteger()");
        put(String.class.getName(), "iter.readString()");
        put(Object.class.getName(), "iter.read()");
        put(Any.class.getName(), "iter.readAny()");
    }};
    final static Map<Class, Decoder> NATIVE_DECODERS = new HashMap<Class, Decoder>() {{
        put(float.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readFloat();
            }
        });
        put(Float.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readFloat();
            }
        });
        put(double.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readDouble();
            }
        });
        put(Double.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readDouble();
            }
        });
        put(boolean.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readBoolean();
            }
        });
        put(Boolean.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readBoolean();
            }
        });
        put(byte.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return Byte.valueOf((byte) iter.readShort());
            }
        });
        put(Byte.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : (byte)iter.readShort();
            }
        });
        put(short.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readShort();
            }
        });
        put(Short.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readShort();
            }
        });
        put(int.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readInt();
            }
        });
        put(Integer.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readInt();
            }
        });
        put(char.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return (char)iter.readInt();
            }
        });
        put(Character.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : (char)iter.readInt();
            }
        });
        put(long.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readLong();
            }
        });
        put(Long.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readNull() ? null : iter.readLong();
            }
        });
        put(BigDecimal.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readBigDecimal();
            }
        });
        put(BigInteger.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readBigInteger();
            }
        });
        put(String.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readString();
            }
        });
        put(Object.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.read();
            }
        });
        put(Any.class, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                return iter.readAny();
            }
        });
    }};

    public static String genReadOp(Type type) {
        String cacheKey = TypeLiteral.create(type).getDecoderCacheKey();
        return String.format("(%s)%s", getTypeName(type), genReadOp(cacheKey, type));
    }

    public static String getTypeName(Type fieldType) {
        if (fieldType instanceof Class) {
            Class clazz = (Class) fieldType;
            return clazz.getCanonicalName();
        } else if (fieldType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) fieldType;
            Class clazz = (Class) pType.getRawType();
            return clazz.getCanonicalName();
        } else if (fieldType instanceof WildcardType) {
            return Object.class.getCanonicalName();
        } else {
            throw new JsonException("unsupported type: " + fieldType);
        }
    }

    static String genField(Binding field) {
        String fieldCacheKey = field.decoderCacheKey();
        Type fieldType = field.valueType;
        return String.format("(%s)%s", getTypeName(fieldType), genReadOp(fieldCacheKey, fieldType));

    }

    private static String genReadOp(String cacheKey, Type valueType) {
        MethodData methodData =  new MethodData(42);
        dictionary.put("CodegenImplNative - genReadOp", methodData);
        // the field decoder might be registered directly
        methodData.branchReached[0] = true;
        Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
        if (decoder == null) {
            methodData.branchReached[1] = true;
            // if cache key is for field, and there is no field decoder specified
            // update cache key for normal type
            cacheKey = TypeLiteral.create(valueType).getDecoderCacheKey();
            decoder = JsoniterSpi.getDecoder(cacheKey);
            if (decoder == null) {
                methodData.branchReached[2] = true;
                if (valueType instanceof Class) {
                    methodData.branchReached[3] = true;
                    Class clazz = (Class) valueType;
                    String nativeRead = NATIVE_READS.get(clazz.getCanonicalName());
                    if (nativeRead != null) {
                        methodData.branchReached[4] = true;
                        return nativeRead;
                    }
                } else if (valueType instanceof WildcardType) {
                    methodData.branchReached[5] = true;
                    return NATIVE_READS.get(Object.class.getCanonicalName());
                }
                methodData.branchReached[6] = true;
                Codegen.getDecoder(cacheKey, valueType);
                if (Codegen.canStaticAccess(cacheKey)) {
                    methodData.branchReached[7] = true;
                    return String.format("%s.decode_(iter)", cacheKey);
                } else {
                    methodData.branchReached[8] = true;
                    // can not use static "decode_" method to access, go through codegen cache
                    return String.format("com.jsoniter.CodegenAccess.read(\"%s\", iter)", cacheKey);
                }
            }
        }
        methodData.branchReached[9] = true;
        if (valueType == boolean.class) {
            methodData.branchReached[10] = true;
            if (!(decoder instanceof Decoder.BooleanDecoder)) {
                methodData.branchReached[11] = true;
                throw new JsonException("decoder for " + cacheKey + "must implement Decoder.BooleanDecoder");
            }
            methodData.branchReached[12] = true;
            return String.format("com.jsoniter.CodegenAccess.readBoolean(\"%s\", iter)", cacheKey);
        }
        methodData.branchReached[13] = true;
        if (valueType == byte.class) {
            methodData.branchReached[14] = true;
            if (!(decoder instanceof Decoder.ShortDecoder)) {
                methodData.branchReached[15] = true;
                throw new JsonException("decoder for " + cacheKey + "must implement Decoder.ShortDecoder");
            }
            methodData.branchReached[16] = true;
            return String.format("com.jsoniter.CodegenAccess.readShort(\"%s\", iter)", cacheKey);
        }
        methodData.branchReached[17] = true;
        if (valueType == short.class) {
            methodData.branchReached[18] = true;
            if (!(decoder instanceof Decoder.ShortDecoder)) {
                methodData.branchReached[19] = true;
                throw new JsonException("decoder for " + cacheKey + "must implement Decoder.ShortDecoder");
            }
            methodData.branchReached[20] = true;
            return String.format("com.jsoniter.CodegenAccess.readShort(\"%s\", iter)", cacheKey);
        }
        methodData.branchReached[21] = true;
        if (valueType == char.class) {
            methodData.branchReached[22] = true;
            if (!(decoder instanceof Decoder.IntDecoder)) {
                methodData.branchReached[23] = true;
                throw new JsonException("decoder for " + cacheKey + "must implement Decoder.IntDecoder");
            }
            methodData.branchReached[24] = true;
            return String.format("com.jsoniter.CodegenAccess.readInt(\"%s\", iter)", cacheKey);
        }
        methodData.branchReached[25] = true;
        if (valueType == int.class) {
            methodData.branchReached[26] = true;
            if (!(decoder instanceof Decoder.IntDecoder)) {
                methodData.branchReached[27] = true;
                throw new JsonException("decoder for " + cacheKey + "must implement Decoder.IntDecoder");
            }
            methodData.branchReached[28] = true;
            return String.format("com.jsoniter.CodegenAccess.readInt(\"%s\", iter)", cacheKey);
        }
        methodData.branchReached[29] = true;
        if (valueType == long.class) {
            methodData.branchReached[30] = true;
            if (!(decoder instanceof Decoder.LongDecoder)) {
                methodData.branchReached[31] = true;
                throw new JsonException("decoder for " + cacheKey + "must implement Decoder.LongDecoder");
            }
            methodData.branchReached[32] = true;
            return String.format("com.jsoniter.CodegenAccess.readLong(\"%s\", iter)", cacheKey);
        }
        methodData.branchReached[33] = true;
        if (valueType == float.class) {
            methodData.branchReached[34] = true;
            if (!(decoder instanceof Decoder.FloatDecoder)) {
                methodData.branchReached[35] = true;
                throw new JsonException("decoder for " + cacheKey + "must implement Decoder.FloatDecoder");
            }
            methodData.branchReached[36] = true;
            return String.format("com.jsoniter.CodegenAccess.readFloat(\"%s\", iter)", cacheKey);
        }
        methodData.branchReached[37] = true;
        if (valueType == double.class) {
            methodData.branchReached[38] = true;
            if (!(decoder instanceof Decoder.DoubleDecoder)) {
                methodData.branchReached[39] = true;
                throw new JsonException("decoder for " + cacheKey + "must implement Decoder.DoubleDecoder");
            }
            methodData.branchReached[40] = true;
            return String.format("com.jsoniter.CodegenAccess.readDouble(\"%s\", iter)", cacheKey);
        }
        methodData.branchReached[41] = true;
        return String.format("com.jsoniter.CodegenAccess.read(\"%s\", iter)", cacheKey);
    }
}
