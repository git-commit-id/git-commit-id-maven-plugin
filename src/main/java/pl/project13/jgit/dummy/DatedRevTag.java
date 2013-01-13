package pl.project13.jgit.dummy;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.revwalk.RevTag;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class DatedRevTag {

  public final AnyObjectId id;
  public final String tagName;
  public final DateTime date;

  public DatedRevTag(RevTag tag) {
    this(tag.getId(), tag.getTagName(), new DateTime(tag.getTaggerIdent().getWhen()));
  }

  public DatedRevTag(AnyObjectId id, String tagName) {
    this(id, tagName, DateTime.now().minusYears(2000));
  }

  public DatedRevTag(AnyObjectId id, String tagName, DateTime date) {
    this.id = id;
    this.tagName = tagName;
    this.date = date;
  }

  @Override
  public String toString() {
    return "DatedRevTag{" +
        "id=" + id.name() +
        ", tagName='" + tagName + '\'' +
        ", date=" + DateTimeFormat.longDateTime().print(date) +
        '}';
  }
}
