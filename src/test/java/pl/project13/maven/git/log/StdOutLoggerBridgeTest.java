package pl.project13.maven.git.log;

import org.junit.Test;

public class StdOutLoggerBridgeTest {

  @Test
  public void log_shouldNotFailWhenMessageContainsPercentSign() throws Exception {
    // given
    StdOutLoggerBridge bridge = new StdOutLoggerBridge(true);

    // when
    bridge.log("'- Finished tests for User Account service and Network service (100% coverage)'");

    // then, should not have thrown
  }

  @Test
  public void error_shouldNotFailWhenMessageContainsPercentSign() throws Exception {
    // given
    StdOutLoggerBridge bridge = new StdOutLoggerBridge(true);

    // when
    bridge.error("AAAA all tests are burning. All 100% of them!");

    // then, should not have thrown
  }

}
