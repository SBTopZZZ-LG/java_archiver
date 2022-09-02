package Utilities;

import java.io.*;
import java.nio.ByteBuffer;

public class BufferedStream {
    public enum JavaStreamSegmentType {
        SHORT,
        INTEGER,
        LONG
    }

    public interface JavaStreamReadSegmentCallback {
        /**
         * Called when the desired segment is fetched
         * @param bytes Fetched byte array (without length bytes)
         * @param segmentType `INTEGER` if four bytes were used to include length, otherwise `LONG`, in which case eight bytes were used
         */
        void onSegmentRetrieve(final byte[] bytes, final JavaStreamSegmentType segmentType);
    }
    public interface JavaStreamWrite {
        /**
         * Writes a segment into the stream
         * <li>Length bytes (either four or eight bytes)</li>
         * <li>`bytes`</li>
         * @param bytes Segment body
         * @param segmentType Four bytes will be used to determine the segment length if segment type is `INTEGER`, and eight bytes if `LONG`
         */
        void writeSegment(byte[] bytes, JavaStreamSegmentType segmentType);
    }
    public interface JavaStreamRead {
        /**
         * Accesses a segment body
         * @param segmentType Segment type to interpret length bytes size
         * @return Segment body
         */
        byte[] readSegment(final JavaStreamSegmentType segmentType);

        /**
         * Accesses a segment body
         * @param segmentType Segment type to interpret length bytes size
         * @param callback Callback
         */
        void readSegment(final JavaStreamSegmentType segmentType, final JavaStreamReadSegmentCallback callback);
    }

    public interface ParsableInput {
        /**
         * Reads and interprets next two bytes as an integer
         * @return Value
         */
        short getShort();

        /**
         * Reads and interprets next four bytes as an integer
         * @return Value
         */
        int getInt();

        /**
         * Reads and interprets next eight bytes as a long
         * @return Value
         */
        long getLong();

        /**
         * Reads and interprets next one byte as a boolean
         * @return Value
         */
        boolean getBoolean();
    }
    public static class Input extends BufferedInputStream implements JavaStreamRead, ParsableInput {
        private final ByteBuffer shortBuffer = ByteBuffer.allocate(Short.BYTES);
        private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
        private final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

        public Input(InputStream in) {
            super(in);
        }

        @Override
        public byte[] readSegment(JavaStreamSegmentType segmentType) {
            if (segmentType == JavaStreamSegmentType.SHORT) {
                try {
                    int segmentSize = getShort();

                    if (segmentSize < 0)
                        throw new Exception("Segment size was negative");

                    return readNBytes(segmentSize);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (segmentType == JavaStreamSegmentType.INTEGER)
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
            if (segmentType == JavaStreamSegmentType.SHORT) {
                try {
                    int segmentSize = getShort();

                    if (segmentSize < 0)
                        throw new Exception("Segment size was negative");

                    callback.onSegmentRetrieve(readNBytes(segmentSize), segmentType);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (segmentType == JavaStreamSegmentType.INTEGER) {
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
        public short getShort() {
            try {
                return shortBuffer.position(0).put(readNBytes(2)).flip().getShort();
            } catch (IOException e) {
                throw new RuntimeException(e);
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
        /**
         * Interprets and writes short `value` in next two bytes
         * @param value Short to interpret
         */
        void putShort(short value);

        /**
         * Interprets and writes integer `value` in next four bytes
         * @param value Integer to interpret
         */
        void putInt(int value);

        /**
         * Interprets and writes long `value` in next eight bytes
         * @param value Long to interpret
         */
        void putLong(long value);

        /**
         * Interprets and writes boolean `value` in next one byte
         * @param value Boolean to interpret
         */
        void putBoolean(boolean value);
    }
    public static class Output extends BufferedOutputStream implements JavaStreamWrite, ParsableOutput {
        private final ByteBuffer shortBuffer = ByteBuffer.allocate(Short.BYTES);
        private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
        private final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

        public Output(OutputStream out) {
            super(out);
        }

        @Override
        public void writeSegment(byte[] bytes, JavaStreamSegmentType segmentType) {
            try {
                if (segmentType == JavaStreamSegmentType.SHORT)
                    putShort((short) bytes.length);
                else if (segmentType == JavaStreamSegmentType.INTEGER)
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
        public void putShort(short value) {
            try {
                write(shortBuffer.position(0).putShort(value).array());
            } catch (IOException e) {
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
