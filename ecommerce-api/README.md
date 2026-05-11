# E-Commerce REST API

A complete Servlet-based REST API for E-commerce with JWT authentication, MySQL database, and Redis caching.

## Features

✅ **Authentication & Authorization**
- User registration and login
- JWT token-based authentication
- Role-based access control (Admin/User)
- Password hashing with SHA-256 and salt

✅ **Product Management**
- View all products (with Redis caching)
- Add/Update/Delete products (Admin only)
- Search and filter products
- Category management

✅ **Shopping Cart**
- Add items to cart
- View cart with totals
- Update quantities
- Remove items

✅ **Orders**
- Place orders from cart
- View order history
- Order status tracking
- Admin order management

✅ **Payments**
- Multiple payment methods (Cash, Card)
- Payment status tracking
- Rate limiting to prevent duplicate payments
- Payment history

## Tech Stack

- **Server**: Servlet API 4.0
- **Database**: MySQL 8.0
- **Cache**: Redis
- **Authentication**: JWT (jjwt)
- **JSON**: Jackson
- **Build**: Maven
- **Java Version**: 11+

## Database Setup

```sql
CREATE DATABASE ecommerce_db;
USE ecommerce_db;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'USER') DEFAULT 'USER',
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    category_id BIGINT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE carts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    total_amount DECIMAL(10, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES carts(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
    shipping_address VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    method ENUM('CASH', 'CARD') NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    details VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
db.url=jdbc:mysql://localhost:3306/ecommerce_db
db.user=root
db.password=password
db.driver=com.mysql.cj.jdbc.Driver

# Redis
redis.host=localhost
redis.port=6379

# JWT
jwt.secret=your_super_secret_key_that_is_at_least_32_characters_long_for_HS256
jwt.expiration=86400000
```

## Building & Running

```bash
# Build with Maven
mvn clean package

# Deploy to Tomcat
# Copy target/ecommerce-api.war to Tomcat webapps directory

# Or run with embedded server
mvn tomcat7:run
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/logout` - Logout user

### Products
- `GET /api/products` - Get all products (cached)
- `GET /api/products/{id}` - Get specific product
- `POST /api/products` - Create product (Admin)
- `PUT /api/products/{id}` - Update product (Admin)
- `DELETE /api/products/{id}` - Delete product (Admin)

### Categories
- `GET /api/categories` - Get all categories
- `POST /api/categories` - Create category (Admin)
- `PUT /api/categories/{id}` - Update category (Admin)
- `DELETE /api/categories/{id}` - Delete category (Admin)

### Cart
- `GET /api/cart` - View cart
- `POST /api/cart/items` - Add to cart
- `PUT /api/cart/items/{itemId}` - Update cart item
- `DELETE /api/cart/items/{itemId}` - Remove from cart

### Orders
- `GET /api/orders` - View user orders
- `POST /api/orders` - Create order from cart
- `PUT /api/orders/{id}` - Update order status (Admin)

### Payments
- `POST /api/payments` - Process payment
- `GET /api/payments/{orderId}` - Get payment details

## Authentication

Include JWT token in requests:

```
Authorization: Bearer <your_jwt_token>
```

## Error Handling

All responses follow consistent format:

```json
{
  "status": 200,
  "message": "Success message",
  "data": {},
  "timestamp": 1234567890
}
```

## License

MIT