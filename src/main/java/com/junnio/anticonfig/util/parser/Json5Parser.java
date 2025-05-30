package com.junnio.anticonfig.util.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Primitive;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Json5Parser {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String json5ToString(Path filePath) throws Exception {
        String json5 = Files.readString(filePath);
        Json5Element parsed = Json5.builder(option -> option
                        .allowInvalidSurrogate()
                        .trailingComma()
                        .prettyPrinting()
                        .build())
                .parse(json5);

        Object normalized = convertJson5ElementToJava(parsed);
        return mapper.writeValueAsString(normalized);
    }

    private static Object convertJson5ElementToJava(Json5Element element) {
        if (element instanceof Json5Object) {
            Map<String, Object> map = new HashMap<>();
            Json5Object obj = (Json5Object) element;
            for (Map.Entry<String, Json5Element> entry : obj.entrySet()) {
                map.put(entry.getKey(), convertJson5ElementToJava(entry.getValue()));
            }
            return map;
        } else if (element instanceof Json5Array) {
            List<Object> list = new ArrayList<>();
            Json5Array array = (Json5Array) element;
            for (Json5Element item : array) {
                list.add(convertJson5ElementToJava(item));
            }
            return list;
        } else if (element instanceof Json5Primitive) {
            Json5Primitive primitive = (Json5Primitive) element;
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else if (primitive.isString()) {
                return primitive.getAsString();
            } else if (primitive.isJson5Null()) {
                return null;
            }
        }
        return element.toString();
    }
}