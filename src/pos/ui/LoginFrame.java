package pos.ui;

import pos.db.DBConnection;
import pos.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {

        setTitle("NURU LIGHTS");
        setSize(420, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // TOP PANEL
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(39, 174, 96));
        topPanel.setPreferredSize(new Dimension(400, 70));

        JLabel title = new JLabel("NURU LIGHTS POS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        topPanel.add(title);

        add(topPanel, BorderLayout.NORTH);

        // CENTER PANEL
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(5, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel userLabel = new JLabel("Username");
        usernameField = new JTextField();

        JLabel passLabel = new JLabel("Password");
        passwordField = new JPasswordField();

        JButton loginBtn = new JButton("LOGIN");
        loginBtn.setBackground(new Color(39, 174, 96));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);

        centerPanel.add(userLabel);
        centerPanel.add(usernameField);
        centerPanel.add(passLabel);
        centerPanel.add(passwordField);
        centerPanel.add(loginBtn);

        add(centerPanel, BorderLayout.CENTER);

        // LOGIN BUTTON ACTION
        loginBtn.addActionListener(e -> login());

        setVisible(true);
    }

    private void login() {

        String username = usernameField.getText().trim();
        String password = String.valueOf(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter username and password");
            return;
        }

        try {

            Connection conn = DBConnection.getConnection();

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?"
            );

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            // LOGIN FAILED
            if (!rs.next()) {

                JOptionPane.showMessageDialog(this,
                        "Invalid username or password");

                return;
            }

            // CREATE USER OBJECT
            User user = new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role")
            );

            String role = rs.getString("role");

            // OPEN MANAGER
            if (role.equalsIgnoreCase("manager")) {

                new ManagerDashboard(user).setVisible(true);

            }

            // OPEN CASHIER
            else {

                new CashierView(user).setVisible(true);

            }

            this.dispose();

            conn.close();

        } catch (Exception ex) {

            ex.printStackTrace();

            JOptionPane.showMessageDialog(this,
                    "Database connection failed");

        }
    }

    // MAIN METHOD
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
        });

    }
}
