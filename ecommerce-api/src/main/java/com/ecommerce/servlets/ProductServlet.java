package com.ecommerce.servlets;

import com.ecommerce.dao.ProductDAO;
import com.ecommerce.models.Product;
import com.ecommerce.util.JwtUtil;
import com.ecommerce.util.ResponseUtil;
import com.ecommerce.util.ValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ProductDAO productDAO = new ProductDAO();
    private static final String PRODUCT_CACHE_KEY = "products:all";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            handleGetAllProducts(req, resp);
        } else {
            // Get specific product
            try {
                Long id = Long.parseLong(path.substring(1));
                handleGetProduct(id, req, resp);
            } catch (NumberFormatException e) {
                ResponseUtil.sendErrorResponse(resp, 400, "Invalid product ID");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isAdmin(req)) {
            ResponseUtil.sendErrorResponse(resp, 403, "Only admins can create products");
            return;
        }

        try {
            Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

            String name = (String) body.get("name");
            String description = (String) body.get("description");
            Object priceObj = body.get("price");
            Object stockObj = body.get("stock");
            Object categoryIdObj = body.get("categoryId");

            if (!ValidationUtil.isNotEmpty(name)) {
                ResponseUtil.sendErrorResponse(resp, 400, "Product name is required");
                return;
            }

            BigDecimal price = new BigDecimal(priceObj.toString());
            Integer stock = Integer.parseInt(stockObj.toString());
            Long categoryId = Long.parseLong(categoryIdObj.toString());

            if (!ValidationUtil.isValidPrice(price.doubleValue()) || stock < 0) {
                ResponseUtil.sendErrorResponse(resp, 400, "Invalid price or stock");
                return;
            }

            Product product = new Product(name, description, price, stock, categoryId);
            if (productDAO.save(product)) {
                invalidateCacheAndRespond(resp, 201, "Product created successfully", product);
            } else {
                ResponseUtil.sendErrorResponse(resp, 500, "Failed to create product");
            }
        } catch (Exception e) {
            ResponseUtil.sendErrorResponse(resp, 400, "Invalid request format");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isAdmin(req)) {
            ResponseUtil.sendErrorResponse(resp, 403, "Only admins can update products");
            return;
        }

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            ResponseUtil.sendErrorResponse(resp, 400, "Product ID is required");
            return;
        }

        try {
            Long id = Long.parseLong(path.substring(1));
            Map<String, Object> body = mapper.readValue(req.getInputStream(), Map.class);

            Product product = productDAO.findById(id);
            if (product == null) {
                ResponseUtil.sendErrorResponse(resp, 404, "Product not found");
                return;
            }

            if (body.containsKey("name")) product.setName((String) body.get("name"));
            if (body.containsKey("description")) product.setDescription((String) body.get("description"));
            if (body.containsKey("price")) product.setPrice(new BigDecimal(body.get("price").toString()));
            if (body.containsKey("stock")) product.setStock(Integer.parseInt(body.get("stock").toString()));

            if (productDAO.update(product)) {
                invalidateCacheAndRespond(resp, 200, "Product updated successfully", product);
            } else {
                ResponseUtil.sendErrorResponse(resp, 500, "Failed to update product");
            }
        } catch (Exception e) {
            ResponseUtil.sendErrorResponse(resp, 400, "Invalid request format");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isAdmin(req)) {
            ResponseUtil.sendErrorResponse(resp, 403, "Only admins can delete products");
            return;
        }

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            ResponseUtil.sendErrorResponse(resp, 400, "Product ID is required");
            return;
        }

        try {
            Long id = Long.parseLong(path.substring(1));
            if (productDAO.delete(id)) {
                invalidateCache();
                ResponseUtil.sendSuccessResponse(resp, 200, "Product deleted successfully", null);
            } else {
                ResponseUtil.sendErrorResponse(resp, 404, "Product not found");
            }
        } catch (NumberFormatException e) {
            ResponseUtil.sendErrorResponse(resp, 400, "Invalid product ID");
        }
    }

    private void handleGetAllProducts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (Jedis jedis = com.ecommerce.config.AppConfig.getRedisConnection()) {
            String cachedData = jedis.get(PRODUCT_CACHE_KEY);
            if (cachedData != null) {
                ResponseUtil.sendSuccessResponse(resp, 200, "Products retrieved from cache", mapper.readValue(cachedData, List.class));
                return;
            }
        } catch (Exception e) {
            // Continue with database query
        }

        List<Product> products = productDAO.findAll();
        try (Jedis jedis = com.ecommerce.config.AppConfig.getRedisConnection()) {
            jedis.setex(PRODUCT_CACHE_KEY, 3600, mapper.writeValueAsString(products));
        } catch (Exception e) {
            // Cache write failed, continue with response
        }
        ResponseUtil.sendSuccessResponse(resp, 200, "Products retrieved successfully", products);
    }

    private void handleGetProduct(Long id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Product product = productDAO.findById(id);
        if (product == null) {
            ResponseUtil.sendErrorResponse(resp, 404, "Product not found");
        } else {
            ResponseUtil.sendSuccessResponse(resp, 200, "Product retrieved successfully", product);
        }
    }

    private void invalidateCacheAndRespond(HttpServletResponse resp, int status, String message, Product product) throws IOException {
        invalidateCache();
        ResponseUtil.sendSuccessResponse(resp, status, message, product);
    }

    private void invalidateCache() {
        try (Jedis jedis = com.ecommerce.config.AppConfig.getRedisConnection()) {
            jedis.del(PRODUCT_CACHE_KEY);
        } catch (Exception e) {
            // Cache deletion failed
        }
    }

    private boolean isAdmin(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String role = JwtUtil.getRoleFromToken(token);
            return "ADMIN".equals(role);
        }
        return false;
    }
}