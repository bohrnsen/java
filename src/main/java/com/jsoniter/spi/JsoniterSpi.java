package com.jsoniter.spi;

import java.lang.reflect.*;
import java.util.*;

public class JsoniterSpi {

    static List<Extension> extensions = new ArrayList<Extension>();
    static Map<Class, Class> typeImpls = new HashMap<Class, Class>();
    static volatile Map<String, MapKeyDecoder> mapKeyDecoders = new HashMap<String, MapKeyDecoder>();
    static volatile Map<String, Encoder> encoders = new HashMap<String, Encoder>();
    static volatile Map<String, Decoder> decoders = new HashMap<String, Decoder>();
    static volatile Map<Class, Extension> objectFactories = new HashMap<Class, Extension>();

    public static void registerExtension(Extension extension) {
        if (!extensions.contains(extension)) {
            extensions.add(extension);
        }
    }

    public static boolean deregisterExtension(Extension extension) {
        return extensions.remove(extension);
    }

    public static List<Extension> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    public static void registerMapKeyDecoder(Type mapKeyType, MapKeyDecoder mapKeyDecoder) {
        addNewMapDecoder(TypeLiteral.create(mapKeyType).getDecoderCacheKey(), mapKeyDecoder);
    }

    public synchronized static void addNewMapDecoder(String cacheKey, MapKeyDecoder mapKeyDecoder) {
        HashMap<String, MapKeyDecoder> newCache = new HashMap<String, MapKeyDecoder>(mapKeyDecoders);
        newCache.put(cacheKey, mapKeyDecoder);
        mapKeyDecoders = newCache;
    }

    public static MapKeyDecoder getMapKeyDecoder(String cacheKey) {
        return mapKeyDecoders.get(cacheKey);
    }

    public static void registerTypeImplementation(Class superClazz, Class implClazz) {
        typeImpls.put(superClazz, implClazz);
    }

    public static Class getTypeImplementation(Class superClazz) {
        return typeImpls.get(superClazz);
    }

    public static void registerTypeDecoder(Class clazz, Decoder decoder) {
        addNewDecoder(TypeLiteral.create(clazz).getDecoderCacheKey(), decoder);
    }

    public static void registerTypeDecoder(TypeLiteral typeLiteral, Decoder decoder) {
        addNewDecoder(typeLiteral.getDecoderCacheKey(), decoder);
    }

    public static void registerPropertyDecoder(Class clazz, String field, Decoder decoder) {
        addNewDecoder(field + "@" + TypeLiteral.create(clazz).getDecoderCacheKey(), decoder);
    }

    public static void registerPropertyDecoder(TypeLiteral typeLiteral, String field, Decoder decoder) {
        addNewDecoder(field + "@" + typeLiteral.getDecoderCacheKey(), decoder);
    }

    public static void registerTypeEncoder(Class clazz, Encoder encoder) {
        addNewEncoder(TypeLiteral.create(clazz).getEncoderCacheKey(), encoder);
    }

    public static void registerTypeEncoder(TypeLiteral typeLiteral, Encoder encoder) {
        addNewEncoder(typeLiteral.getDecoderCacheKey(), encoder);
    }

    public static void registerPropertyEncoder(Class clazz, String field, Encoder encoder) {
        addNewEncoder(field + "@" + TypeLiteral.create(clazz).getEncoderCacheKey(), encoder);
    }

    public static void registerPropertyEncoder(TypeLiteral typeLiteral, String field, Encoder encoder) {
        addNewEncoder(field + "@" + typeLiteral.getDecoderCacheKey(), encoder);
    }

    public static Decoder getDecoder(String cacheKey) {
        return decoders.get(cacheKey);
    }

    public synchronized static void addNewDecoder(String cacheKey, Decoder decoder) {
        HashMap<String, Decoder> newCache = new HashMap<String, Decoder>(decoders);
        newCache.put(cacheKey, decoder);
        decoders = newCache;
    }

    public static Encoder getEncoder(String cacheKey) {
        return encoders.get(cacheKey);
    }

    public synchronized static void addNewEncoder(String cacheKey, Encoder encoder) {
        HashMap<String, Encoder> newCache = new HashMap<String, Encoder>(encoders);
        newCache.put(cacheKey, encoder);
        encoders = newCache;
    }

