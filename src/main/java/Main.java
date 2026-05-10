import java.lang.System;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Hello world");
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        } 
    }
}

