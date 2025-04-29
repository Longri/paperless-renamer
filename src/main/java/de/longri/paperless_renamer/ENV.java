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

    private static Ini ini;

    public static void load(String path) throws IOException {
        ini = new Ini(new File(path));
        PAPERLESS_ARCHIVE_PATH = ini.get("PAPERLESS", "PAPERLESS_ARCHIVE_PATH");
        POSTGRES_DB = ini.get("PAPERLESS", "POSTGRES_DB");
        POSTGRES_USER = ini.get("PAPERLESS", "POSTGRES_USER");
        POSTGRES_PASSWORD = ini.get("PAPERLESS", "POSTGRES_PASSWORD");
        POSTGRES_PORT = ini.get("PAPERLESS", "POSTGRES_PORT");
    }


    public static String tostring() {
        if (ini != null) {
            return "ini is loaded:\n" +
                    "    PAPERLESS_ARCHIVE_PATH=" + PAPERLESS_ARCHIVE_PATH + "\n" +
                    "    POSTGRES_DB=" + POSTGRES_DB + "\n" +
                    "    POSTGRES_USER=" + POSTGRES_USER + "\n" +
                    "    POSTGRES_PASSWORD=" + POSTGRES_PASSWORD + "\n" +
                    "    POSTGRES_PORT=" + POSTGRES_PORT + "\n";
        } else {
            return "ini is null";
        }
    }
}
