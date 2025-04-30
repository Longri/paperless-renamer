package de.longri.paperless_renamer;

import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            ENV.load("./.env");
            System.out.println(ENV.tostring());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            DAO dao = new DAO();
            ENV.setLastRun(dao.moveChangedFiles(ENV.PAPERLESS_LAST_MODIFIED));


        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}