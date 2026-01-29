
package com.junnio.anticonfig.util.parser;

import com.junnio.anticonfig.util.ConfigParserUtils;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Primitive;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Json5Parser {
    public static String json5ToString(Path filePath) throws Exception {
        String json5 = Files.readString(filePath);
        Json5Element parsed = Json5.builder(option -> option
                        .allowInvalidSurrogate()
                        .trailingComma()
                        .build())
                .parse(json5);

        Object normalized = convertJson5ElementToJava(parsed);
        return ConfigParserUtils.getMapper().writeValueAsString(normalized);
    }

    private static Object convertJson5ElementToJava(Json5Element element) {
        if (element == null) {
            return null;
        }

        if (element instanceof Json5Object) {
            return convertJson5ObjectToMap((Json5Object) element);
        } else if (element instanceof Json5Array) {
            return convertJson5ArrayToList((Json5Array) element);
        } else if (element instanceof Json5Primitive) {
            return convertJson5PrimitiveToJava((Json5Primitive) element);
        }

        return element.toString();
    }

    private static Map<String, Object> convertJson5ObjectToMap(Json5Object obj) {
        TreeMap<String, Object> map = new TreeMap<>();
        for (Map.Entry<String, Json5Element> entry : obj.entrySet()) {
            map.put(entry.getKey(), convertJson5ElementToJava(entry.getValue()));
        }
        return map;
    }

    private static List<Object> convertJson5ArrayToList(Json5Array array) {
        List<Object> list = new ArrayList<>(array.size());
        for (Json5Element item : array) {
            list.add(convertJson5ElementToJava(item));
        }
        return list;
    }

    private static Object convertJson5PrimitiveToJava(Json5Primitive primitive) {
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        } else if (primitive.isNumber()) {
            Number num = primitive.getAsNumber();
            String strValue = num.toString();
            // Use the shared normalizer to ensure consistent number handling
            return ConfigParserUtils.normalizeValue(strValue);
        } else if (primitive.isString()) {
            return primitive.getAsString();
        } else if (primitive.isJson5Null()) {
            return null;
        }
        return primitive.toString();
    }
}