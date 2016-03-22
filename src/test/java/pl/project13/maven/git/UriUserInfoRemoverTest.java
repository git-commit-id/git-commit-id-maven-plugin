package pl.project13.maven.git;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by ryan on 3/21/16.
 */
public class UriUserInfoRemoverTest {

    @Test
    public void testHttpsUriWithoutUserInfo() throws Exception {
        String result = GitDataProvider.stripCredentialsFromOriginUrl("https://example.com");
        assertEquals("https://example.com", result);
    }

    @Test
    public void testHttpsUriWithUserInfo() throws Exception {
        String result = GitDataProvider.stripCredentialsFromOriginUrl("https://user@example.com");
        assertEquals("https://user@example.com", result);
    }

    @Test
    public void testHttpsUriWithUserInfoAndPassword() throws Exception {
        String result = GitDataProvider.stripCredentialsFromOriginUrl("https://user:password@example.com");
        assertEquals("https://user@example.com", result);
    }

    @Test
    public void testWithSCPStyleSSHProtocolGitHub() throws Exception {
        String result = GitDataProvider.stripCredentialsFromOriginUrl("git@github.com");
        assertEquals("git@github.com",result);
    }

    @Test
    public void testWithSCPStyleSSHProtocol() throws Exception {
        String result = GitDataProvider.stripCredentialsFromOriginUrl("user@host.xz:~user/path/to/repo.git");
        assertEquals("user@host.xz:~user/path/to/repo.git",result);
    }

    @Test
    public void testWithSSHUri() throws Exception {
        String result = GitDataProvider.stripCredentialsFromOriginUrl("ssh://git@github.com/");
        assertEquals("ssh://git@github.com/",result);
    }
}
