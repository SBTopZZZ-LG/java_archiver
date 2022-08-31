package Utilities;

import java.io.*;
import java.nio.ByteBuffer;

public class BufferedStream {
    public enum JavaStreamSegmentType {
        INTEGER,
        LONG
    }

    public interface JavaStreamReadSegmentCallback {
        void onSegmentRetrieve(final byte[] bytes, final JavaStreamSegmentType segmentType);
    }
    public interface JavaStreamWrite {
        void writeSegment(byte[] bytes, JavaStreamSegmentType segmentType);
    }
    public interface JavaStreamRead {
        byte[] readSegment(final JavaStreamSegmentType segmentType);
        void readSegment(final JavaStreamSegmentType segmentType, final JavaStreamReadSegmentCallback callback);
    }

    public interface ParsableInput {
        int getInt();
        long getLong();
        boolean getBoolean();
    }
    public static class Input extends BufferedInputStream implements JavaStreamRead, ParsableInput {
        private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
        private final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

        public Input(InputStream in) {
            super(in);
        }

        @Override
        public byte[] readSegment(JavaStreamSegmentType segmentType) {
            if (segmentType == JavaStreamSegmentType.INTEGER)
                try {
                    int segmentSize = getInt();

                    if (segmentSize < 0)
                        throw new Exception("Segment size was negative");

                    return readNBytes(segmentSize);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            else if (segmentType == JavaStreamSegmentType.LONG)
                try {
                    int segmentSize = (int) getLong();

                    if (segmentSize < 0)
                        throw new Exception("Segment size was negative");

                    return readNBytes(segmentSize);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            return null;
        }

        @Override
        public void readSegment(JavaStreamSegmentType segmentType, JavaStreamReadSegmentCallback callback) {
            if (segmentType == JavaStreamSegmentType.INTEGER) {
                try {
                    int segmentSize = getInt();

                    if (segmentSize < 0)
                        throw new Exception("Segment size was negative");

                    callback.onSegmentRetrieve(readNBytes(segmentSize), segmentType);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (segmentType == JavaStreamSegmentType.LONG) {
                try {
                    int segmentSize = (int) getLong();

                    if (segmentSize < 0)
                        throw new Exception("Segment size was negative");

                    callback.onSegmentRetrieve(readNBytes(segmentSize), segmentType);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public int getInt() {
            try {
                return intBuffer.position(0).put(readNBytes(4)).flip().getInt();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public long getLong() {
            try {
                return longBuffer.position(0).put(readNBytes(8)).flip().getLong();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean getBoolean() {
            try {
                return readNBytes(1)[0] == 1;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public interface ParsableOutput {
        void putInt(int value);
        void putLong(long value);
        void putBoolean(boolean value);
    }
    public static class Output extends BufferedOutputStream implements JavaStreamWrite, ParsableOutput {
        private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
        private final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

        public Output(OutputStream out) {
            super(out);
        }

        @Override
        public void writeSegment(byte[] bytes, JavaStreamSegmentType segmentType) {
            try {
                if (segmentType == JavaStreamSegmentType.INTEGER)
                    putInt(bytes.length);
                else if (segmentType == JavaStreamSegmentType.LONG)
                    putLong(bytes.length);
                else
                    throw new Exception("Unknown segment type specified");

                write(bytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void putInt(int value) {
            try {
                write(intBuffer.position(0).putInt(value).array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void putLong(long value) {
            try {
                write(longBuffer.position(0).putLong(value).array());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void putBoolean(boolean value) {
            try {
                write(new byte[]{(byte)(value ? 1 : 0)});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
