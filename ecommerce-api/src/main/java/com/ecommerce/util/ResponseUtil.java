package com.ecommerce.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void sendJsonResponse(HttpServletResponse response, int status, String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", status);
        responseBody.put("message", message);
        responseBody.put("data", data);
        responseBody.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(mapper.writeValueAsString(responseBody));
    }

    public static void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        sendJsonResponse(response, status, message, null);
    }

    public static void sendSuccessResponse(HttpServletResponse response, int status, String message, Object data) throws IOException {
        sendJsonResponse(response, status, message, data);
    }
}