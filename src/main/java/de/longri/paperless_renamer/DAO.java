package de.longri.paperless_renamer;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DAO {

    private static final DateTimeFormatter FORMATTER_X = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Connection connection;

    private Map<Integer, String> UserIdMap = new HashMap<>();
    private Map<Integer, String> CorrespondentIdMap = new HashMap<>();
    private Map<Integer, String> DocumenttypeIdMap = new HashMap<>();


    public DAO() throws SQLException {
        String url = String.format("jdbc:postgresql://localhost:%s/%s",
                ENV.POSTGRES_PORT,
                ENV.POSTGRES_DB);

        connection = DriverManager.getConnection(
                url,
                ENV.POSTGRES_USER,
                ENV.POSTGRES_PASSWORD
        );

        loadMaps();
    }

    public void loadMaps() {
        loadUserMap();
        loadCorrospondentMap();
        loadDocumenttypeMap();
    }

    public void loadUserMap() {
        String sql = "SELECT id, username FROM auth_user";
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                UserIdMap.put(id, username);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user map", e);
        }
        System.out.println("User map loaded");
        System.out.println(UserIdMap);
        System.out.println();
    }

    public void loadCorrospondentMap() {
        String sql = "SELECT id, name FROM documents_correspondent";
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                CorrespondentIdMap.put(id, name);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load correspondent map", e);
        }
        System.out.println("correspondent map loaded");
        System.out.println(CorrespondentIdMap);
        System.out.println();
    }

    public void loadDocumenttypeMap() {
        String sql = "SELECT id, name FROM documents_documenttype";
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                DocumenttypeIdMap.put(id, name);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load Documenttype map", e);
        }
        System.out.println("Documenttype map loaded");
        System.out.println(DocumenttypeIdMap);
        System.out.println();
    }

    public String moveChangedFiles(String paperlessLastModified) {
        String sql = "SELECT id, title, created, modified, correspondent_id, document_type_id, archive_filename, owner_id " +
                "FROM documents_document " +
                "WHERE modified > ?";


        Timestamp timestamp = paperlessLastModified != null ? Timestamp.valueOf(paperlessLastModified) : Timestamp.valueOf("2000-01-01 00:00:00");


        try (var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setTimestamp(1, timestamp);
            try (var resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String title = resultSet.getString("title");
                    String created = resultSet.getString("created");
                    String modified = resultSet.getString("modified");
                    int documentTypeId = resultSet.getInt("document_type_id");
                    String archiveFilename = resultSet.getString("archive_filename");
                    int ownerId = resultSet.getInt("owner_id");
                    int correspondentId = resultSet.getInt("correspondent_id");

                    String correspondentName = CorrespondentIdMap.get(correspondentId);
                    String documentTypeName = DocumenttypeIdMap.get(documentTypeId);
                    String userName = UserIdMap.get(ownerId);

                    LocalDateTime createdDateTime = LocalDateTime.parse(created, FORMATTER_X);

                    String generatedArchiveFileName = generateArchiveFileName(title, createdDateTime, correspondentName, documentTypeName, userName);

                    if (archiveFilename.equals(generatedArchiveFileName)) {
                        System.out.println("No change for " + id + " " + title);
                    } else {
                        System.out.println("Change for " + id + " " + title + " from " + archiveFilename + " to " + generatedArchiveFileName);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to process documents", e);
        }
        return LocalDateTime.now().format(FORMATTER);
    }


//    PAPERLESS_FILENAME_FORMAT={created_year}/{created_year}_{created_month}_{created_day}_{title}
//    PAPERLESS_STORAGE_PATH_FORMAT={correspondent}/{document_type}

    final static String CREATED_YEAR = "{created_year}";
    final static String CREATED_MONTH = "{created_month}";
    final static String CREATED_DAY = "{created_day}";
    final static String TITLE = "{title}";
    final static String CORRESPONDENT = "{correspondent}";
    final static String DOCUMENT_TYPE = "{document_type}";
    final static String USER = "{user}";

    private String generateArchiveFileName(String title, LocalDateTime created, String correspondentName, String documentTypeName, String userName) {

        String fileName = ENV.PAPERLESS_STORAGE_PATH_FORMAT + "/" + ENV.PAPERLESS_FILENAME_FORMAT;
        fileName = fileName.replace(CORRESPONDENT, correspondentName == null ? "none_correspondent" : correspondentName);
        fileName = fileName.replace(DOCUMENT_TYPE, documentTypeName == null ? "none_type" : documentTypeName);

        String createdYear = String.valueOf(created.getYear());
        String createdMonth = String.valueOf(created.getMonthValue());
        if (createdMonth.length() == 1) {
            createdMonth = "0" + createdMonth;
        }
        String createdDay = String.valueOf(created.getDayOfMonth());
        if (createdDay.length() == 1) {
            createdDay = "0" + createdDay;
        }

        fileName = fileName.replace(CREATED_YEAR, createdYear);
        fileName = fileName.replace(CREATED_MONTH, createdMonth);
        fileName = fileName.replace(CREATED_DAY, createdDay);

        fileName = fileName.replace(TITLE, title == null ? "none" : title);
        fileName = fileName.replace(USER, userName == null ? "name" : userName);

        return fileName;
    }
}
