package moesif.analytics.reporter.utils;
import java.nio.ByteBuffer;
import java.util.UUID;
public class UUIDCreator {

    public static byte[] joinByteArray(byte[] byte1, byte[] byte2) {
        return ByteBuffer.allocate(byte1.length + byte2.length)
                .put(byte1)
                .put(byte2)
                .array();
    }

    public String getUUIDStrFromName(String namespace, String name){
        byte[] nameSpaceByteArray = namespace.getBytes();
        byte[] nameByteArray = name.getBytes();
        byte[] result = joinByteArray(nameSpaceByteArray,nameByteArray);
        UUID uuid = UUID.nameUUIDFromBytes(result);
        return uuid.toString();
    }
}
