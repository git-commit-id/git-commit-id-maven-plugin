package pl.project13.jgit;

import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class DescribeResultTest {

  String VERSION = "v2.5";
  String ZEROES_COMMIT_ID = "0000000000000000000000000000000000000000";
  String DIRTY_MARKER = "DEV";

  @Test
  public void shouldToStringForTag() throws Exception {
    // given
    DescribeResult res = new DescribeResult(VERSION);

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION);
  }


  @Test
  public void shouldToStringForDirtyTag() throws Exception {
    // given
    DescribeResult res = new DescribeResult(VERSION, 2, ObjectId.zeroId(), true, DIRTY_MARKER);

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + ZEROES_COMMIT_ID + "-" + DIRTY_MARKER);
  }

  @Test
  public void shouldToStringFor2CommitsAwayFromTag() throws Exception {
    // given
    DescribeResult res = new DescribeResult(VERSION, 2, ObjectId.zeroId());

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(VERSION + "-" + 2 + "-" + ZEROES_COMMIT_ID);
  }

  @Test
  public void shouldToStringForNoTagJustACommit() throws Exception {
    // given
    DescribeResult res = new DescribeResult(ObjectId.zeroId());

    // when
    String s = res.toString();

    // then
    assertThat(s).isEqualTo(ZEROES_COMMIT_ID);
  }
}
