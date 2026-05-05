import java.sql.*;
import java.util.*;
import java.time.*;

public class POS_DB {

    static final String URL = "jdbc:mysql://localhost:3306/pos_system";
    static final String USER = "root";
    static final String PASS = "1234";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        Connection conn = DriverManager.getConnection(URL, USER, PASS);

        System.out.print("Username: ");
        String username = sc.next();

        System.out.print("Password: ");
        String password = sc.next();

        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE username=? AND password=?"
        );

        ps.setString(1, username);
        ps.setString(2, password);

        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            System.out.println("Login failed");
            return;
        }

        String role = rs.getString("role");
        int userId = rs.getInt("user_id");

        System.out.println("Welcome " + role);

        while (true) {

            if (role.equals("MANAGER")) {

                System.out.println("\n--- MANAGER MENU ---");
                System.out.println("1.View Products");
                System.out.println("2.Add Product");
                System.out.println("3.Make Sale");
                System.out.println("4.View ALL Sales");
                System.out.println("5.Daily Report");
                System.out.println("6.Refund Sale");
                System.out.println("7.Reprint Receipt");
                System.out.println("8.Exit");

                int c = sc.nextInt();

                if (c == 1) showProducts(conn);
                if (c == 2) addProduct(conn);
                if (c == 3) makeSale(conn, userId, username);
                if (c == 4) viewAllSales(conn);
                if (c == 5) dailyReport(conn);
                if (c == 6) refundSale(conn);
                if (c == 7) reprintReceipt(conn);
                if (c == 8) break;
            }

            else {

                System.out.println("\n--- CASHIER MENU ---");
                System.out.println("1.View Products");
                System.out.println("2.Make Sale");
                System.out.println("3.View My Sales");
                System.out.println("4.Exit");

                int c = sc.nextInt();

                if (c == 1) showProducts(conn);
                if (c == 2) makeSale(conn, userId, username);
                if (c == 3) viewMySales(conn, userId);
                if (c == 4) break;
            }
        }

        conn.close();
    }

    static void showProducts(Connection conn) throws Exception {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM products");

        while (rs.next()) {
            int stock = rs.getInt("stock");

            System.out.println(
                    rs.getInt("product_id") + " | " +
                            rs.getString("product_name") +
                            " | Ksh " + rs.getDouble("price") +
                            " | Qty: " + stock +
                            (stock <= 5 ? " ⚠ LOW STOCK" : "")
            );
        }
    }

    static void addProduct(Connection conn) throws Exception {

        sc.nextLine();

        System.out.print("Name: ");
        String name = sc.nextLine();

        System.out.print("Price: ");
        double price = sc.nextDouble();

        System.out.print("Stock: ");
        int stock = sc.nextInt();

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO products(product_name,price,stock) VALUES(?,?,?)"
        );

        ps.setString(1, name);
        ps.setDouble(2, price);
        ps.setInt(3, stock);

        ps.executeUpdate();

        System.out.println("Product added");
    }

    static void makeSale(Connection conn, int userId, String cashierName) throws Exception {

        Map<Integer, Integer> cart = new HashMap<>();
        Map<Integer, Double> priceUsed = new HashMap<>();

        sc.nextLine();

        System.out.print("Sale Type (1=Retail, 2=Wholesale): ");
        int typeChoice = Integer.parseInt(sc.nextLine());

        String saleType = (typeChoice == 2) ? "WHOLESALE" : "RETAIL";

        while (true) {

            System.out.print("Product (done/cancel): ");
            String name = sc.nextLine();

            if (name.equalsIgnoreCase("cancel")) {
                System.out.println("Sale cancelled");
                return;
            }

            if (name.equalsIgnoreCase("done")) break;

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM products WHERE LOWER(product_name) LIKE LOWER(?)"
            );

            ps.setString(1, "%" + name + "%");

            ResultSet rs = ps.executeQuery();

            List<Integer> ids = new ArrayList<>();

            int i = 1;
            while (rs.next()) {
                System.out.println(i + ". " + rs.getString("product_name"));
                ids.add(rs.getInt("product_id"));
                i++;
            }

            if (ids.isEmpty()) continue;

            System.out.print("Choose: ");
            int choice = Integer.parseInt(sc.nextLine());

            int productId = ids.get(choice - 1);

            System.out.print("Qty: ");
            int qty = Integer.parseInt(sc.nextLine());

            double price;

            PreparedStatement gp = conn.prepareStatement(
                    "SELECT price FROM products WHERE product_id=?"
            );
            gp.setInt(1, productId);
            ResultSet pr = gp.executeQuery();
            pr.next();

            double original = pr.getDouble("price");

            if (saleType.equals("WHOLESALE")) {
                System.out.print("Enter wholesale price: ");
                price = Double.parseDouble(sc.nextLine());
            } else {
                price = original;
            }

            cart.put(productId, qty);
            priceUsed.put(productId, price);
        }

        double total = 0;

        PreparedStatement insertSale = conn.prepareStatement(
                "INSERT INTO sales(total,user_id,sale_type) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS
        );

        insertSale.setDouble(1, 0);
        insertSale.setInt(2, userId);
        insertSale.setString(3, saleType);
        insertSale.executeUpdate();

        ResultSet keys = insertSale.getGeneratedKeys();
        keys.next();
        int saleId = keys.getInt(1);

        StringBuilder receipt = new StringBuilder();

        receipt.append("\n=== RECEIPT ===\n");
        receipt.append("Cashier: ").append(cashierName).append("\n");
        receipt.append("Type: ").append(saleType).append("\n");
        receipt.append("Date: ").append(LocalDateTime.now()).append("\n");

        for (int pid : cart.keySet()) {

            int qty = cart.get(pid);
            double price = priceUsed.get(pid);

            double sub = qty * price;
            total += sub;

            PreparedStatement item = conn.prepareStatement(
                    "INSERT INTO sale_items(sale_id,product_id,quantity,subtotal,price_used) VALUES(?,?,?,?,?)"
            );

            item.setInt(1, saleId);
            item.setInt(2, pid);
            item.setInt(3, qty);
            item.setDouble(4, sub);
            item.setDouble(5, price);
            item.executeUpdate();

            PreparedStatement update = conn.prepareStatement(
                    "UPDATE products SET stock=stock-? WHERE product_id=?"
            );

            update.setInt(1, qty);
            update.setInt(2, pid);
            update.executeUpdate();

            receipt.append("Item ").append(pid)
                    .append(" x").append(qty)
                    .append(" = ").append(sub).append("\n");
        }

        PreparedStatement updateSale = conn.prepareStatement(
                "UPDATE sales SET total=? WHERE sale_id=?"
        );

        updateSale.setDouble(1, total);
        updateSale.setInt(2, saleId);
        updateSale.executeUpdate();

        receipt.append("TOTAL: ").append(total);

        System.out.println(receipt);
    }

    static void refundSale(Connection conn) throws Exception {

        System.out.print("Enter Sale ID: ");
        int saleId = sc.nextInt();

        PreparedStatement ps = conn.prepareStatement(
                "SELECT product_id, quantity FROM sale_items WHERE sale_id=?"
        );

        ps.setInt(1, saleId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            PreparedStatement update = conn.prepareStatement(
                    "UPDATE products SET stock=stock+? WHERE product_id=?"
            );
            update.setInt(1, rs.getInt("quantity"));
            update.setInt(2, rs.getInt("product_id"));
            update.executeUpdate();
        }

        PreparedStatement del = conn.prepareStatement(
                "DELETE FROM sales WHERE sale_id=?"
        );
        del.setInt(1, saleId);
        del.executeUpdate();

        System.out.println("Sale refunded");
    }

    static void reprintReceipt(Connection conn) throws Exception {

        System.out.print("Enter Sale ID: ");
        int saleId = sc.nextInt();

        PreparedStatement ps = conn.prepareStatement(
                "SELECT s.*, u.username FROM sales s JOIN users u ON s.user_id=u.user_id WHERE sale_id=?"
        );

        ps.setInt(1, saleId);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) return;

        System.out.println("\n=== RECEIPT ===");
        System.out.println("Cashier: " + rs.getString("username"));
        System.out.println("Date: " + rs.getTimestamp("sale_date"));

        PreparedStatement items = conn.prepareStatement(
                "SELECT * FROM sale_items WHERE sale_id=?"
        );

        items.setInt(1, saleId);
        ResultSet ir = items.executeQuery();

        while (ir.next()) {
            System.out.println("Item " + ir.getInt("product_id") +
                    " x" + ir.getInt("quantity") +
                    " = " + ir.getDouble("subtotal"));
        }

        System.out.println("TOTAL: " + rs.getDouble("total"));
    }

    static void viewMySales(Connection conn, int userId) throws Exception {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM sales WHERE user_id=?"
        );
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            System.out.println("Sale " + rs.getInt("sale_id") +
                    " | " + rs.getDouble("total"));
        }
    }

    static void viewAllSales(Connection conn) throws Exception {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM sales");

        while (rs.next()) {
            System.out.println("Sale " + rs.getInt("sale_id") +
                    " | " + rs.getDouble("total"));
        }
    }

    static void dailyReport(Connection conn) throws Exception {
        Statement st = conn.createStatement();

        ResultSet rs = st.executeQuery(
                "SELECT SUM(total), COUNT(*) FROM sales WHERE DATE(sale_date)=CURDATE()"
        );

        if (rs.next()) {
            System.out.println("Total: " + rs.getDouble(1));
            System.out.println("Transactions: " + rs.getInt(2));
        }
    }
}
