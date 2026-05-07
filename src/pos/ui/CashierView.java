package pos.ui;

import pos.model.User;
import pos.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CashierView extends JFrame {

    private User loggedInUser;

    private DefaultTableModel productModel;
    private DefaultTableModel cartModel;

    private JTable productTable;
    private JLabel totalLabel;

    private Map<Integer, Double> priceMap = new HashMap<>();

    private JTextField searchField;
    private JTextField quickAddField;

    public CashierView(User user) {

        this.loggedInUser = user;

        setTitle("Cashier View - " + user.getUsername());
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ================= TOP BAR =================
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0, 153, 76));
        topBar.setPreferredSize(new Dimension(1000, 50));

        JLabel userLabel = new JLabel("  Cashier: " + user.getUsername());
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton logoutBtn = new JButton("Logout");
        styleButton(logoutBtn);

        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        topBar.add(userLabel, BorderLayout.WEST);
        topBar.add(logoutBtn, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ================= SPLIT =================
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(450);

        // ================= PRODUCTS =================
        JPanel left = new JPanel(new BorderLayout());

        productModel = new DefaultTableModel(
                new String[]{"ID", "Name", "Price", "Stock"}, 0
        );

        productTable = new JTable(productModel);

        loadProducts();

        left.add(new JScrollPane(productTable), BorderLayout.CENTER);

        JPanel topInputs = new JPanel(new GridLayout(2, 1));

        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createTitledBorder("Search Product"));

        quickAddField = new JTextField();
        quickAddField.setBorder(BorderFactory.createTitledBorder("Quick Add (type name + Enter)"));

        topInputs.add(searchField);
        topInputs.add(quickAddField);

        left.add(topInputs, BorderLayout.NORTH);

        JButton addBtn = new JButton("Add to Cart");
        styleButton(addBtn);
        left.add(addBtn, BorderLayout.SOUTH);

        splitPane.setLeftComponent(left);

        // ================= CART =================
        JPanel right = new JPanel(new BorderLayout());

        cartModel = new DefaultTableModel(
                new String[]{"ID", "Qty", "Price", "Subtotal"}, 0
        );

        JTable cartTable = new JTable(cartModel);

        right.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());

        totalLabel = new JLabel("Total: 0.00 KSh");

        JButton checkoutBtn = new JButton("CHECKOUT");
        styleButton(checkoutBtn);

        bottom.add(totalLabel, BorderLayout.WEST);
        bottom.add(checkoutBtn, BorderLayout.EAST);

        right.add(bottom, BorderLayout.SOUTH);

        splitPane.setRightComponent(right);

        add(splitPane, BorderLayout.CENTER);

        // ================= SEARCH =================
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                searchProducts(searchField.getText());
            }
        });

        // ================= QUICK ADD =================
        quickAddField.addActionListener(e -> quickAdd());

        // ================= ADD BUTTON =================
        addBtn.addActionListener(e -> addSelectedProduct());

        // ================= CHECKOUT =================
        checkoutBtn.addActionListener(e -> checkout());
    }

    // ================= LOAD PRODUCTS =================
    private void loadProducts() {

        try (Connection conn = DBConnection.getConnection()) {

            String sql = "SELECT * FROM products";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            productModel.setRowCount(0);
            priceMap.clear();

            while (rs.next()) {

                int id = rs.getInt("product_id");
                double price = rs.getDouble("price");

                productModel.addRow(new Object[]{
                        id,
                        rs.getString("product_name"),
                        price,
                        rs.getInt("stock")
                });

                priceMap.put(id, price);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SEARCH =================
    private void searchProducts(String text) {

        try (Connection conn = DBConnection.getConnection()) {

            String sql = "SELECT * FROM products WHERE LOWER(product_name) LIKE LOWER(?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, "%" + text + "%");

            ResultSet rs = ps.executeQuery();

            productModel.setRowCount(0);

            while (rs.next()) {

                int id = rs.getInt("product_id");
                double price = rs.getDouble("price");

                productModel.addRow(new Object[]{
                        id,
                        rs.getString("product_name"),
                        price,
                        rs.getInt("stock")
                });

                priceMap.put(id, price);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= QUICK ADD =================
    private void quickAdd() {

        String name = quickAddField.getText().trim();

        if (name.isEmpty()) return;

        try (Connection conn = DBConnection.getConnection()) {

            String sql = "SELECT * FROM products WHERE LOWER(product_name)=LOWER(?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, name);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                int id = rs.getInt("product_id");
                double price = rs.getDouble("price");

                String qtyStr = JOptionPane.showInputDialog("Qty:");
                int qty = Integer.parseInt(qtyStr);

                cartModel.addRow(new Object[]{
                        id, qty, price, qty * price
                });

                updateTotal();

            } else {
                JOptionPane.showMessageDialog(this, "Product not found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= ADD SELECTED =================
    private void addSelectedProduct() {

        int row = productTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select product");
            return;
        }

        int id = (int) productModel.getValueAt(row, 0);
        double price = priceMap.get(id);

        String qtyStr = JOptionPane.showInputDialog("Qty:");
        int qty = Integer.parseInt(qtyStr);

        cartModel.addRow(new Object[]{
                id, qty, price, qty * price
        });

        updateTotal();
    }

    // ================= TOTAL =================
    private void updateTotal() {

        double total = 0;

        for (int i = 0; i < cartModel.getRowCount(); i++) {
            total += (double) cartModel.getValueAt(i, 3);
        }

        totalLabel.setText("Total: " + total + " KSh");
    }

    // ================= CHECKOUT =================
    private void checkout() {

        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart empty");
            return;
        }

        String[] options = {"Retail", "Wholesale"};

        int type = JOptionPane.showOptionDialog(
                this,
                "Sale Type",
                "Select",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        String saleType = (type == 1) ? "WHOLESALE" : "RETAIL";

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            PreparedStatement sale = conn.prepareStatement(
                    "INSERT INTO sales(total,user_id,sale_type,created_at) VALUES(?,?,?,NOW())",
                    Statement.RETURN_GENERATED_KEYS
            );

            sale.setDouble(1, 0);
            sale.setInt(2, loggedInUser.getId());
            sale.setString(3, saleType);

            sale.executeUpdate();

            ResultSet rs = sale.getGeneratedKeys();
            rs.next();

            int saleId = rs.getInt(1);

            double total = 0;

            StringBuilder receipt = new StringBuilder();
            receipt.append("=== RECEIPT ===\n");
            receipt.append("Cashier: ").append(loggedInUser.getUsername()).append("\n");
            receipt.append("Type: ").append(saleType).append("\n");
            receipt.append("Date: ").append(LocalDateTime.now()).append("\n\n");

            for (int i = 0; i < cartModel.getRowCount(); i++) {

                int pid = (int) cartModel.getValueAt(i, 0);
                int qty = (int) cartModel.getValueAt(i, 1);

                double price;
                double sub;

                if (saleType.equals("WHOLESALE")) {

                    String wp = JOptionPane.showInputDialog("Wholesale price for " + pid);
                    price = Double.parseDouble(wp);
                    sub = qty * price;

                } else {

                    price = (double) cartModel.getValueAt(i, 2);
                    sub = (double) cartModel.getValueAt(i, 3);
                }

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

                PreparedStatement stock = conn.prepareStatement(
                        "UPDATE products SET stock=stock-? WHERE product_id=?"
                );

                stock.setInt(1, qty);
                stock.setInt(2, pid);
                stock.executeUpdate();

                receipt.append(pid)
                        .append(" x ")
                        .append(qty)
                        .append(" = ")
                        .append(sub)
                        .append("\n");
            }

            PreparedStatement up = conn.prepareStatement(
                    "UPDATE sales SET total=? WHERE sale_id=?"
            );

            up.setDouble(1, total);
            up.setInt(2, saleId);
            up.executeUpdate();

            conn.commit();

            receipt.append("\nTOTAL: ").append(total);

            printReceipt(receipt.toString());

            JTextArea area = new JTextArea(receipt.toString());
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));

            JOptionPane.showMessageDialog(this, new JScrollPane(area));

            cartModel.setRowCount(0);
            updateTotal();
            loadProducts();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= PRINT RECEIPT (FIXED PLACE!) =================
    private void printReceipt(String text) {

        PrinterJob job = PrinterJob.getPrinterJob();

        job.setPrintable((g, f, p) -> {

            if (p > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.drawString(text, 50, 50);

            return Printable.PAGE_EXISTS;
        });

        try {
            job.print();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= STYLE BUTTON =================
    private void styleButton(JButton btn) {
        btn.setBackground(new Color(0, 153, 76));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
    }
}
