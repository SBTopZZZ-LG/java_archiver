package Utilities;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing resources that need to be closed
 * Implements try-with-resources pattern for classes that don't implement AutoCloseable
 */
public class ResourceManager implements AutoCloseable {
    private final List<Closeable> resources = new ArrayList<>();
    private final List<CipherKit> cipherKits = new ArrayList<>();
    
    /**
     * Adds a resource to be managed
     * @param resource Resource to manage
     * @param <T> Type of resource
     * @return The same resource for chaining
     */
    public <T extends Closeable> T manage(T resource) {
        if (resource != null) {
            resources.add(resource);
        }
        return resource;
    }
    
    /**
     * Adds a CipherKit to be managed (for clearing sensitive data)
     * @param kit CipherKit to manage
     * @return The same CipherKit for chaining
     */
    public CipherKit manage(CipherKit kit) {
        if (kit != null) {
            cipherKits.add(kit);
        }
        return kit;
    }
    
    /**
     * Closes all managed resources and clears sensitive data
     */
    @Override
    public void close() {
        // Clear sensitive data from cipher kits first
        for (CipherKit kit : cipherKits) {
            try {
                kit.clearSensitiveData();
            } catch (Exception e) {
                // Log but don't fail on cleanup errors
                System.err.println("Warning: Failed to clear cipher kit data: " + e.getMessage());
            }
        }
        
        // Close all managed resources
        for (Closeable resource : resources) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log but don't fail on cleanup errors
                System.err.println("Warning: Failed to close resource: " + e.getMessage());
            }
        }
        
        resources.clear();
        cipherKits.clear();
    }
}