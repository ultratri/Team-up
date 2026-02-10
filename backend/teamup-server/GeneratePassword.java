import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash for "123456"
        String password = "123456";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        
        // Verify the hash
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification: " + matches);
        
        // Also test the existing hash
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye/JDMXqmNYFd5NQv6hBpxaPWxMf.ILiq";
        boolean existingMatches = encoder.matches(password, existingHash);
        System.out.println("\nTesting existing hash:");
        System.out.println("Existing Hash: " + existingHash);
        System.out.println("Matches '123456': " + existingMatches);
    }
}
