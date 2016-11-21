package pl.project13.maven.git.release;

import java.util.List;

/**
 * Created by pankaj on 11/19/16.
 */
public class Tag {

    private String name;
    private List<Feature> featureList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Feature> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(List<Feature> featureList) {
        this.featureList = featureList;
    }
}