    public static boolean canCreate(Class clazz) {
        if (objectFactories.containsKey(clazz)) {
            return true;
        }
        for (Extension extension : extensions) {
            if (extension.canCreate(clazz)) {
                addObjectFactory(clazz, extension);
                return true;
            }
        }
        return false;
    }

    public static Object create(Class clazz) {
        return objectFactories.get(clazz).create(clazz);
    }

    private synchronized static void addObjectFactory(Class clazz, Extension extension) {
        HashMap<Class, Extension> copy = new HashMap<Class, Extension>(objectFactories);
        copy.put(clazz, extension);
        objectFactories = copy;
    }

    public static ClassDescriptor getDecodingClassDescriptor(Class clazz, boolean includingPrivate) {
        Map<String, Type> lookup = collectTypeVariableLookup(clazz);
        ClassDescriptor desc = new ClassDescriptor();
        desc.clazz = clazz;
        desc.lookup = lookup;
        desc.ctor = getCtor(clazz);
        desc.fields = getFields(lookup, clazz, includingPrivate);
        desc.setters = getSetters(lookup, clazz, includingPrivate);
        desc.bindingTypeWrappers = new ArrayList<WrapperDescriptor>();
        desc.keyValueTypeWrappers = new ArrayList<Method>();
        desc.unwrappers = new ArrayList<UnwrapperDescriptor>();
        for (Extension extension : extensions) {
            extension.updateClassDescriptor(desc);
        }
        for (Binding field : desc.fields) {
            if (field.valueType instanceof Class) {
                Class valueClazz = (Class) field.valueType;
                if (valueClazz.isArray()) {
                    field.valueCanReuse = false;
                    continue;
                }
            }
            field.valueCanReuse = field.valueTypeLiteral.nativeType == null;
        }
        decodingDeduplicate(desc);
        if (includingPrivate) {
            if (desc.ctor.ctor != null) {
                desc.ctor.ctor.setAccessible(true);
            }
            if (desc.ctor.staticFactory != null) {
                desc.ctor.staticFactory.setAccessible(true);
            }
            for (WrapperDescriptor setter : desc.bindingTypeWrappers) {
                setter.method.setAccessible(true);
            }
        }
        for (Binding binding : desc.allDecoderBindings()) {
            if (binding.fromNames == null) {
                binding.fromNames = new String[]{binding.name};
            }
            if (binding.field != null && includingPrivate) {
                binding.field.setAccessible(true);
            }
            if (binding.method != null && includingPrivate) {
                binding.method.setAccessible(true);
            }
            if (binding.decoder != null) {
                JsoniterSpi.addNewDecoder(binding.decoderCacheKey(), binding.decoder);
            }
        }
        return desc;
    }

    public static ClassDescriptor getEncodingClassDescriptor(Class clazz, boolean includingPrivate) {
        Map<String, Type> lookup = collectTypeVariableLookup(clazz);
        ClassDescriptor desc = new ClassDescriptor();
        desc.clazz = clazz;
        desc.lookup = lookup;
        desc.fields = getFields(lookup, clazz, includingPrivate);
        desc.getters = getGetters(lookup, clazz, includingPrivate);
        desc.bindingTypeWrappers = new ArrayList<WrapperDescriptor>();
        desc.keyValueTypeWrappers = new ArrayList<Method>();
        desc.unwrappers = new ArrayList<UnwrapperDescriptor>();
        for (Extension extension : extensions) {
            extension.updateClassDescriptor(desc);
        }
        encodingDeduplicate(desc);
        for (Binding binding : desc.allEncoderBindings()) {
            if (binding.toNames == null) {
                binding.toNames = new String[]{binding.name};
            }
            if (binding.field != null && includingPrivate) {
                binding.field.setAccessible(true);
            }
            if (binding.method != null && includingPrivate) {
                binding.method.setAccessible(true);
            }
            if (binding.encoder != null) {
                JsoniterSpi.addNewEncoder(binding.encoderCacheKey(), binding.encoder);
            }
        }
        return desc;
    }

