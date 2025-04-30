package de.longri.paperless_renamer;

import org.ini4j.Ini;

import java.io.File;
import java.io.IOException;

public class ENV {

    public static String PAPERLESS_ARCHIVE_PATH;

    public static String POSTGRES_DB;
    public static String POSTGRES_USER;
    public static String POSTGRES_PASSWORD;
    public static String POSTGRES_PORT;
    public static String PAPERLESS_FILENAME_FORMAT;
    public static String PAPERLESS_STORAGE_PATH_FORMAT;

    public static String PAPERLESS_LAST_MODIFIED;

    private static Ini ini;

    public static void load(String path) throws IOException {
        ini = new Ini(new File(path));
        PAPERLESS_ARCHIVE_PATH = ini.get("PAPERLESS", "PAPERLESS_ARCHIVE_PATH");
        POSTGRES_DB = ini.get("PAPERLESS", "POSTGRES_DB");
        POSTGRES_USER = ini.get("PAPERLESS", "POSTGRES_USER");
        POSTGRES_PASSWORD = ini.get("PAPERLESS", "POSTGRES_PASSWORD");
        POSTGRES_PORT = ini.get("PAPERLESS", "POSTGRES_PORT");

        PAPERLESS_LAST_MODIFIED = ini.get("SERVICE", "PAPERLESS_LAST_MODIFIED");
        PAPERLESS_FILENAME_FORMAT = ini.get("PAPERLESS", "PAPERLESS_FILENAME_FORMAT");
        PAPERLESS_STORAGE_PATH_FORMAT = ini.get("PAPERLESS", "PAPERLESS_STORAGE_PATH_FORMAT");
    }

    public static void setLastRun(String lastRun) {
        ini.put("SERVICE", "PAPERLESS_LAST_MODIFIED", lastRun);
        try {
            ini.store();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("LastRun set to " + lastRun);
    }


    public static String tostring() {
        if (ini != null) {
            return "ini is loaded:\n" +
                    "    PAPERLESS_ARCHIVE_PATH=" + PAPERLESS_ARCHIVE_PATH + "\n" +
                    "    POSTGRES_DB=" + POSTGRES_DB + "\n" +
                    "    POSTGRES_USER=" + POSTGRES_USER + "\n" +
                    "    POSTGRES_PASSWORD=" + POSTGRES_PASSWORD + "\n" +
                    "    POSTGRES_PORT=" + POSTGRES_PORT + "\n" +
                    "    PAPERLESS_LAST_MODIFIED=" + PAPERLESS_LAST_MODIFIED + "\n" +
                    "    PAPERLESS_FILENAME_FORMAT=" + PAPERLESS_FILENAME_FORMAT + "\n" +
                    "    PAPERLESS_STORAGE_PATH_FORMAT=" + PAPERLESS_STORAGE_PATH_FORMAT + "\n";
        } else {
            return "ini is null";
        }
    }
}
