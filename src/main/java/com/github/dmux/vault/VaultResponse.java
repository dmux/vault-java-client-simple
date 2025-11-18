package com.github.dmux.vault;

import com.github.dmux.vault.exception.VaultException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates HTTP response from Vault server.
 * Provides access to status code, response body, headers, and JSON parsing functionality.
 */
public class VaultResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;

    /**
     * Constructs a VaultResponse with the given HTTP response data.
     *
     * @param statusCode HTTP status code
     * @param body Response body as string
     * @param headers Response headers
     */
    public VaultResponse(int statusCode, String body, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }

    /**
     * Gets the HTTP status code.
     *
     * @return HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the response body as string.
     *
     * @return Response body
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the response headers.
     *
     * @return Map of response headers
     */
    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    /**
     * Parses the response body as JSON and returns a Map representation.
     *
     * @return Map containing parsed JSON data
     * @throws VaultException if the body is not valid JSON
     */
    public Map<String, Object> parseJson() throws VaultException {
        if (body == null || body.trim().isEmpty()) {
            throw new VaultException("Cannot parse JSON: response body is empty");
        }

        try {
            JSONObject jsonObject = new JSONObject(body);
            return jsonObjectToMap(jsonObject);
        } catch (JSONException e) {
            throw new VaultException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a JSONObject to a Map recursively.
     *
     * @param jsonObject JSONObject to convert
     * @return Map representation of the JSON object
     */
    private Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) value));
            } else if (value instanceof org.json.JSONArray) {
                map.put(key, jsonArrayToList((org.json.JSONArray) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Converts a JSONArray to a List recursively.
     *
     * @param jsonArray JSONArray to convert
     * @return List representation of the JSON array
     */
    private java.util.List<Object> jsonArrayToList(org.json.JSONArray jsonArray) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                list.add(jsonObjectToMap((JSONObject) value));
            } else if (value instanceof org.json.JSONArray) {
                list.add(jsonArrayToList((org.json.JSONArray) value));
            } else {
                list.add(value);
            }
        }
        return list;
    }
}
