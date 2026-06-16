package example;

import java.nio.file.*;

public class DumpBytes {
    public static void main(String[] args) throws Exception {
        Path cls = Paths.get("target/debugsw/p/Sw.class");
        byte[] data = Files.readAllBytes(cls);
        String hex = "cafebabe";
        int pos = 0;
        for (byte b : data) {
            if (pos > 0 && pos % 16 == 0) System.out.println();
            System.out.printf("%02x ", b & 0xFF);
            pos++;
        }
        System.out.println();
    }
}
