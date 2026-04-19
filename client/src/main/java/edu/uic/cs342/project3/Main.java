package edu.uic.cs342.project3;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uic.cs342.project3.model.Color;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            String jsonString = Main.OBJECT_MAPPER.writeValueAsString(Color.RED);
            System.out.println(jsonString);

            Color color = Main.OBJECT_MAPPER.readValue(jsonString, Color.class);
            System.out.println(color);
        } catch (Exception exception) {
            Main.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
            System.exit(1);
        }
    }
}
