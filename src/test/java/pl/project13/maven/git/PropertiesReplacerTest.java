package pl.project13.maven.git;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.project13.maven.git.log.MavenLoggerBridge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;

@RunWith(JUnitParamsRunner.class)
public class PropertiesReplacerTest
{
	public static Collection useRegexReplacement() {
		return asList(true, false);
	}

	private PropertiesReplacer propertiesReplacer;

	@Before
	public void setUp() {
		this.propertiesReplacer = new PropertiesReplacer(mock(MavenLoggerBridge.class));
	}

	@Test
	public void testPerformReplacementWithNullValues() throws IOException
	{
		Properties properties = null;
		List<ReplacementProperty> replacementProperties = null;
		propertiesReplacer.performReplacement(properties, replacementProperties);
	}

	@Test
	@Parameters(method = "useRegexReplacement")
	public void testPerformReplacementWithInvalidReplacement(boolean regex) throws IOException {
		Properties actualProperties = build("key1", "value1", "key2", "value2");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty("key1", null, null, regex));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);
	}

	@Test
	@Parameters(method = "useRegexReplacement")
	public void testPerformReplacementWithSingleProperty(boolean regex) throws IOException {
		Properties actualProperties = build("key1", "value1", "key2", "value2");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty("key1", "value", "another", regex));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("key1", "another1", "key2", "value2");
		assertEquals(exptecedProperties, actualProperties);
	}

	@Test
	@Parameters(method = "useRegexReplacement")
	public void testPerformReplacementWithMultipleProperties(boolean regex) throws IOException {
		Properties actualProperties = build("key1", "value1", "key2", "value2");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty(null, "value", "another", regex));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("key1", "another1", "key2", "another2");
		assertEquals(exptecedProperties, actualProperties);
	}

	@Test
	public void testPerformReplacementWithPatternGroupAndMatching() throws IOException {
		Properties actualProperties = build("git.branch", "feature/feature_name", "git.commit.author", "author/name");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty("git.branch", "^([^\\/]*)\\/([^\\/]*)$", "$1-$2", true));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("git.branch", "feature-feature_name", "git.commit.author", "author/name");
		assertEquals(exptecedProperties, actualProperties);
	}

	@Test
	public void testPerformReplacementWithPatternGroupAndNoMatch() throws IOException {
		Properties actualProperties = build("git.branch", "feature#feature_name", "git.commit.author", "author#");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty("git.branch", "^([^\\/]*)\\/([^\\/]*)$", "$1-$2", true));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("git.branch", "feature#feature_name", "git.commit.author", "author#");
		assertEquals(exptecedProperties, actualProperties);
	}

	private Properties build(String key1, String value1, String key2, String value2) {
		Properties properties = new Properties();
		properties.put(key1, value1);
		properties.put(key2, value2);
		return properties;
	}

	private void assertEquals(Properties expected, Properties actual) {
		if(expected == null) {
			Assert.assertNull(actual);
		} else if(actual == null) {
			Assert.assertNull(expected);
		} else {
			Assert.assertEquals(expected.size(), actual.size());
			for(Map.Entry<Object, Object> expectedElementEntry : expected.entrySet()) {
				String expectedKey = (String)expectedElementEntry.getKey();
				String expectedValue = (String)expectedElementEntry.getValue();
				Assert.assertTrue(actual.containsKey(expectedKey));
				String actualValue = actual.getProperty(expectedKey);
				Assert.assertEquals(expectedValue, actualValue);
			}
		}
	}

}