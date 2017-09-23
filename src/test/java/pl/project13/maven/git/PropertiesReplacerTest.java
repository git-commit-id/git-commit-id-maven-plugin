package pl.project13.maven.git;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import pl.project13.maven.git.TransformationRule.ActionEnum;
import pl.project13.maven.git.TransformationRule.ApplyEnum;
import pl.project13.maven.git.log.MavenLoggerBridge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
		replacementProperties.add(new ReplacementProperty("key1", null, null, null, regex, null));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);
	}

	@Test
	@Parameters(method = "useRegexReplacement")
	public void testPerformReplacementWithSingleProperty(boolean regex) throws IOException {
		Properties actualProperties = build("key1", "value1", "key2", "value2");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty("key1", null, "value", "another", regex, null));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("key1", "another1", "key2", "value2");
		assertEquals(exptecedProperties, actualProperties);
	}

	@Test
	@Parameters(method = "useRegexReplacement")
	public void testPerformReplacementWithMultipleProperties(boolean regex) throws IOException {
		Properties actualProperties = build("key1", "value1", "key2", "value2");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty(null, null, "value", "another", regex, null));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("key1", "another1", "key2", "another2");
		assertEquals(exptecedProperties, actualProperties);
	}

	@Test
	public void testPerformReplacementWithPatternGroupAndMatching() throws IOException {
		Properties actualProperties = build("git.branch", "feature/feature_name", "git.commit.author", "author/name");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty("git.branch", null, "^([^\\/]*)\\/([^\\/]*)$", "$1-$2", true, null));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("git.branch", "feature-feature_name", "git.commit.author", "author/name");
		assertEquals(exptecedProperties, actualProperties);
	}

	@Test
	public void testPerformReplacementWithPatternGroupAndNoMatch() throws IOException {
		Properties actualProperties = build("git.branch", "feature#feature_name", "git.commit.author", "author#");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty("git.branch", null, "^([^\\/]*)\\/([^\\/]*)$", "$1-$2", true, null));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("git.branch", "feature#feature_name", "git.commit.author", "author#");
		assertEquals(exptecedProperties, actualProperties);
	}

	@Test
	public void testPerformReplacementOnSinglePropertyAndExpectNewPropertyGenerated() {
		Properties actualProperties = build("git.branch", "feature/feature_name", "git.commit.author", "author#");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty("git.branch", "something", "^([^\\/]*)\\/([^\\/]*)$", "$1-$2", true, null));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("git.branch", "feature/feature_name", "git.branch.something", "feature-feature_name", "git.commit.author", "author#");
		assertEquals(exptecedProperties, actualProperties);
	}

	@Test
	public void testPerformReplacementOnEveryPropertyAndExpectNewPropertyGenerated() {
		Properties actualProperties = build("git.branch", "feature/feature_name", "git.commit.author", "author#");

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty(null, "something", "^([^\\/]*)\\/([^\\/]*)$", "$1-$2", true, null));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("git.branch", "feature/feature_name", "git.branch.something", "feature-feature_name", "git.commit.author", "author#", "git.commit.author.something", "author#");
		assertEquals(exptecedProperties, actualProperties);
	}

	public static Collection<Object[]> testPerformReplacementWithTransformationRule() {
		return Arrays.asList(new Object[][] {
			{ "feature/AbCdEfGh0123456789", "[^/a-z0-9\\-]", ApplyEnum.BEFORE, ActionEnum.LOWER_CASE, "feature/abcdefgh0123456789" },
			{ "feature/AbCdEfGh0123456789", "[^/a-z0-9\\-]", ApplyEnum.AFTER, ActionEnum.LOWER_CASE, "feature/-b-d-f-h0123456789" },
			{ "feature/AbCdEfGh0123456789", "[^/A-Z0-9\\-]", ApplyEnum.BEFORE, ActionEnum.UPPER_CASE, "FEATURE/ABCDEFGH0123456789" },
			{ "feature/AbCdEfGh0123456789", "[^/A-Z0-9\\-]", ApplyEnum.AFTER, ActionEnum.UPPER_CASE, "-------/A-C-E-G-0123456789" },
		});
	}

	@Test
	@Parameters(method = "testPerformReplacementWithTransformationRule")
	public void runTransformationTestHelper(String input, String regex, ApplyEnum applyRule, ActionEnum actionRule, String expectedOutput) {
		Properties actualProperties = build("git.branch", input);

		List<TransformationRule> transformationRules = new ArrayList<>();
		transformationRules.add(new TransformationRule(applyRule, actionRule));

		List<ReplacementProperty> replacementProperties = new ArrayList<>();
		replacementProperties.add(new ReplacementProperty(null, null, regex , "-", true, transformationRules));

		propertiesReplacer.performReplacement(actualProperties, replacementProperties);

		Properties exptecedProperties = build("git.branch", expectedOutput);
		assertEquals(exptecedProperties, actualProperties);
	}

	private Properties build(String... args) {
		if ((args.length == 0) || ((args.length % 2) != 0)) {
			Assert.fail("Expecting a pair of values...");
		}
		Properties properties = new Properties();
		for(int i=0; i<args.length;i=i+2) {
			String key = args[i];
			String value = args[i+1];
			properties.put(key, value);
		}
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
