package pos.ui;

import pos.model.User;
import pos.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ManagerDashboard extends JFrame {

    private User loggedInUser;
    private DefaultTableModel salesModel;
    private DefaultListModel<String> lowStockModel;

    public ManagerDashboard(User user) {

        this.loggedInUser = user;

        setTitle("Manager Dashboard - " + user.getUsername());
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // TOP BAR
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0, 153, 76));

        JLabel welcome = new JLabel(" Manager: " + user.getUsername());
        welcome.setForeground(Color.WHITE);

        JButton logout = new JButton("Logout");
        styleButton(logout);

        logout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        topBar.add(welcome, BorderLayout.WEST);
        topBar.add(logout, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // TABS
        JTabbedPane tabs = new JTabbedPane();

        tabs.add("Dashboard", createDashboardPanel());
        tabs.add("Products", createProductsPanel());
        tabs.add("Employees", createEmployeesPanel());

        add(tabs, BorderLayout.CENTER);

        // LOW STOCK ALERT
        showLowStockAlert();
    }

    // ================= DASHBOARD =================
    private JPanel createDashboardPanel() {

        JPanel panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel();

        String[] filters = {"Today", "This Week", "This Month"};
        JComboBox<String> filterBox = new JComboBox<>(filters);

        top.add(new JLabel("Filter: "));
        top.add(filterBox);

        panel.add(top, BorderLayout.NORTH);

        salesModel = new DefaultTableModel(
                new String[]{"Date", "Total Sales"}, 0
        );

        JTable table = new JTable(salesModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        lowStockModel = new DefaultListModel<>();
        JList<String> lowList = new JList<>(lowStockModel);
        lowList.setBorder(BorderFactory.createTitledBorder("Low Stock"));

        panel.add(new JScrollPane(lowList), BorderLayout.SOUTH);

        loadDashboardData("TODAY");

        filterBox.addActionListener(e -> {
            String s = filterBox.getSelectedItem().toString();
            if (s.contains("Week")) loadDashboardData("WEEK");
            else if (s.contains("Month")) loadDashboardData("MONTH");
            else loadDashboardData("TODAY");
        });

        return panel;
    }

    private void loadDashboardData(String filter) {

        try (Connection conn = DBConnection.getConnection()) {

            String condition;

            if (filter.equals("WEEK"))
                condition = "YEARWEEK(created_at)=YEARWEEK(NOW())";
            else if (filter.equals("MONTH"))
                condition = "MONTH(created_at)=MONTH(NOW())";
            else
                condition = "DATE(created_at)=CURDATE()";

            // SALES
            String sql = "SELECT DATE(created_at), SUM(total) FROM sales WHERE " +
                    condition + " GROUP BY DATE(created_at)";

            ResultSet rs = conn.prepareStatement(sql).executeQuery();

            salesModel.setRowCount(0);

            while (rs.next()) {
                salesModel.addRow(new Object[]{
                        rs.getString(1),
                        rs.getDouble(2)
                });
            }

            // LOW STOCK LIST
            lowStockModel.clear();

            rs = conn.prepareStatement(
                    "SELECT product_name FROM products WHERE stock < 5"
            ).executeQuery();

            while (rs.next()) {
                lowStockModel.addElement("LOW: " + rs.getString(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= PRODUCTS =================
    private JPanel createProductsPanel() {

        JPanel panel = new JPanel(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Name", "Price", "Stock"}, 0
        );

        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();

        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");

        styleButton(add);
        styleButton(edit);
        styleButton(delete);

        btnPanel.add(add);
        btnPanel.add(edit);
        btnPanel.add(delete);

        panel.add(btnPanel, BorderLayout.SOUTH);

        Runnable load = () -> {
            try (Connection conn = DBConnection.getConnection()) {

                model.setRowCount(0);

                ResultSet rs = conn.prepareStatement("SELECT * FROM products").executeQuery();

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getDouble("price"),
                            rs.getInt("stock")
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        load.run();

        // ADD
        add.addActionListener(e -> {
            JTextField name = new JTextField();
            JTextField price = new JTextField();
            JTextField stock = new JTextField();

            Object[] f = {"Name:", name, "Price:", price, "Stock:", stock};

            if (JOptionPane.showConfirmDialog(this, f, "Add Product",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

                try (Connection conn = DBConnection.getConnection()) {

                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO products(product_name,price,stock) VALUES(?,?,?)"
                    );

                    ps.setString(1, name.getText());
                    ps.setDouble(2, Double.parseDouble(price.getText()));
                    ps.setInt(3, Integer.parseInt(stock.getText()));

                    ps.executeUpdate();
                    load.run();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // EDIT
        edit.addActionListener(e -> {

            int row = table.getSelectedRow();
            if (row == -1) return;

            int id = (int) model.getValueAt(row, 0);

            JTextField name = new JTextField(model.getValueAt(row, 1).toString());
            JTextField price = new JTextField(model.getValueAt(row, 2).toString());
            JTextField stock = new JTextField(model.getValueAt(row, 3).toString());

            Object[] f = {"Name:", name, "Price:", price, "Stock:", stock};

            if (JOptionPane.showConfirmDialog(this, f, "Edit",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

                try (Connection conn = DBConnection.getConnection()) {

                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE products SET product_name=?,price=?,stock=? WHERE product_id=?"
                    );

                    ps.setString(1, name.getText());
                    ps.setDouble(2, Double.parseDouble(price.getText()));
                    ps.setInt(3, Integer.parseInt(stock.getText()));
                    ps.setInt(4, id);

                    ps.executeUpdate();
                    load.run();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // DELETE
        delete.addActionListener(e -> {

            int row = table.getSelectedRow();
            if (row == -1) return;

            int id = (int) model.getValueAt(row, 0);

            try (Connection conn = DBConnection.getConnection()) {

                PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM products WHERE product_id=?"
                );

                ps.setInt(1, id);
                ps.executeUpdate();

                load.run();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return panel;
    }

    // ================= EMPLOYEES =================
    private JPanel createEmployeesPanel() {

        JPanel panel = new JPanel(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Username", "Role"}, 0
        );

        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();

        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");

        styleButton(add);
        styleButton(edit);
        styleButton(delete);

        btnPanel.add(add);
        btnPanel.add(edit);
        btnPanel.add(delete);

        panel.add(btnPanel, BorderLayout.SOUTH);

        Runnable load = () -> {
            try (Connection conn = DBConnection.getConnection()) {

                model.setRowCount(0);

                ResultSet rs = conn.prepareStatement("SELECT * FROM users").executeQuery();

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("role")
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        load.run();

        // ADD
        add.addActionListener(e -> {

            JTextField username = new JTextField();
            JTextField password = new JTextField();
            JComboBox<String> role = new JComboBox<>(new String[]{"manager", "cashier"});

            Object[] f = {"Username:", username, "Password:", password, "Role:", role};

            if (JOptionPane.showConfirmDialog(this, f, "Add Employee",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

                try (Connection conn = DBConnection.getConnection()) {

                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO users(username,password,role) VALUES(?,?,?)"
                    );

                    ps.setString(1, username.getText());
                    ps.setString(2, password.getText());
                    ps.setString(3, role.getSelectedItem().toString());

                    ps.executeUpdate();
                    load.run();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // EDIT
        edit.addActionListener(e -> {

            int row = table.getSelectedRow();
            if (row == -1) return;

            int id = (int) model.getValueAt(row, 0);

            JTextField username = new JTextField(model.getValueAt(row, 1).toString());
            JTextField password = new JTextField();
            JComboBox<String> role = new JComboBox<>(new String[]{"manager", "cashier"});
            role.setSelectedItem(model.getValueAt(row, 2));

            Object[] f = {"Username:", username, "New Password:", password, "Role:", role};

            if (JOptionPane.showConfirmDialog(this, f, "Edit",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

                try (Connection conn = DBConnection.getConnection()) {

                    String sql;

                    if (password.getText().isEmpty()) {
                        sql = "UPDATE users SET username=?,role=? WHERE user_id=?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, username.getText());
                        ps.setString(2, role.getSelectedItem().toString());
                        ps.setInt(3, id);
                        ps.executeUpdate();
                    } else {
                        sql = "UPDATE users SET username=?,password=?,role=? WHERE user_id=?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, username.getText());
                        ps.setString(2, password.getText());
                        ps.setString(3, role.getSelectedItem().toString());
                        ps.setInt(4, id);
                        ps.executeUpdate();
                    }

                    load.run();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // DELETE
        delete.addActionListener(e -> {

            int row = table.getSelectedRow();
            if (row == -1) return;

            int id = (int) model.getValueAt(row, 0);

            try (Connection conn = DBConnection.getConnection()) {

                PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM users WHERE user_id=?"
                );

                ps.setInt(1, id);
                ps.executeUpdate();

                load.run();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return panel;
    }

    // ================= LOW STOCK ALERT =================
    private void showLowStockAlert() {

        try (Connection conn = DBConnection.getConnection()) {

            ResultSet rs = conn.prepareStatement(
                    "SELECT product_name FROM products WHERE stock < 5"
            ).executeQuery();

            StringBuilder msg = new StringBuilder();

            while (rs.next()) {
                msg.append(rs.getString(1)).append("\n");
            }

            if (msg.length() > 0) {
                JOptionPane.showMessageDialog(this,
                        "LOW STOCK:\n\n" + msg,
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void styleButton(JButton b) {
        b.setBackground(new Color(0, 153, 76));
        b.setForeground(Color.WHITE);
    }
}
