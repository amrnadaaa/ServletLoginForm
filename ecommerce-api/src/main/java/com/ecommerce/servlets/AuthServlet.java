package com.ecommerce.servlets;

import com.ecommerce.dao.UserDAO;
import com.ecommerce.models.User;
import com.ecommerce.util.JwtUtil;
import com.ecommerce.util.PasswordUtil;
import com.ecommerce.util.ResponseUtil;
import com.ecommerce.util.ValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();

        if ("/register".equals(path)) {
            handleRegister(req, resp);
        } else if ("/login".equals(path)) {
            handleLogin(req, resp);
        } else if ("/logout".equals(path)) {
            handleLogout(req, resp);
        } else {
            ResponseUtil.sendErrorResponse(resp, 404, "Endpoint not found");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String firstName = (String) body.get("firstName");
            String lastName = (String) body.get("lastName");

            // Validate inputs
            if (!ValidationUtil.isValidEmail(email)) {
                ResponseUtil.sendErrorResponse(resp, 400, "Invalid email format");
                return;
            }
            if (!ValidationUtil.isValidPassword(password)) {
                ResponseUtil.sendErrorResponse(resp, 400, "Password must be at least 6 characters");
                return;
            }
            if (!ValidationUtil.isNotEmpty(firstName) || !ValidationUtil.isNotEmpty(lastName)) {
                ResponseUtil.sendErrorResponse(resp, 400, "First and last name are required");
                return;
            }

            // Check if user already exists
            if (userDAO.findByEmail(email) != null) {
                ResponseUtil.sendErrorResponse(resp, 409, "Email already registered");
                return;
            }

            // Create new user
            User user = new User(email, PasswordUtil.hashPassword(password), firstName, lastName, "USER");
            if (userDAO.save(user)) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", user.getId());
                data.put("email", user.getEmail());
                data.put("firstName", user.getFirstName());
                data.put("lastName", user.getLastName());
                data.put("role", user.getRole());
                ResponseUtil.sendSuccessResponse(resp, 201, "User registered successfully", data);
            } else {
                ResponseUtil.sendErrorResponse(resp, 500, "Failed to register user");
            }
        } catch (Exception e) {
            ResponseUtil.sendErrorResponse(resp, 400, "Invalid request format");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

            String email = (String) body.get("email");
            String password = (String) body.get("password");

            if (!ValidationUtil.isValidEmail(email) || !ValidationUtil.isNotEmpty(password)) {
                ResponseUtil.sendErrorResponse(resp, 400, "Email and password are required");
                return;
            }

            User user = userDAO.findByEmail(email);
            if (user == null || !PasswordUtil.verifyPassword(password, user.getPassword())) {
                ResponseUtil.sendErrorResponse(resp, 401, "Invalid email or password");
                return;
            }

            String token = JwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", new HashMap<String, Object>() {{
                put("id", user.getId());
                put("email", user.getEmail());
                put("firstName", user.getFirstName());
                put("lastName", user.getLastName());
                put("role", user.getRole());
            }});
            ResponseUtil.sendSuccessResponse(resp, 200, "Login successful", data);
        } catch (Exception e) {
            ResponseUtil.sendErrorResponse(resp, 400, "Invalid request format");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // JWT doesn't require server-side logout, but we can validate token here
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ResponseUtil.sendErrorResponse(resp, 401, "Missing or invalid authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (JwtUtil.isTokenValid(token)) {
            ResponseUtil.sendSuccessResponse(resp, 200, "Logout successful", null);
        } else {
            ResponseUtil.sendErrorResponse(resp, 401, "Invalid token");
        }
    }
}