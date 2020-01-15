package moon.codec;

import moon.core.extension.SPI;
import moon.core.extension.Scope;

import java.io.IOException;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
@SPI(value = "protostuff", scope = Scope.SINGLETON)
public interface Serializer {

    byte[] serialize(Object msg) throws IOException;

    <T> T deserialize(byte[] data, Class<T> type) throws IOException;
}
