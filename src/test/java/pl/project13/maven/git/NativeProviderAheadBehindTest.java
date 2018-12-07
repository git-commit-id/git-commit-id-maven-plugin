package pl.project13.maven.git;

public class NativeProviderAheadBehindTest extends AheadBehindTest<NativeGitProvider> {

    @Override
    protected NativeGitProvider gitProvider() {
	return new NativeGitProvider(localRepository.getRoot(), 1000l, null);
    }
    
    @Override
    protected void extraSetup() {
	gitProvider.setEvaluateOnCommit("HEAD");
    }
}
