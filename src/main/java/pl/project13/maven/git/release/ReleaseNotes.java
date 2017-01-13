package pl.project13.maven.git.release;

import java.util.List;

/**
 * Created by pankaj on 11/20/16.
 */
public class ReleaseNotes {

    private String generationTime;
    private String startTag;
    private String endTag;
    private String commitMessageRegex;
    private String tagNameRegex;

    private List<Tag> tagList;

    public String getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(String generationTime) {
        this.generationTime = generationTime;
    }

    public List<Tag> getTagList() {
        return tagList;
    }

    public void setTagList(List<Tag> tagList) {
        this.tagList = tagList;
    }

    public String getStartTag() {
        return startTag;
    }

    public void setStartTag(String startTag) {
        this.startTag = startTag;
    }

    public String getEndTag() {
        return endTag;
    }

    public void setEndTag(String endTag) {
        this.endTag = endTag;
    }

    public String getCommitMessageRegex() {
        return commitMessageRegex;
    }

    public void setCommitMessageRegex(String commitMessageRegex) {
        this.commitMessageRegex = commitMessageRegex;
    }

    public String getTagNameRegex() {
        return tagNameRegex;
    }

    public void setTagNameRegex(String tagNameRegex) {
        this.tagNameRegex = tagNameRegex;
    }
}
