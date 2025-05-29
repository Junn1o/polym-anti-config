package com.junnio.anticonfig.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConfigDiffUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GREEN = "§a"; // Color code for green
    private static final String RED = "§c";   // Color code for red
    private static final String RESET = "§r";  // Reset formatting
    private static final String YELLOW = "§e";

    public static String findDifferences(String filename, String serverContent, String clientContent) throws Exception {
        JsonNode serverJson = MAPPER.readTree(serverContent);
        JsonNode clientJson = MAPPER.readTree(clientContent);

        List<String> differences = new ArrayList<>();
        findDifferences("", serverJson, clientJson, differences);

        if (differences.isEmpty()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        // Left align the filename with a line break
        result.append(filename).append("\n");
        for (String diff : differences) {
            result.append(diff).append("\n");
        }
        return result.toString();
    }

    private static void findDifferences(String path, JsonNode server, JsonNode client, List<String> differences) {
        if (server.equals(client)) {
            return;
        }

        if (server.isObject() && client.isObject()) {
            ObjectNode serverObj = (ObjectNode) server;
            ObjectNode clientObj = (ObjectNode) client;

            Iterator<Map.Entry<String, JsonNode>> serverFields = serverObj.fields();
            while (serverFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = serverFields.next();
                String key = entry.getKey();
                JsonNode serverValue = entry.getValue();
                JsonNode clientValue = clientObj.get(key);

                String currentPath = path.isEmpty() ? key : path + "." + key;

                if (clientValue == null) {
                    differences.add(currentPath + ": " + GREEN + "present in server" + RESET + ", " +
                            RED + "missing in client" + RESET);
                } else if (!serverValue.equals(clientValue)) {
                    if (serverValue.isObject() && clientValue.isObject()) {
                        findDifferences(currentPath, serverValue, clientValue, differences);
                    } else {
                        differences.add(
                                YELLOW + currentPath + ": " +
                                        GREEN + "server:" + serverValue + RESET + ", " +
                                        RED + "client:" + clientValue + RESET);
                    }
                }
            }

            Iterator<String> clientFields = clientObj.fieldNames();
            while (clientFields.hasNext()) {
                String key = clientFields.next();
                if (!serverObj.has(key)) {
                    String currentPath = path.isEmpty() ? key : path + "." + key;
                    differences.add(currentPath + ": " +
                            GREEN + "missing in server" + RESET + ", " +
                            RED + "present in client" + RESET);
                }
            }
        } else {
            differences.add(path + ": " +
                    GREEN + "server:" + server + RESET + ", " +
                    RED + "client:" + client + RESET);
        }
    }
}