package pl.project13.maven.git.log;

import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class MavenLoggerBridgeTest {
  Logger logger = mock(Logger.class);
  MavenLoggerBridge bridge = new MavenLoggerBridge(null, true);

  @Test
  public void shouldNotFailWhenMessageContainsPercentSigns() throws Exception {
    // given
    String start = "the output was: [";
    String content = "100% coverage!!!";
    String end = "]";
    String expectedExplicit = "the output was: [ 100% coverage!!! ]";

    // when
    bridge.setLogger(logger);
    bridge.log(start, content, end);

    // then
    verify(logger).info(expectedExplicit);
  }

}
