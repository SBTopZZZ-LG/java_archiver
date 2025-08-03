import Models.Binary;
import Models.SerializableObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Models classes
 */
class ModelsTest {

    @Test
    @DisplayName("Binary.getFormattedSize should handle various sizes correctly")
    void testBinaryGetFormattedSize() {
        assertThat(Binary.getFormattedSize(0)).isEqualTo("0 bytes");
        assertThat(Binary.getFormattedSize(512)).isEqualTo("512 bytes");
        assertThat(Binary.getFormattedSize(1024)).isEqualTo("1 KB");
        assertThat(Binary.getFormattedSize(1536)).isEqualTo("1 KB"); // 1.5 KB rounds down
        assertThat(Binary.getFormattedSize(1048576)).isEqualTo("1 MB");
        assertThat(Binary.getFormattedSize(1073741824)).isEqualTo("1.0 GB");
        assertThat(Binary.getFormattedSize(1099511627776L)).isEqualTo("1.0 TB");
    }

    @Test
    @DisplayName("Binary.getFormattedSize should handle edge cases")
    void testBinaryGetFormattedSizeEdgeCases() {
        assertThat(Binary.getFormattedSize(Long.MAX_VALUE)).contains("TB");
        assertThat(Binary.getFormattedSize(1023)).isEqualTo("1023 bytes");
        assertThat(Binary.getFormattedSize(1025)).isEqualTo("1 KB");
    }

    @Test
    @DisplayName("Binary should handle SizeType enumeration")
    void testBinarySizeType() {
        // Test that enum values exist and can be compared
        assertThat(Binary.SizeType.SHORT).isNotNull();
        assertThat(Binary.SizeType.INTEGER).isNotNull();
        assertThat(Binary.SizeType.LONG).isNotNull();
        
        // Test enum comparison
        assertThat(Binary.SizeType.SHORT).isNotEqualTo(Binary.SizeType.INTEGER);
        assertThat(Binary.SizeType.INTEGER).isNotEqualTo(Binary.SizeType.LONG);
    }

    /**
     * Test implementation of Binary for testing purposes
     */
    private static class TestBinary extends Binary {
        private String data;
        
        public TestBinary(String data) {
            this.data = data;
        }
        
        @Override
        public int getSize() {
            return data.length();
        }
        
        @Override
        public byte[] toByteArray() {
            return data.getBytes();
        }
        
        @Override
        public void fromByteArray(byte[] bytes) {
            this.data = new String(bytes);
        }
        
        public String getData() {
            return data;
        }
    }

    @Test
    @DisplayName("Binary sizeToByteArray should work with different size types")
    void testBinarySizeToByteArray() {
        TestBinary binary = new TestBinary("test");
        
        byte[] shortBytes = binary.sizeToByteArray(Binary.SizeType.SHORT);
        assertThat(shortBytes).hasSize(2); // short is 2 bytes
        
        byte[] intBytes = binary.sizeToByteArray(Binary.SizeType.INTEGER);
        assertThat(intBytes).hasSize(4); // integer is 4 bytes
        
        byte[] longBytes = binary.sizeToByteArray(Binary.SizeType.LONG);
        assertThat(longBytes).hasSize(8); // long is 8 bytes
    }

    @Test
    @DisplayName("Binary should handle null fromByteArray input gracefully")
    void testBinaryFromByteArrayNull() {
        TestBinary binary = new TestBinary("initial");
        
        // This will throw an exception due to null bytes in String constructor
        assertThatThrownBy(() -> binary.fromByteArray(null))
            .isInstanceOf(NullPointerException.class);
    }

    /**
     * Test implementation of SerializableObject for testing purposes
     */
    private static class TestSerializableObject extends SerializableObject {
        private String content;
        
        public TestSerializableObject(String content) {
            this.content = content;
        }
        
        @Override
        public int getSize() {
            return content.length();
        }
        
        @Override
        public byte[] toByteArray() {
            return content.getBytes();
        }
        
        @Override
        public void fromByteArray(byte[] bytes) {
            this.content = new String(bytes);
        }
        
        public String getContent() {
            return content;
        }
    }

    @Test
    @DisplayName("SerializableObject should provide basic serialization functionality")
    void testSerializableObjectBasicFunctionality() {
        TestSerializableObject obj = new TestSerializableObject("test content");
        
        assertThat(obj.getSize()).isEqualTo(12); // "test content".length()
        
        byte[] serialized = obj.toByteArray();
        assertThat(serialized).isEqualTo("test content".getBytes());
        
        TestSerializableObject obj2 = new TestSerializableObject("");
        obj2.fromByteArray(serialized);
        assertThat(obj2.getContent()).isEqualTo("test content");
    }

    @Test
    @DisplayName("SerializableObject should handle empty content")
    void testSerializableObjectEmptyContent() {
        TestSerializableObject obj = new TestSerializableObject("");
        
        assertThat(obj.getSize()).isEqualTo(0);
        assertThat(obj.toByteArray()).isEmpty();
        
        TestSerializableObject obj2 = new TestSerializableObject("initial");
        obj2.fromByteArray(new byte[0]);
        assertThat(obj2.getContent()).isEmpty();
    }

    @Test
    @DisplayName("SerializableObject should handle round-trip serialization")
    void testSerializableObjectRoundTrip() {
        String originalContent = "Original test content with special chars: !@#$%^&*()";
        TestSerializableObject obj1 = new TestSerializableObject(originalContent);
        
        byte[] serialized = obj1.toByteArray();
        
        TestSerializableObject obj2 = new TestSerializableObject("");
        obj2.fromByteArray(serialized);
        
        assertThat(obj2.getContent()).isEqualTo(originalContent);
        assertThat(obj2.getSize()).isEqualTo(obj1.getSize());
    }
}