package bgu.spl.net.impl.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class StompMessageEncoderDecoderTest {
    @Test
    void testEncodeAppendsNullTerminator() {
        StompMessageEncoderDecoder encdec = new StompMessageEncoderDecoder();
        String frame = "CONNECT\nversion:1.2\n\n";
        byte[] bytes = encdec.encode(frame);
        assertEquals((frame + "\u0000").length(), new String(bytes).length());
        assertTrue(new String(bytes).endsWith("\u0000"));
    }

    @Test
    void testDecodeReturnsOnNullByte() {
        StompMessageEncoderDecoder encdec = new StompMessageEncoderDecoder();
        String frame = "SEND\ndestination:/a\n\nhi";
        String built = null;
        for (byte b : (frame + "\u0000more").getBytes()) {
            String s = encdec.decodeNextByte(b);
            if (s != null) {
                built = s;
                break;
            }
        }
        assertEquals(frame, built);
    }

    @Test
    void testDecodeHandlesMultipleFrames() {
        StompMessageEncoderDecoder encdec = new StompMessageEncoderDecoder();
        String f1 = "MESSAGE\n\nfirst";
        String f2 = "MESSAGE\n\nsecond";
        String d1 = null, d2 = null;
        for (byte b : (f1 + "\u0000" + f2 + "\u0000").getBytes()) {
            String s = encdec.decodeNextByte(b);
            if (s != null) {
                if (d1 == null) d1 = s; else { d2 = s; break; }
            }
        }
        assertEquals(f1, d1);
        assertEquals(f2, d2);
    }
}
