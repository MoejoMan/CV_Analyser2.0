import java.sql.*;
import java.io.File;
import java.util.List;
import java.util.Scanner;

public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:resumes.db";
    private static final String JOB_DESCRIPTIONS_PATH = "job_descriptions/";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    public static void createSchema() {
        String createResumesTable = "CREATE TABLE IF NOT EXISTS resumes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "email TEXT, "
                + "phone TEXT, "
                + "skills TEXT, "
                + "education TEXT, "
                + "work_experience TEXT"
                + ");";

        String createJobDescriptionsTable = "CREATE TABLE IF NOT EXISTS job_descriptions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT, "
                + "description TEXT"
                + ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createResumesTable);
            stmt.execute(createJobDescriptionsTable);
            System.out.println("Database schema created successfully.");
        } catch (SQLException e) {
            System.out.println("Error creating schema: " + e.getMessage());
        }
    }

    public static void loadJobDescriptions(List<String> jobDescriptionPaths) {
        try (Connection conn = connect()) {
            for (String filePath : jobDescriptionPaths) {
                File jobFile = new File(filePath.trim());

                if (jobFile.exists() && jobFile.isFile()) {
                    StringBuilder jobDesc = new StringBuilder();
                    try (Scanner scanner = new Scanner(jobFile)) {
                        while (scanner.hasNextLine()) {
                            jobDesc.append(scanner.nextLine()).append("\n");
                        }
                    }

                    String insertSQL = "INSERT INTO job_descriptions (title, description) VALUES (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                        pstmt.setString(1, jobFile.getName());  // Use filename as the title
                        pstmt.setString(2, jobDesc.toString());
                        pstmt.executeUpdate();
                        System.out.println("Inserted job description: " + jobFile.getName());
                    }
                } else {
                    System.out.println("Job description file not found: " + filePath);
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading job descriptions: " + e.getMessage());
        }
    }
}

