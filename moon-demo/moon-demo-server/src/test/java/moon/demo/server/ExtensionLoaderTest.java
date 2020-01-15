package moon.demo.server;

import moon.codec.Serializer;
import moon.core.extension.ExtensionLoader;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class ExtensionLoaderTest {

    @Test
    public void testApp() {

        ExtensionLoader loader = ExtensionLoader.getExtensionLoader(Serializer.class);
        loader.hasExtension("hessian");
    }
}
