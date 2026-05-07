package pos.db;

import pos.model.Product;
import java.sql.*;
import java.util.*;

public class ProductDAO {

    // 🔍 SEARCH PRODUCTS
    public static List<Product> searchByName(String name) {

        List<Product> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {

            String sql = "SELECT * FROM products WHERE LOWER(product_name) LIKE LOWER(?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + name + "%");

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // 📦 GET ALL PRODUCTS (FIXES YOUR ERROR)
    public static List<Product> getAllProducts() {

        List<Product> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {

            String sql = "SELECT * FROM products";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ➕ ADD PRODUCT
    public static void addProduct(Product p) {

        try (Connection conn = DBConnection.getConnection()) {

            String sql = "INSERT INTO products(product_name, price, stock) VALUES(?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, p.getName());
            ps.setDouble(2, p.getPrice());
            ps.setInt(3, p.getStock());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔄 UPDATE STOCK
    public static void updateStock(int id, int stock) {

        try (Connection conn = DBConnection.getConnection()) {

            String sql = "UPDATE products SET stock=? WHERE product_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, stock);
            ps.setInt(2, id);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ❌ DELETE PRODUCT
    public static void deleteProduct(int id) {

        try (Connection conn = DBConnection.getConnection()) {

            String sql = "DELETE FROM products WHERE product_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
