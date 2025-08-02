package Utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Utility class for data integrity verification and compression
 */
public class DataIntegrity {
    
    /**
     * Calculates SHA-256 hash of data
     * @param data Data to hash
     * @return SHA-256 hash as hex string
     */
    public static String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Calculates CRC32 checksum of data
     * @param data Data to checksum
     * @return CRC32 checksum
     */
    public static long calculateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
    
    /**
     * Compresses data using DEFLATE algorithm
     * @param data Data to compress
     * @return Compressed data
     * @throws IOException If compression fails
     */
    public static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        
        outputStream.close();
        deflater.end();
        
        return outputStream.toByteArray();
    }
    
    /**
     * Decompresses data using INFLATE algorithm
     * @param compressedData Compressed data
     * @param originalLength Original length of data (for buffer allocation)
     * @return Decompressed data
     * @throws IOException If decompression fails
     */
    public static byte[] decompress(byte[] compressedData, int originalLength) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(originalLength);
        byte[] buffer = new byte[1024];
        
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
        } catch (Exception e) {
            throw new IOException("Failed to decompress data", e);
        } finally {
            outputStream.close();
            inflater.end();
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Checks if compression would be beneficial for the given data
     * @param data Data to check
     * @return true if compression would likely reduce size
     */
    public static boolean shouldCompress(byte[] data) {
        // Don't compress small files (overhead not worth it)
        if (data.length < 100) {
            return false;
        }
        
        // Quick entropy check - if first 100 bytes are very repetitive, likely compressible
        if (data.length >= 100) {
            byte first = data[0];
            int sameCount = 1;
            for (int i = 1; i < Math.min(100, data.length); i++) {
                if (data[i] == first) {
                    sameCount++;
                }
            }
            
            // If more than 50% of first 100 bytes are the same, likely compressible
            return sameCount > 50;
        }
        
        return true; // Default to attempting compression
    }
    
    /**
     * Archive integrity metadata
     */
    public static class IntegrityMetadata {
        public final String sha256Hash;
        public final long crc32Checksum;
        public final boolean isCompressed;
        public final int originalSize;
        public final int compressedSize;
        
        public IntegrityMetadata(String sha256Hash, long crc32Checksum, boolean isCompressed, 
                               int originalSize, int compressedSize) {
            this.sha256Hash = sha256Hash;
            this.crc32Checksum = crc32Checksum;
            this.isCompressed = isCompressed;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
        }
        
        /**
         * Verifies data against this metadata
         * @param data Data to verify
         * @return true if data matches metadata
         */
        public boolean verify(byte[] data) {
            if (data.length != (isCompressed ? compressedSize : originalSize)) {
                return false;
            }
            
            String actualSHA256 = calculateSHA256(data);
            long actualCRC32 = calculateCRC32(data);
            
            return sha256Hash.equals(actualSHA256) && crc32Checksum == actualCRC32;
        }
        
        /**
         * Serializes metadata to byte array
         * @return Serialized metadata
         */
        public byte[] toByteArray() {
            ByteArrayBuilder builder = ByteArrayBuilder.build();
            
            // SHA-256 hash (32 bytes)
            byte[] hashBytes = new byte[32];
            for (int i = 0; i < 32; i++) {
                hashBytes[i] = (byte) Integer.parseInt(sha256Hash.substring(i * 2, i * 2 + 2), 16);
            }
            builder.appendBytes(hashBytes);
            
            // CRC32 (4 bytes)
            builder.appendBytes(ByteArrayBuilder.intToBytes((int) crc32Checksum));
            
            // Compression flag (1 byte)
            builder.appendByte((byte) (isCompressed ? 1 : 0));
            
            // Original size (4 bytes)
            builder.appendBytes(ByteArrayBuilder.intToBytes(originalSize));
            
            // Compressed size (4 bytes)
            builder.appendBytes(ByteArrayBuilder.intToBytes(compressedSize));
            
            return builder.toByteArray();
        }
        
        /**
         * Deserializes metadata from byte array
         * @param data Serialized metadata
         * @return IntegrityMetadata instance
         */
        public static IntegrityMetadata fromByteArray(byte[] data) {
            if (data.length < 45) { // 32 + 4 + 1 + 4 + 4
                throw new IllegalArgumentException("Invalid metadata size");
            }
            
            // SHA-256 hash
            StringBuilder hashBuilder = new StringBuilder();
            for (int i = 0; i < 32; i++) {
                hashBuilder.append(String.format("%02x", data[i] & 0xff));
            }
            String sha256Hash = hashBuilder.toString();
            
            // CRC32
            long crc32 = ByteArrayBuilder.bytesToInt(data, 32) & 0xffffffffL;
            
            // Compression flag
            boolean isCompressed = data[36] == 1;
            
            // Original size
            int originalSize = ByteArrayBuilder.bytesToInt(data, 37);
            
            // Compressed size
            int compressedSize = ByteArrayBuilder.bytesToInt(data, 41);
            
            return new IntegrityMetadata(sha256Hash, crc32, isCompressed, originalSize, compressedSize);
        }
    }
}