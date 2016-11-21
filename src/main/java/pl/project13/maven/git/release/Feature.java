package pl.project13.maven.git.release;

import java.util.Date;

/**
 * Created by pankaj on 11/19/16.
 */
public class Feature {
    private String description;
    private String commitHashShort;
    private String commitHashLong;
    private String author;
    private Date commitTime;

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

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }
}
