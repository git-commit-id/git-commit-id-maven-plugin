package pl.project13.maven.git.release;

/**
 * Created by pankaj on 11/19/16.
 */
public class Feature {
    private String description;
    private String commitHashShort;
    private String commitHashLong;
    private String author;
    private String commitTime;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCommitHashShort() {
        return commitHashShort;
    }

    public void setCommitHashShort(String commitHashShort) {
        this.commitHashShort = commitHashShort;
    }

    public String getCommitHashLong() {
        return commitHashLong;
    }

    public void setCommitHashLong(String commitHashLong) {
        this.commitHashLong = commitHashLong;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(String commitTime) {
        this.commitTime = commitTime;
    }
}
