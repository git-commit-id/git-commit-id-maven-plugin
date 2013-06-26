package pl.project13.maven.git.log;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MavenLoggerBridgeTest {

  Log logger = mock(Log.class);

  MavenLoggerBridge bridge = new MavenLoggerBridge(logger, true);

  @Test
  public void shouldNotFailWhenMessageContainsPercentSigns() throws Exception {
    // given
    String msg = "the output was: [%s]";
    String content = "100% coverage!!!";
    String expectedExplicit = "the output was: [100% coverage!!!]";

    // when
    bridge.log(msg, content);

    // then
    verify(logger).info(expectedExplicit);
  }

}
