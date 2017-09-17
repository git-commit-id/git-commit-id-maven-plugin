package pl.project13.git.api;

public interface ReplacementProperty {
	public String getProperty();
	public String getToken();
	public String getValue();
	public boolean isRegex();
}
