package pl.project13.maven.git.release;

import java.util.Date;
import java.util.List;

/**
 * Created by pankaj on 11/20/16.
 */
public class ReleaseNotes {

    public ReleaseNotes() {
        this.generationTime = new Date();
    }
    private Date generationTime;
    private List<Tag> tagList;

    public Date getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(Date generationTime) {
        this.generationTime = generationTime;
    }

    public List<Tag> getTagList() {
        return tagList;
    }

    public void setTagList(List<Tag> tagList) {
        this.tagList = tagList;
    }
}
