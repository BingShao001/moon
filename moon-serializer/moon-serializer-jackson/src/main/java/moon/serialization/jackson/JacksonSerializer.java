package moon.serialization.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import moon.codec.Serializer;
import moon.util.StringUtils;
import java.io.IOException;

/**
 * ${DESCRIPTION}
 *
 * @author Ricky Fung
 */
public class JacksonSerializer implements Serializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(Object msg) throws IOException {
        String jsonString = objectMapper.writeValueAsString(msg);
        return StringUtils.getBytes(jsonString);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> type) throws IOException {
        String jsonString =  StringUtils.getString(data);
        return objectMapper.readValue(jsonString, type);
    }
}
