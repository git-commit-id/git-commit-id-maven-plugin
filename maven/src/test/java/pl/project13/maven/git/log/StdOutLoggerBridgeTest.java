package pl.project13.maven.git.log;

import org.junit.Test;

public class StdOutLoggerBridgeTest {

  @Test
  public void log_shouldNotFailWhenMessageContainsPercentSign() throws Exception {
    // given
    StdOutLoggerBridge bridge = new StdOutLoggerBridge(true);

    // when
    bridge.log();

    // then, should not have thrown
  }

  @Test
  public void error_shouldNotFailWhenMessageContainsPercentSign() throws Exception {
    // given
    StdOutLoggerBridge bridge = new StdOutLoggerBridge(true);

    // when
    bridge.error();

    // then, should not have thrown
  }

}
