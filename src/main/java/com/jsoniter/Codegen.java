package com.jsoniter;

import com.jsoniter.spi.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

import static com.jsoniter.GlobalData.dictionary;

class Codegen {

    // only read/write when generating code with synchronized protection
    private final static Set<String> generatedClassNames = new HashSet<String>();
    static CodegenAccess.StaticCodegenTarget isDoingStaticCodegen = null;

    static Decoder getDecoder(String cacheKey, Type type) {
        Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
        if (decoder != null) {
            return decoder;
        }
        return gen(cacheKey, type);
    }

    private synchronized static Decoder gen(String cacheKey, Type type) {
        Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
        if (decoder != null) {
            return decoder;
        }
        List<Extension> extensions = JsoniterSpi.getExtensions();
        for (Extension extension : extensions) {
            type = extension.chooseImplementation(type);
        }
        type = chooseImpl(type);
        for (Extension extension : extensions) {
            decoder = extension.createDecoder(cacheKey, type);
            if (decoder != null) {
                JsoniterSpi.addNewDecoder(cacheKey, decoder);
                return decoder;
            }
        }
        ClassInfo classInfo = new ClassInfo(type);
        decoder = CodegenImplNative.NATIVE_DECODERS.get(classInfo.clazz);
        if (decoder != null) {
            return decoder;
        }
        addPlaceholderDecoderToSupportRecursiveStructure(cacheKey);
        try {
            Config currentConfig = JsoniterSpi.getCurrentConfig();
            DecodingMode mode = currentConfig.decodingMode();
            if (mode == DecodingMode.REFLECTION_MODE) {
                decoder = ReflectionDecoderFactory.create(classInfo);
                return decoder;
            }
            if (isDoingStaticCodegen == null) {
                try {
                    decoder = (Decoder) Class.forName(cacheKey).newInstance();
                    return decoder;
                } catch (Exception e) {
                    if (mode == DecodingMode.STATIC_MODE) {
                        throw new JsonException("static gen should provide the decoder we need, but failed to create the decoder", e);
                    }
                }
            }
            String source = genSource(mode, classInfo);
            source = "public static java.lang.Object decode_(com.jsoniter.JsonIterator iter) throws java.io.IOException { "
                    + source + "}";
            if ("true".equals(System.getenv("JSONITER_DEBUG"))) {
                System.out.println(">>> " + cacheKey);
                System.out.println(source);
            }
            try {
                generatedClassNames.add(cacheKey);
                if (isDoingStaticCodegen == null) {
                    decoder = DynamicCodegen.gen(cacheKey, source);
                } else {
                    staticGen(cacheKey, source);
                }
                return decoder;
            } catch (Exception e) {
                String msg = "failed to generate decoder for: " + classInfo + " with " + Arrays.toString(classInfo.typeArgs) + ", exception: " + e;
                msg = msg + "\n" + source;
                throw new JsonException(msg, e);
            }
        } finally {
            JsoniterSpi.addNewDecoder(cacheKey, decoder);
        }
    }

