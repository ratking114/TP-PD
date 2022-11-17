package shared;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static String hashString (String string_to_hash){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed_string = digest.digest(string_to_hash.getBytes(StandardCharsets.UTF_8));
            return new String(hashed_string);
        }catch (NoSuchAlgorithmException ignored){return null;}

    }
}