    private static void decodingDeduplicate(ClassDescriptor desc) {
        HashMap<String, Binding> byName = new HashMap<String, Binding>();
        for (Binding field : desc.fields) {
            for (String fromName : field.fromNames) {
                if (byName.containsKey(fromName)) {
                    throw new JsonException("field decode from same name: " + fromName);
                }
                byName.put(fromName, field);
            }
        }
        ArrayList<Binding> iteratingSetters = new ArrayList<Binding>(desc.setters);
        Collections.reverse(iteratingSetters);
        for (Binding setter : iteratingSetters) {
            for (String fromName : setter.fromNames) {
                Binding existing = byName.get(fromName);
                if (existing == null) {
                    byName.put(fromName, setter);
                    continue;
                }
                if (desc.fields.remove(existing)) {
                    continue;
                }
                if (existing.method != null && existing.method.getName().equals(setter.method.getName())) {
                    // inherited interface setter
                    // iterate in reverse order, so that the setter from child class will be kept
                    desc.setters.remove(existing);
                    continue;
                }
                throw new JsonException("setter decode from same name: " + fromName);
            }
        }
        for (WrapperDescriptor wrapper : desc.bindingTypeWrappers) {
            for (Binding param : wrapper.parameters) {
                for (String fromName : param.fromNames) {
                    Binding existing = byName.get(fromName);
                    if (existing == null) {
                        byName.put(fromName, param);
                        continue;
                    }
                    if (desc.fields.remove(existing)) {
                        continue;
                    }
                    if (desc.setters.remove(existing)) {
                        continue;
                    }
                    throw new JsonException("wrapper parameter decode from same name: " + fromName);
                }
            }
        }
        for (Binding param : desc.ctor.parameters) {
            for (String fromName : param.fromNames) {
                Binding existing = byName.get(fromName);
                if (existing == null) {
                    byName.put(fromName, param);
                    continue;
                }
                if (desc.fields.remove(existing)) {
                    continue;
                }
                if (desc.setters.remove(existing)) {
                    continue;
                }
                throw new JsonException("ctor parameter decode from same name: " + fromName);
            }
        }
    }

    private static void encodingDeduplicate(ClassDescriptor desc) {
        HashMap<String, Binding> byName = new HashMap<String, Binding>();
        for (Binding field : desc.fields) {
            for (String toName : field.toNames) {
                if (byName.containsKey(toName)) {
                    throw new JsonException("field encode to same name: " + toName);
                }
                byName.put(toName, field);
            }
        }

        for (Binding getter : new ArrayList<Binding>(desc.getters)) {
            for (String toName : getter.toNames) {
                Binding existing = byName.get(toName);
                if (existing == null) {
                    byName.put(toName, getter);
                    continue;
                }
                if (desc.fields.remove(existing)) {
                    continue;
                }
                if (existing.method != null && existing.method.getName().equals(getter.method.getName())) {
                    // inherited interface getter
                    desc.getters.remove(getter);
                    continue;
                }
                throw new JsonException("field encode to same name: " + toName);
            }
        }
    }

    private static ConstructorDescriptor getCtor(Class clazz) {
        ConstructorDescriptor cctor = new ConstructorDescriptor();
        if (canCreate(clazz)) {
            cctor.objectFactory = objectFactories.get(clazz);
            return cctor;
        }
        try {
            cctor.ctor = clazz.getDeclaredConstructor();
        } catch (Exception e) {
            cctor.ctor = null;
        }
        return cctor;
    }

    private static List<Binding> getFields(Map<String, Type> lookup, Class clazz, boolean includingPrivate) {
        ArrayList<Binding> bindings = new ArrayList<Binding>();
        for (Field field : getAllFields(clazz, includingPrivate)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            if (!includingPrivate && !Modifier.isPublic(field.getType().getModifiers())) {
                continue;
            }
            if (includingPrivate) {
                field.setAccessible(true);
            }
            Binding binding = createBindingFromField(lookup, clazz, field);
            bindings.add(binding);
        }
        return bindings;
    }

    private static Binding createBindingFromField(Map<String, Type> lookup, Class clazz, Field field) {
        try {
            Binding binding = new Binding(clazz, lookup, field.getGenericType());
            binding.fromNames = new String[]{field.getName()};
            binding.toNames = new String[]{field.getName()};
            binding.name = field.getName();
            binding.annotations = field.getAnnotations();
            binding.field = field;
            return binding;
        } catch (Exception e) {
            throw new JsonException("failed to create binding for field: " + field, e);
        }
    }

