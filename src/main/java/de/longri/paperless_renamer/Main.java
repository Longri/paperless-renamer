package de.longri.paperless_renamer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            ENV.load("./.env");
            System.out.println(ENV.tostring());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}