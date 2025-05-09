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


        long intervall = Long.parseLong(ENV.INTERVAL_IN_SEC) * 1000;
        DAO dao = null;
        try {
            dao = new DAO();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while (true) {

            // must reload maps in case of changes like new Correspondent
            dao.loadMaps();

            ENV.setLastRun(dao.moveChangedFiles(ENV.PAPERLESS_LAST_MODIFIED));
            try {
                Thread.sleep(intervall);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}