    private static List<Field> getAllFields(Class clazz, boolean includingPrivate) {
        List<Field> allFields = Arrays.asList(clazz.getFields());
        if (includingPrivate) {
            allFields = new ArrayList<Field>();
            Class current = clazz;
            while (current != null) {
                allFields.addAll(Arrays.asList(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
        }
        return allFields;
    }

    private static List<Binding> getSetters(Map<String, Type> lookup, Class clazz, boolean includingPrivate) {
        ArrayList<Binding> setters = new ArrayList<Binding>();
        for (Method method : getAllMethods(clazz, includingPrivate)) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String methodName = method.getName();
            if (methodName.length() < 4) {
                continue;
            }
            if (!methodName.startsWith("set")) {
                continue;
            }
            Type[] paramTypes = method.getGenericParameterTypes();
            if (paramTypes.length != 1) {
                continue;
            }
            if (!includingPrivate && !Modifier.isPublic(method.getParameterTypes()[0].getModifiers())) {
                continue;
            }
            if (includingPrivate) {
                method.setAccessible(true);
            }
            try {
                String fromName = translateSetterName(methodName);
                Binding binding = new Binding(clazz, lookup, paramTypes[0]);
                binding.fromNames = new String[]{fromName};
                binding.name = fromName;
                binding.method = method;
                binding.annotations = method.getAnnotations();
                setters.add(binding);
            } catch (Exception e) {
                throw new JsonException("failed to create binding from setter: " + method, e);
            }
        }
        return setters;
    }

    private static List<Method> getAllMethods(Class clazz, boolean includingPrivate) {
        List<Method> allMethods = Arrays.asList(clazz.getMethods());
        if (includingPrivate) {
            allMethods = new ArrayList<Method>();
            Class current = clazz;
            while (current != null) {
                allMethods.addAll(Arrays.asList(current.getDeclaredMethods()));
                current = current.getSuperclass();
            }
        }
        return allMethods;
    }

    private static String translateSetterName(String methodName) {
        if (!methodName.startsWith("set")) {
            return null;
        }
        String fromName = methodName.substring("set".length());
        char[] fromNameChars = fromName.toCharArray();
        fromNameChars[0] = Character.toLowerCase(fromNameChars[0]);
        fromName = new String(fromNameChars);
        return fromName;
    }

    private static List<Binding> getGetters(Map<String, Type> lookup, Class clazz, boolean includingPrivate) {
        ArrayList<Binding> getters = new ArrayList<Binding>();
        for (Method method : getAllMethods(clazz, includingPrivate)) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String methodName = method.getName();
            if ("getClass".equals(methodName)) {
                continue;
            }
            if (methodName.length() < 4) {
                continue;
            }
            if (!methodName.startsWith("get")) {
                continue;
            }
            if (method.getGenericParameterTypes().length != 0) {
                continue;
            }
            String toName = methodName.substring("get".length());
            char[] fromNameChars = toName.toCharArray();
            fromNameChars[0] = Character.toLowerCase(fromNameChars[0]);
            toName = new String(fromNameChars);
            Binding getter = new Binding(clazz, lookup, method.getGenericReturnType());
            getter.toNames = new String[]{toName};
            getter.name = toName;
            getter.method = method;
            getter.annotations = method.getAnnotations();
            getters.add(getter);
        }
        return getters;
    }

    public static void dump() {
        for (String cacheKey : decoders.keySet()) {
            System.err.println(cacheKey);
        }
        for (String cacheKey : encoders.keySet()) {
            System.err.println(cacheKey);
        }
    }

    private static Map<String, Type> collectTypeVariableLookup(Type type) {
        HashMap<String, Type> vars = new HashMap<String, Type>();
        if (null == type) {
            return vars;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] actualTypeArguments = pType.getActualTypeArguments();
            Class clazz = (Class) pType.getRawType();
            for (int i = 0; i < clazz.getTypeParameters().length; i++) {
                TypeVariable variable = clazz.getTypeParameters()[i];
                vars.put(variable.getName() + "@" + clazz.getCanonicalName(), actualTypeArguments[i]);
            }
            vars.putAll(collectTypeVariableLookup(clazz.getGenericSuperclass()));
            return vars;
        }
        if (type instanceof Class) {
            Class clazz = (Class) type;
            vars.putAll(collectTypeVariableLookup(clazz.getGenericSuperclass()));
            return vars;
        }
        throw new JsonException("unexpected type: " + type);
    }
}
