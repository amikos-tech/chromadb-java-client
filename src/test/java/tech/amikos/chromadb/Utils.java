package tech.amikos.chromadb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Utils {
    public static void loadEnvFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println(".env file does not exist. Skipping loading.");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String key = line.substring(0, equalIndex).trim();
                    String value = line.substring(equalIndex + 1).trim();
                    System.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load .env file: " + e.getMessage());
        }
    }

    public static String getEnvOrProperty(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return value;
    }
}