    private static void addPlaceholderDecoderToSupportRecursiveStructure(final String cacheKey) {
        JsoniterSpi.addNewDecoder(cacheKey, new Decoder() {
            @Override
            public Object decode(JsonIterator iter) throws IOException {
                Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
                if (this == decoder) {
                    for(int i = 0; i < 30; i++) {
                        decoder = JsoniterSpi.getDecoder(cacheKey);
                        if (this == decoder) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new JsonException(e);
                            }
                        } else {
                            break;
                        }
                    }
                    if (this == decoder) {
                        throw new JsonException("internal error: placeholder is not replaced with real decoder");
                    }
                }
                return decoder.decode(iter);
            }
        });
    }

    public static boolean canStaticAccess(String cacheKey) {
        return generatedClassNames.contains(cacheKey);
    }

    private static Type chooseImpl(Type type) {
        MethodData methodData =  new MethodData(26);
        dictionary.put("Codegen - chooseImpl", methodData);
        Type[] typeArgs = new Type[0];
        Class clazz;
        methodData.branchReached[0] = true;
        if (type instanceof ParameterizedType) {
            methodData.branchReached[1] = true;
            ParameterizedType pType = (ParameterizedType) type;
            clazz = (Class) pType.getRawType();
            typeArgs = pType.getActualTypeArguments();
        } else if (type instanceof WildcardType) {
            methodData.branchReached[2] = true;
            return Object.class;
        } else {
            methodData.branchReached[3] = true;
            clazz = (Class) type;
        }
        methodData.branchReached[4] = true;
        Class implClazz = JsoniterSpi.getTypeImplementation(clazz);
        if (Collection.class.isAssignableFrom(clazz)) {
            methodData.branchReached[5] = true;
            Type compType = Object.class;
            if (typeArgs.length == 0) {
                methodData.branchReached[6] = true;
                // default to List<Object>
            } else if (typeArgs.length == 1) {
                methodData.branchReached[7] = true;
                compType = typeArgs[0];
            } else {
                methodData.branchReached[8] = true;
                throw new IllegalArgumentException(
                        "can not bind to generic collection without argument types, " +
                                "try syntax like TypeLiteral<List<Integer>>{}");
            }
            methodData.branchReached[9] = true;
            if (clazz == List.class) {
                methodData.branchReached[10] = true;
                clazz = implClazz == null ? ArrayList.class: implClazz;
            } else if (clazz == Set.class) {
                methodData.branchReached[11] = true;
                clazz = implClazz == null ? HashSet.class : implClazz;
            }
            methodData.branchReached[12] = true;
            return GenericsHelper.createParameterizedType(new Type[]{compType}, null, clazz);
        }
        if (Map.class.isAssignableFrom(clazz)) {
            methodData.branchReached[13] = true;
            Type keyType = String.class;
            Type valueType = Object.class;
            if (typeArgs.length == 0) {
                methodData.branchReached[14] = true;
                // default to Map<String, Object>
            } else if (typeArgs.length == 2) {
                methodData.branchReached[15] = true;
                keyType = typeArgs[0];
                valueType = typeArgs[1];
            } else {
                methodData.branchReached[16] = true;
                throw new IllegalArgumentException(
                        "can not bind to generic collection without argument types, " +
                                "try syntax like TypeLiteral<Map<String, String>>{}");
            }
            methodData.branchReached[17] = true;
            if (clazz == Map.class) {
                methodData.branchReached[18] = true;
                clazz = implClazz == null ? HashMap.class : implClazz;
            }
            if (keyType == Object.class) {
                methodData.branchReached[19] = true;
                keyType = String.class;
            }
            methodData.branchReached[20] = true;
            DefaultMapKeyDecoder.registerOrGetExisting(keyType);
            return GenericsHelper.createParameterizedType(new Type[]{keyType, valueType}, null, clazz);
        }

        methodData.branchReached[21] = true;
        if (implClazz != null) {
            methodData.branchReached[22] = true;
            if (typeArgs.length == 0) {
                methodData.branchReached[23] = true;
                return implClazz;
            } else {
                methodData.branchReached[24] = true;
                return GenericsHelper.createParameterizedType(typeArgs, null, implClazz);
            }
        }
        methodData.branchReached[25] = true;
        return type;
    }

    private static void staticGen(String cacheKey, String source) throws IOException {
        createDir(cacheKey);
        String fileName = cacheKey.replace('.', '/') + ".java";
        FileOutputStream fileOutputStream = new FileOutputStream(new File(isDoingStaticCodegen.outputDir, fileName));
        try {
            OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
            try {
                staticGen(cacheKey, writer, source);
            } finally {
                writer.close();
            }
        } finally {
            fileOutputStream.close();
        }
    }

    private static void staticGen(String cacheKey, OutputStreamWriter writer, String source) throws IOException {
        String className = cacheKey.substring(cacheKey.lastIndexOf('.') + 1);
        String packageName = cacheKey.substring(0, cacheKey.lastIndexOf('.'));
        writer.write("package " + packageName + ";\n");
        writer.write("public class " + className + " implements com.jsoniter.spi.Decoder {\n");
        writer.write(source);
        writer.write("public java.lang.Object decode(com.jsoniter.JsonIterator iter) throws java.io.IOException {\n");
        writer.write("return decode_(iter);\n");
        writer.write("}\n");
        writer.write("}\n");
    }

    private static void createDir(String cacheKey) {
        String[] parts = cacheKey.split("\\.");
        File parent = new File(isDoingStaticCodegen.outputDir);
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            File current = new File(parent, part);
            current.mkdir();
            parent = current;
        }
    }

    private static String genSource(DecodingMode mode, ClassInfo classInfo) {
        if (classInfo.clazz.isArray()) {
            return CodegenImplArray.genArray(classInfo);
        }
        if (Map.class.isAssignableFrom(classInfo.clazz)) {
            return CodegenImplMap.genMap(classInfo);
        }
        if (Collection.class.isAssignableFrom(classInfo.clazz)) {
            return CodegenImplArray.genCollection(classInfo);
        }
        if (classInfo.clazz.isEnum()) {
            return CodegenImplEnum.genEnum(classInfo);
        }
        ClassDescriptor desc = ClassDescriptor.getDecodingClassDescriptor(classInfo, false);
        if (shouldUseStrictMode(mode, desc)) {
            return CodegenImplObjectStrict.genObjectUsingStrict(desc);
        } else {
            return CodegenImplObjectHash.genObjectUsingHash(desc);
        }
    }

    private static boolean shouldUseStrictMode(DecodingMode mode, ClassDescriptor desc) {
        if (mode == DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_STRICTLY) {
            return true;
        }
        List<Binding> allBindings = desc.allDecoderBindings();
        for (Binding binding : allBindings) {
            if (binding.asMissingWhenNotPresent || binding.asExtraWhenPresent || binding.shouldSkip) {
                // only slice support mandatory tracking
                return true;
            }
        }
        if (desc.asExtraForUnknownProperties) {
            // only slice support unknown field tracking
            return true;
        }
        if (!desc.keyValueTypeWrappers.isEmpty()) {
            return true;
        }
        boolean hasBinding = false;
        for (Binding allBinding : allBindings) {
            if (allBinding.fromNames.length > 0) {
                hasBinding = true;
            }
        }
        if (!hasBinding) {
            // empty object can only be handled by strict mode
            return true;
        }
        return false;
    }

    public static void staticGenDecoders(TypeLiteral[] typeLiterals, CodegenAccess.StaticCodegenTarget staticCodegenTarget) {
        isDoingStaticCodegen = staticCodegenTarget;
        for (TypeLiteral typeLiteral : typeLiterals) {
            gen(typeLiteral.getDecoderCacheKey(), typeLiteral.getType());
        }
    }
}
