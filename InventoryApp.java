import java.sql.*;
import java.util.Scanner;

public class InventoryApp {

    static final String URL = "jdbc:mysql://localhost:3306/retail_db";
    static final String USER = "root";
    static final String PASSWORD = "Shubham@123";

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected to retail_db successfully!");
            int choice;

            do {
                System.out.println("\n===== INVENTORY MANAGEMENT =====");
                System.out.println("1. Insert Product Records");
                System.out.println("2. Show Low-Stock Products by Category");
                System.out.println("3. Update Product Stock");
                System.out.println("4. Search and Modify Price");
                System.out.println("5. Total Value and Delete Product");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");
                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1: insertProducts(con); break;
                    case 2: showLowStock(con, sc); break;
                    case 3: updateStock(con, sc); break;
                    case 4: modifyPrice(con, sc); break;
                    case 5: valueAndDelete(con, sc); break;
                    case 6: System.out.println("Goodbye!"); break;
                    default: System.out.println("Invalid choice.");
                }
            } while (choice != 6);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // (a) Insert at least 3 records using PreparedStatement
    static void insertProducts(Connection con) {
        int[] ids = {101, 102, 103};
        String[] names = {"Keyboard", "Mouse", "Notebook"};
        String[] cats = {"Electronics", "Electronics", "Stationery"};
        int[] qtys = {10, 15, 0};
        double[] prices = {500.0, 300.0, 40.0};

        String sql = "INSERT INTO Product VALUES (?, ?, ?, ?, ?)";

        for (int i = 0; i < ids.length; i++) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, ids[i]);
                ps.setString(2, names[i]);
                ps.setString(3, cats[i]);
                ps.setInt(4, qtys[i]);
                ps.setDouble(5, prices[i]);
                ps.executeUpdate();
                System.out.println("Inserted product " + ids[i]);
            } catch (SQLIntegrityConstraintViolationException e) {
                System.out.println("Product ID " + ids[i] + " already exists. Skipped.");
            } catch (SQLException e) {
                System.out.println("Error inserting " + ids[i] + ": " + e.getMessage());
            }
        }
    }

    // (b) Category filter, quantity < 20, sorted by price DESC
    static void showLowStock(Connection con, Scanner sc) {
        System.out.print("Enter category: ");
        String category = sc.nextLine();

        String sql = "SELECT * FROM Product WHERE category = ? AND quantity < 20 ORDER BY price DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category);
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                printProduct(rs);
            }
            if (!found) {
                System.out.println("No products found with quantity below 20 in this category.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // (c) Add to existing stock
    static void updateStock(Connection con, Scanner sc) {
        System.out.print("Enter product ID: ");
        int id = sc.nextInt();
        System.out.print("Enter quantity to add: ");
        int qty = sc.nextInt();
        sc.nextLine();

        String sql = "UPDATE Product SET quantity = quantity + ? WHERE product_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Stock updated successfully.");
            } else {
                System.out.println("Product with ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // (d) Search, then update price if product exists and price > 0
    static void modifyPrice(Connection con, Scanner sc) {
        System.out.print("Enter product ID: ");
        int id = sc.nextInt();
        System.out.print("Enter new price: ");
        double newPrice = sc.nextDouble();
        sc.nextLine();

        String selectSql = "SELECT * FROM Product WHERE product_id = ?";
        String updateSql = "UPDATE Product SET price = ? WHERE product_id = ?";

        try (PreparedStatement sel = con.prepareStatement(selectSql)) {
            sel.setInt(1, id);
            ResultSet rs = sel.executeQuery();

            if (!rs.next()) {
                System.out.println("Product with ID " + id + " does not exist.");
                return;
            }
            if (newPrice <= 0) {
                System.out.println("Price must be greater than zero. Update cancelled.");
                return;
            }

            try (PreparedStatement upd = con.prepareStatement(updateSql)) {
                upd.setDouble(1, newPrice);
                upd.setInt(2, id);
                upd.executeUpdate();
            }

            // Fetch again to show updated details
            sel.setInt(1, id);
            ResultSet updated = sel.executeQuery();
            if (updated.next()) {
                System.out.println("Updated product details:");
                printProduct(updated);
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // (e) Total inventory value, then delete only if quantity is zero
    static void valueAndDelete(Connection con, Scanner sc) {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT SUM(quantity * price) AS total FROM Product")) {
            if (rs.next()) {
                System.out.println("Total inventory value: " + rs.getDouble("total"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        System.out.print("Enter product ID to delete: ");
        int id = sc.nextInt();
        sc.nextLine();

        String checkSql = "SELECT quantity FROM Product WHERE product_id = ?";
        String deleteSql = "DELETE FROM Product WHERE product_id = ?";

        try (PreparedStatement chk = con.prepareStatement(checkSql)) {
            chk.setInt(1, id);
            ResultSet rs = chk.executeQuery();

            if (!rs.next()) {
                System.out.println("Product with ID " + id + " does not exist.");
            } else if (rs.getInt("quantity") != 0) {
                System.out.println("Cannot delete: product still has stock (quantity = "
                        + rs.getInt("quantity") + ").");
            } else {
                try (PreparedStatement del = con.prepareStatement(deleteSql)) {
                    del.setInt(1, id);
                    del.executeUpdate();
                    System.out.println("Product deleted successfully.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void printProduct(ResultSet rs) throws SQLException {
        System.out.println("ID: " + rs.getInt("product_id")
                + " | Name: " + rs.getString("product_name")
                + " | Category: " + rs.getString("category")
                + " | Qty: " + rs.getInt("quantity")
                + " | Price: " + rs.getDouble("price"));
    }
}