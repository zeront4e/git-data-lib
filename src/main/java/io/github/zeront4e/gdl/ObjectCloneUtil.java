package io.github.zeront4e.gdl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

class ObjectCloneUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Creates a deep copy of the given object using Jackson's ObjectMapper.
     * @param object The object to be cloned.
     * @return A deep copy of the given object.
     * @param <Type> The type of the object to be cloned.
     * @throws Exception If there's an error serializing or deserializing the object.
     */
    public static <Type> Type deepCloneObject(Type object) throws Exception {
        if(object == null)
            return null;

        return (Type) OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(object), object.getClass());
    }

    /**
     * Creates a deep copy of the given object using Jackson's ObjectMapper.
     * @param object The object to be cloned.
     * @param parameterizedClasses The parameterized types of the object to be cloned.
     * @return A deep copy of the given object.
     * @param <Type> The type of the object to be cloned.
     * @throws Exception If there's an error serializing or deserializing the object.
     */
    public static <Type> Type deepCloneParameterizedObject(Type object, Class<?> ...parameterizedClasses) throws Exception {
        if(object == null)
            return null;

        JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(object.getClass(),
                parameterizedClasses);

        String serializedType = OBJECT_MAPPER.writeValueAsString(object);

        return OBJECT_MAPPER.readValue(serializedType, javaType);
    }
}
