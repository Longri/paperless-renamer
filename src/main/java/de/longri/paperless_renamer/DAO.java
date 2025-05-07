package de.longri.paperless_renamer;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DAO {

    private static final DateTimeFormatter FORMATTER_X = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX");
    private static final DateTimeFormatter FORMATTER_X_2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSX");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Connection connection;

    private Map<Integer, String> UserIdMap = new HashMap<>();
    private Map<Integer, String> CorrespondentIdMap = new HashMap<>();
    private Map<Integer, String> DocumenttypeIdMap = new HashMap<>();
    private Map<Integer, String> UserTagIdMap = new HashMap<>();

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
        loadUserTagMap();
    }

    public void loadUserTagMap() {
        String sql = "SELECT id, name FROM documents_tag";
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                for (String usrName : UserIdMap.values()) {
                    if (name.equals(usrName))
                        UserTagIdMap.put(id, name);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user Tag map", e);
        }
        System.out.println("User Tag map loaded");
        System.out.println(UserTagIdMap);
        System.out.println();
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


        LocalDateTime lastModified = paperlessLastModified != null ? parseDateTime(paperlessLastModified) : parseDateTime("2000-01-01 00:00:00");
        Timestamp timestamp = Timestamp.valueOf(lastModified);

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


                    //simplify created date
                    // Regex: Suche den Punkt gefolgt von genau 5 Ziffern, vor dem + oder - der Zeitzone
//                    Pattern pattern = Pattern.compile("(\\.\\d{2})\\d{3}(?=[+-]\\d{2})");
                    Pattern pattern = Pattern.compile("(\\.\\d{2})\\d+(?=[+-]\\d{2})");

                    Matcher matcher = pattern.matcher(created);

                    // Ersetze die letzten 3 Ziffern durch nichts, behalte die ersten 2
                    created = matcher.replaceFirst("$1");


                    //check document have user name Tag
                    checkDocumentUserTag(id);

                    LocalDateTime createdDateTime = parseDateTime(created);

                    String fileExtension = archiveFilename.substring(archiveFilename.lastIndexOf(".") + 1);

                    String generatedArchiveFileName = generateArchiveFileName(title, createdDateTime, correspondentName, documentTypeName, userName, fileExtension);

                    if (archiveFilename.equals(generatedArchiveFileName)) {
                        System.out.println("No change for " + id + " " + title);
                    } else {
                        System.out.println("Change for " + id + " " + title + " from " + archiveFilename + " to " + generatedArchiveFileName);
                        moveFile(id, archiveFilename, generatedArchiveFileName);
                    }


                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to process documents", e);
        }
        return LocalDateTime.now().format(FORMATTER);
    }


    private static LocalDateTime parseDateTime(String dateTime) {

        LocalDateTime localDateTime = null;
        try {
            localDateTime = LocalDateTime.parse(dateTime, FORMATTER_X);
        } catch (Exception ignore) {
        }

        if (localDateTime == null) {
            try {
                localDateTime = LocalDateTime.parse(dateTime, FORMATTER);
            } catch (Exception ignore) {
            }
        }

        if (localDateTime == null) {
            try {
                localDateTime = LocalDateTime.parse(dateTime, FORMATTER_X_2);
            } catch (Exception ignore) {
            }
        }

        if (localDateTime == null) {
            try {
                localDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (Exception ignore) {
            }
        }

        if (localDateTime == null) {
            throw new RuntimeException("Failed to parse date: " + dateTime);
        }

        return localDateTime;
    }


    private void checkDocumentUserTag(int document_id) {

        System.out.println("Check document " + document_id + " have user tag");

        String sql = "SELECT tag_id FROM documents_document_tags WHERE document_id = ?";

        try (var preparedStatement = connection.prepareStatement(sql)) {

            int superUserId = getFirstSuperUser();

            preparedStatement.setInt(1, document_id);
            try (var resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int tag_id = resultSet.getInt("tag_id");

                    // check if tag is user tag
                    if (UserTagIdMap.containsKey(tag_id)) {
                        String taggedUserName = UserTagIdMap.get(tag_id);
                        System.out.println("Document " + document_id + " has user tag " + tag_id + " with username: " + taggedUserName);
                        UserIdMap.keySet().stream().filter(userId -> taggedUserName.equals(UserIdMap.get(userId))).forEach(userId -> {
                            // set this userId as Dokument Owner
                            setDokumentOwner(document_id, superUserId, userId);
                        });


                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to process documents", e);
        }
    }

    private void setDokumentOwner(int dokument_id, int superUserId, int userID) {
        // set super user as owner
        String sql = "UPDATE documents_document SET owner_id = ? WHERE id = ?";
        try (var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, superUserId);
            preparedStatement.setInt(2, dokument_id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update document owner", e);
        }

        // set permission to user

        //check if the permission 'change' entry already exists
        sql = "SELECT id FROM guardian_userobjectpermission WHERE object_pk = '" + Integer.toString(dokument_id) + "' AND content_type_id = 14 AND permission_id = 54 ";
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            int entryId = -1;
            while (resultSet.next()) {
                entryId = resultSet.getInt("id");
            }

            if (entryId != -1) {
                System.out.println("permission entry already exists for document " + dokument_id + " and user " + userID);
                // do update
                sql = "UPDATE guardian_userobjectpermission SET user_id = ? WHERE id = ?";
                try (var preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, userID);
                    preparedStatement.setInt(2, entryId);
                    preparedStatement.executeUpdate();
                }
            } else {
                sql = "INSERT INTO guardian_userobjectpermission (object_pk" +  //dokument_id
                        ", content_type_id" +                                   // contentype 'Dokument' (14)
                        ", permission_id" +                                     // permission 'Can change document' (54)
                        ", user_id) VALUES (?, 14, 54, ?)";

                try (var preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setString(1, Integer.toString(dokument_id));
                    preparedStatement.setInt(2, userID);
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update document permission 'Can change document'", e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user map", e);
        }


        //check if the permission 'view' entry already exists
        sql = "SELECT id FROM guardian_userobjectpermission WHERE object_pk = '" + Integer.toString(dokument_id) + "' AND content_type_id = 14 AND permission_id = 56 ";
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            int entryId = -1;
            while (resultSet.next()) {
                entryId = resultSet.getInt("id");
            }
            if (entryId != -1) {
                System.out.println("permission entry already exists for document " + dokument_id + " and user " + userID);
                // do update
                sql = "UPDATE guardian_userobjectpermission SET user_id = ? WHERE id = ?";
                try (var preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, userID);
                    preparedStatement.setInt(2, entryId);
                    preparedStatement.executeUpdate();
                }
            } else {
                sql = "INSERT INTO guardian_userobjectpermission (object_pk" +  //dokument_id
                        ", content_type_id" +                                   // contentype 'Dokument' (14)
                        ", permission_id" +                                     // permission 'Can view document' (54)
                        ", user_id) VALUES (?, 14, 56, ?)";

                try (var preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setString(1, Integer.toString(dokument_id));
                    preparedStatement.setInt(2, userID);
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to update document permission 'Can view document'", e);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user map", e);
        }

        System.out.println("Document " + dokument_id + " set owner to " + superUserId);
    }

    private int getFirstSuperUser() {
        String sql = "SELECT id FROM auth_user WHERE is_superuser = true";
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                System.out.println("Return first super user: " + id + "");
                System.out.println();
                return id;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user map", e);
        }
        System.err.println("No super user found");
        System.err.println();
        return -1;
    }

    private void moveFile(int id, String archiveFilename, String generatedArchiveFileName) {

        File file = new File(ENV.PAPERLESS_ARCHIVE_PATH + "/" + archiveFilename);
        File newFile = new File(ENV.PAPERLESS_ARCHIVE_PATH + "/" + generatedArchiveFileName);

        if (!file.exists()) {
            throw new RuntimeException("Source file does not exist: " + file.getAbsolutePath());
        }

        newFile.getParentFile().mkdirs();

        if (!file.renameTo(newFile)) {
            throw new RuntimeException("Failed to move file from " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath());
        }

        String sql = "UPDATE documents_document SET archive_filename = ? WHERE id = ?";
        try (var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, generatedArchiveFileName);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update archive_filename in database", e);
        }

//    write audit log
//
//                                                alter name															neuer name
//        {"archive_filename": ["2000/2000_10_21_Nachtrag für die Erneiterte Haushaltversicherung 2000.pdf", "Longri/Testkorospondent/Prospekt/2000/2000_10_21_Nachtrag für die Erneiterte Haushaltversicherung 2000.pdf"]}
//                                                                                                                                              id  id   1=update     14=documente
//        INSERT INTO auditlog_logentry (object_pk , object_id,object_repr, action, changes, timestamp, content_type_id, changes_text) values {"51",51,?,1 json, now, 14, "Path geaendert von JAVA ROBO" }


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

    private String generateArchiveFileName(String title, LocalDateTime created, String correspondentName, String documentTypeName, String userName, String fileExtension) {

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

        return fileName + "." + fileExtension;
    }
}
