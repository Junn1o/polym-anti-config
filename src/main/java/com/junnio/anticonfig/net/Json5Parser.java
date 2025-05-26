package com.junnio.anticonfig.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Options;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Json5Parser {
    public static String json5ToString(Path filePath) throws Exception {
        // Read and parse JSON5 content
        String json5 = Files.readString(filePath);
        Json5Element parsed = Json5.builder(option -> option
                        .allowInvalidSurrogate()
                        .trailingComma()
                        .prettyPrinting()
                        .build())
                .parse(json5);

        // Convert Json5Element to a standard Java Map
        Map<String, Object> map = convertJson5ElementToMap(parsed);

        // Use ObjectMapper to convert the map to JSON string
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(map);
    }

    private static Map<String, Object> convertJson5ElementToMap(Json5Element element) {
        if (element instanceof Json5Object) {
            Json5Object obj = (Json5Object) element;
            Map<String, Object> map = new HashMap<>();

            for (Map.Entry<String, Json5Element> entry : obj.entrySet()) {
                Json5Element value = entry.getValue();
                map.put(entry.getKey(), value.toString());
            }

            return map;
        }
        throw new IllegalArgumentException("Root element must be a Json5Object");
    }
}

