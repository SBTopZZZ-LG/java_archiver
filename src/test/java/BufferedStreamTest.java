import Utilities.BufferedStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for BufferedStream input/output functionality
 */
class BufferedStreamTest {

    @TempDir
    Path tempDir;
    
    private File testFile;
    private byte[] testData;

    @BeforeEach
    void setUp() throws IOException {
        testFile = tempDir.resolve("test_stream.dat").toFile();
        testData = "Hello, BufferedStream Test!".getBytes();
    }

    @Test
    @DisplayName("BufferedStream Output should write data correctly")
    void testBufferedStreamOutputBasic() throws IOException {
        try (BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile))) {
            output.write(testData);
            output.flush();
        }
        
        assertThat(testFile).exists();
        assertThat(testFile.length()).isEqualTo(testData.length);
        
        byte[] readData = new byte[testData.length];
        try (FileInputStream fis = new FileInputStream(testFile)) {
            int bytesRead = fis.read(readData);
            assertThat(bytesRead).isEqualTo(testData.length);
            assertThat(readData).isEqualTo(testData);
        }
    }

    @Test
    @DisplayName("BufferedStream Input should read data correctly")
    void testBufferedStreamInputBasic() throws IOException {
        // First write test data
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(testData);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            assertThat(input.available()).isEqualTo(testData.length);
            
            byte[] readData = input.readNBytes(testData.length);
            assertThat(readData).isEqualTo(testData);
            
            assertThat(input.available()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("BufferedStream should handle boolean operations")
    void testBufferedStreamBooleanOperations() throws IOException {
        try (BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile))) {
            output.putBoolean(true);
            output.putBoolean(false);
            output.putBoolean(true);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            assertThat(input.getBoolean()).isTrue();
            assertThat(input.getBoolean()).isFalse();
            assertThat(input.getBoolean()).isTrue();
        }
    }

    @Test
    @DisplayName("BufferedStream should handle segment operations")
    void testBufferedStreamSegmentOperations() throws IOException {
        byte[] segmentData = "Segment test data".getBytes();
        
        try (BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile))) {
            output.writeSegment(segmentData, BufferedStream.JavaStreamSegmentType.LONG);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            byte[] readSegment = input.readSegment(BufferedStream.JavaStreamSegmentType.LONG);
            assertThat(readSegment).isEqualTo(segmentData);
        }
    }

    @Test
    @DisplayName("BufferedStream should handle segment operations with callback")
    void testBufferedStreamSegmentOperationsWithCallback() throws IOException {
        byte[] segmentData = "Callback segment test".getBytes();
        
        try (BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile))) {
            output.writeSegment(segmentData, BufferedStream.JavaStreamSegmentType.LONG);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            final byte[][] receivedData = new byte[1][];
            
            input.readSegment(BufferedStream.JavaStreamSegmentType.LONG, new BufferedStream.JavaStreamReadSegmentCallback() {
                @Override
                public void onSegmentRetrieve(byte[] bytes, BufferedStream.JavaStreamSegmentType segmentType) {
                    receivedData[0] = bytes;
                    assertThat(segmentType).isEqualTo(BufferedStream.JavaStreamSegmentType.LONG);
                }
            });
            
            assertThat(receivedData[0]).isEqualTo(segmentData);
        }
    }

    @Test
    @DisplayName("BufferedStream should handle readNBytes with offset")
    void testBufferedStreamReadNBytesWithOffset() throws IOException {
        byte[] largeData = new byte[100];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) i;
        }
        
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(largeData);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            byte[] buffer = new byte[50];
            int bytesRead = input.readNBytes(buffer, 10, 30);
            
            assertThat(bytesRead).isEqualTo(30);
            
            // Check that bytes were written to correct offset
            for (int i = 0; i < 10; i++) {
                assertThat(buffer[i]).isEqualTo((byte) 0); // Should remain unchanged
            }
            for (int i = 10; i < 40; i++) {
                assertThat(buffer[i]).isEqualTo((byte) (i - 10)); // Should match source data
            }
            for (int i = 40; i < 50; i++) {
                assertThat(buffer[i]).isEqualTo((byte) 0); // Should remain unchanged
            }
        }
    }

    @Test
    @DisplayName("BufferedStream should handle skipNBytes operation")
    void testBufferedStreamSkipNBytes() throws IOException {
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(data);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            // Read first 10 bytes
            byte[] first10 = input.readNBytes(10);
            assertThat(first10).hasSize(10);
            
            // Skip 20 bytes
            input.skipNBytes(20);
            
            // Read next 10 bytes (should be bytes 30-39)
            byte[] next10 = input.readNBytes(10);
            assertThat(next10).hasSize(10);
            
            for (int i = 0; i < 10; i++) {
                assertThat(next10[i]).isEqualTo((byte) (30 + i));
            }
        }
    }

    @Test
    @DisplayName("BufferedStream should handle getLong operation")
    void testBufferedStreamGetLong() throws IOException {
        long testLong = 0x123456789ABCDEFL;
        
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(testFile))) {
            dos.writeLong(testLong);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            long readLong = input.getLong();
            assertThat(readLong).isEqualTo(testLong);
        }
    }

    @Test
    @DisplayName("BufferedStream should handle multiple boolean operations in sequence")
    void testBufferedStreamMultipleBooleans() throws IOException {
        boolean[] booleans = {true, false, true, true, false, false, true};
        
        try (BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile))) {
            for (boolean b : booleans) {
                output.putBoolean(b);
            }
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            for (boolean expected : booleans) {
                assertThat(input.getBoolean()).isEqualTo(expected);
            }
        }
    }

    @Test
    @DisplayName("BufferedStream should handle multiple segments")
    void testBufferedStreamMultipleSegments() throws IOException {
        byte[] segment1 = "First segment".getBytes();
        byte[] segment2 = "Second segment".getBytes();
        byte[] segment3 = "Third segment".getBytes();
        
        try (BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile))) {
            output.writeSegment(segment1, BufferedStream.JavaStreamSegmentType.LONG);
            output.writeSegment(segment2, BufferedStream.JavaStreamSegmentType.LONG);
            output.writeSegment(segment3, BufferedStream.JavaStreamSegmentType.LONG);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            byte[] read1 = input.readSegment(BufferedStream.JavaStreamSegmentType.LONG);
            byte[] read2 = input.readSegment(BufferedStream.JavaStreamSegmentType.LONG);
            byte[] read3 = input.readSegment(BufferedStream.JavaStreamSegmentType.LONG);
            
            assertThat(read1).isEqualTo(segment1);
            assertThat(read2).isEqualTo(segment2);
            assertThat(read3).isEqualTo(segment3);
        }
    }

    @Test
    @DisplayName("BufferedStream should handle empty segments")
    void testBufferedStreamEmptySegments() throws IOException {
        byte[] emptySegment = new byte[0];
        
        try (BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile))) {
            output.writeSegment(emptySegment, BufferedStream.JavaStreamSegmentType.LONG);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            byte[] readSegment = input.readSegment(BufferedStream.JavaStreamSegmentType.LONG);
            assertThat(readSegment).isEqualTo(emptySegment);
            assertThat(readSegment).hasSize(0);
        }
    }

    @Test
    @DisplayName("BufferedStream should handle large segments")
    void testBufferedStreamLargeSegments() throws IOException {
        byte[] largeSegment = new byte[50000]; // 50KB
        for (int i = 0; i < largeSegment.length; i++) {
            largeSegment[i] = (byte) (i % 256);
        }
        
        try (BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile))) {
            output.writeSegment(largeSegment, BufferedStream.JavaStreamSegmentType.LONG);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            byte[] readSegment = input.readSegment(BufferedStream.JavaStreamSegmentType.LONG);
            assertThat(readSegment).isEqualTo(largeSegment);
        }
    }

    @Test
    @DisplayName("BufferedStream should handle reading beyond available data gracefully")
    void testBufferedStreamReadBeyondAvailable() throws IOException {
        byte[] smallData = "small".getBytes();
        
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(smallData);
        }
        
        try (BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile))) {
            assertThat(input.available()).isEqualTo(smallData.length);
            
            // Try to read more than available
            byte[] buffer = new byte[100];
            int bytesRead = input.readNBytes(buffer, 0, 100);
            
            assertThat(bytesRead).isEqualTo(smallData.length);
            for (int i = 0; i < smallData.length; i++) {
                assertThat(buffer[i]).isEqualTo(smallData[i]);
            }
        }
    }

    @Test
    @DisplayName("BufferedStream should handle IOException properly")
    void testBufferedStreamIOException() throws IOException {
        // Create a file and then create input stream
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(testData);
        }
        
        // Create input stream from existing file
        BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile));
        
        // Read some data normally first
        byte[] normalRead = input.readNBytes(5);
        assertThat(normalRead).hasSize(5);
        
        // Close the underlying stream to force an error
        input.close();
        
        // Now trying to read should fail
        assertThatThrownBy(() -> input.readNBytes(100))
            .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("BufferedStream should handle close operations properly")
    void testBufferedStreamCloseOperations() throws IOException {
        BufferedStream.Output output = new BufferedStream.Output(new FileOutputStream(testFile));
        output.write(testData);
        output.close();
        
        // Should not throw exception when closing again
        assertThatCode(() -> output.close()).doesNotThrowAnyException();
        
        BufferedStream.Input input = new BufferedStream.Input(new FileInputStream(testFile));
        input.readNBytes(testData.length);
        input.close();
        
        // Should not throw exception when closing again
        assertThatCode(() -> input.close()).doesNotThrowAnyException();
    }
}