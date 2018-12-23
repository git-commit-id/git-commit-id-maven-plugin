package pl.project13.maven.git;

import java.util.Objects;

public class AheadBehind {

  public static final AheadBehind NO_REMOTE = AheadBehind.of("NO_REMOTE", "NO_REMOTE");

  private final String ahead;

  private final String behind;

  private AheadBehind(String ahead, String behind) {
    this.ahead = ahead;
    this.behind = behind;
  }

  public static AheadBehind of(int ahead, int behind) {
    return new AheadBehind(String.valueOf(ahead), String.valueOf(behind));
  }

  public static AheadBehind of(String ahead, String behind) {
    return new AheadBehind(ahead, behind);
  }

  public String ahead() {
    return ahead;
  }

  public String behind() {
    return behind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ahead, behind);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AheadBehind other = (AheadBehind) obj;

    if (!Objects.equals(ahead, other.ahead)) {
      return false;
    }
    if (!Objects.equals(behind, other.behind)) {
      return false;
    }
    return true;
  }
}