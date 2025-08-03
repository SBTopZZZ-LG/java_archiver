import Utilities.Binaries.BinaryString;
import Utilities.Binaries.BinaryLong;
import Utilities.Binaries.BinaryBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for Binary utility classes
 */
class BinaryUtilitiesTest {

    @Test
    @DisplayName("BinaryString should handle normal string operations")
    void testBinaryStringNormalOperations() {
        BinaryString binaryString = new BinaryString("test");
        
        assertThat(binaryString.data).isEqualTo("test");
        assertThat(binaryString.getSize()).isEqualTo(4);
        
        byte[] byteArray = binaryString.toByteArray();
        assertThat(byteArray).isEqualTo("test".getBytes(StandardCharsets.UTF_8));
        
        BinaryString deserializedString = new BinaryString();
        deserializedString.fromByteArray(byteArray);
        assertThat(deserializedString.data).isEqualTo("test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "test", "multiword string", "with\nnewlines", "special chars: !@#$%^&*()"})
    @DisplayName("BinaryString should handle various string inputs")
    void testBinaryStringVariousInputs(String input) {
        BinaryString binaryString = new BinaryString(input);
        
        assertThat(binaryString.data).isEqualTo(input);
        assertThat(binaryString.getSize()).isEqualTo(input.length());
        
        byte[] byteArray = binaryString.toByteArray();
        BinaryString deserializedString = new BinaryString();
        deserializedString.fromByteArray(byteArray);
        assertThat(deserializedString.data).isEqualTo(input);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("BinaryString should handle null input")
    void testBinaryStringNullInput(String input) {
        assertThatThrownBy(() -> new BinaryString(input))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("BinaryString should handle Unicode characters")
    void testBinaryStringUnicode() {
        String unicodeString = "Hello 世界 🌍 🚀";
        BinaryString binaryString = new BinaryString(unicodeString);
        
        assertThat(binaryString.data).isEqualTo(unicodeString);
        
        byte[] byteArray = binaryString.toByteArray();
        BinaryString deserializedString = new BinaryString();
        deserializedString.fromByteArray(byteArray);
        assertThat(deserializedString.data).isEqualTo(unicodeString);
    }

    @Test
    @DisplayName("BinaryLong should handle normal long operations")
    void testBinaryLongNormalOperations() {
        BinaryLong binaryLong = new BinaryLong(42L);
        
        assertThat(binaryLong.data).isEqualTo(42L);
        assertThat(binaryLong.getSize()).isEqualTo(8); // long is 8 bytes
        
        byte[] byteArray = binaryLong.toByteArray();
        assertThat(byteArray).hasSize(8);
        
        BinaryLong deserializedLong = new BinaryLong();
        deserializedLong.fromByteArray(byteArray);
        assertThat(deserializedLong.data).isEqualTo(42L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, -1L, 42L, -42L, Long.MAX_VALUE, Long.MIN_VALUE})
    @DisplayName("BinaryLong should handle various long values")
    void testBinaryLongVariousValues(long value) {
        BinaryLong binaryLong = new BinaryLong(value);
        
        assertThat(binaryLong.data).isEqualTo(value);
        
        byte[] byteArray = binaryLong.toByteArray();
        BinaryLong deserializedLong = new BinaryLong();
        deserializedLong.fromByteArray(byteArray);
        assertThat(deserializedLong.data).isEqualTo(value);
    }

    @Test
    @DisplayName("BinaryBoolean should handle normal boolean operations")
    void testBinaryBooleanNormalOperations() {
        BinaryBoolean binaryTrue = new BinaryBoolean(true);
        BinaryBoolean binaryFalse = new BinaryBoolean(false);
        
        assertThat(binaryTrue.data).isTrue();
        assertThat(binaryFalse.data).isFalse();
        assertThat(binaryTrue.getSize()).isEqualTo(1);
        assertThat(binaryFalse.getSize()).isEqualTo(1);
        
        byte[] trueBytes = binaryTrue.toByteArray();
        byte[] falseBytes = binaryFalse.toByteArray();
        
        assertThat(trueBytes).hasSize(1);
        assertThat(falseBytes).hasSize(1);
        assertThat(trueBytes[0]).isEqualTo((byte) 1);
        assertThat(falseBytes[0]).isEqualTo((byte) 0);
        
        BinaryBoolean deserializedTrue = new BinaryBoolean();
        BinaryBoolean deserializedFalse = new BinaryBoolean();
        
        deserializedTrue.fromByteArray(trueBytes);
        deserializedFalse.fromByteArray(falseBytes);
        
        assertThat(deserializedTrue.data).isTrue();
        assertThat(deserializedFalse.data).isFalse();
    }

    @Test
    @DisplayName("BinaryString should handle edge case of empty byte array")
    void testBinaryStringEmptyByteArray() {
        BinaryString binaryString = new BinaryString();
        binaryString.fromByteArray(new byte[0]);
        
        assertThat(binaryString.data).isEmpty();
        assertThat(binaryString.getSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("BinaryLong should handle invalid byte array size")
    void testBinaryLongInvalidByteArraySize() {
        BinaryLong binaryLong = new BinaryLong();
        
        assertThatThrownBy(() -> binaryLong.fromByteArray(new byte[4])) // Wrong size
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("BinaryBoolean should handle invalid byte array size")
    void testBinaryBooleanInvalidByteArraySize() {
        BinaryBoolean binaryBoolean = new BinaryBoolean();
        
        assertThatThrownBy(() -> binaryBoolean.fromByteArray(new byte[2])) // Wrong size
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Binary objects should handle default constructors")
    void testBinaryDefaultConstructors() {
        BinaryString binaryString = new BinaryString();
        BinaryLong binaryLong = new BinaryLong();
        BinaryBoolean binaryBoolean = new BinaryBoolean();
        
        assertThat(binaryString.data).isNull();
        assertThat(binaryLong.data).isEqualTo(0L);
        assertThat(binaryBoolean.data).isFalse();
    }

    @Test
    @DisplayName("BinaryString should handle very large strings")
    void testBinaryStringLargeSize() {
        StringBuilder largeString = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeString.append("test");
        }
        String input = largeString.toString();
        
        BinaryString binaryString = new BinaryString(input);
        
        assertThat(binaryString.data).isEqualTo(input);
        assertThat(binaryString.getSize()).isEqualTo(input.length());
        
        byte[] byteArray = binaryString.toByteArray();
        BinaryString deserializedString = new BinaryString();
        deserializedString.fromByteArray(byteArray);
        assertThat(deserializedString.data).isEqualTo(input);
    }
}