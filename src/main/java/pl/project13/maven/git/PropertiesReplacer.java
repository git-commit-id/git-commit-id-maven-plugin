/*
 * This file is part of git-commit-id-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.maven.git;

import pl.project13.maven.git.log.LoggerBridge;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class PropertiesReplacer
{
	private final LoggerBridge log;

	public PropertiesReplacer(LoggerBridge log)
	{
		this.log = log;
	}

	public void performReplacement(Properties properties, List<ReplacementProperty> replacementProperties) {
		if((replacementProperties != null) && (properties != null)) {
			for(ReplacementProperty replacementProperty: replacementProperties) {
				String propertyKey = replacementProperty.getProperty();
				if(propertyKey == null) {
					for (Map.Entry<Object, Object> entry : properties.entrySet()) {
						String key = (String)entry.getKey();
						String content = (String)entry.getValue();
						String result = performReplacement(replacementProperty, content);
						entry.setValue(result);
						log.info("apply replace on property " + key + ": original value '" + content + "' with '" + result + "'");
					}
				} else {
					String content = properties.getProperty(propertyKey);
					String result = performReplacement(replacementProperty, content);
					properties.setProperty(propertyKey, result);
					log.info("apply replace on property " + propertyKey + ": original value '" + content + "' with '" + result + "'");
				}
			}
		}
	}

	private String performReplacement(ReplacementProperty replacementProperty, String content) {
		String result = content;
		if(replacementProperty != null) {
			if(replacementProperty.isRegex()) {
				result = replaceRegex(content, replacementProperty.getToken(), replacementProperty.getValue());
			} else {
				result = replaceNonRegex(content, replacementProperty.getToken(), replacementProperty.getValue());
			}
		}
		return result;
	}

	private String replaceRegex(String content, String token, String value) {
		if((token == null) || (value == null)) {
			log.error("found replacementProperty without required token or value.");
			return content;
		}
		final Pattern compiledPattern = Pattern.compile(token);
		return compiledPattern.matcher(content).replaceAll(value);
	}

	private String replaceNonRegex(String content, String token, String value) {
		if((token == null) || (value == null)) {
			log.error("found replacementProperty without required token or value.");
			return content;
		}
		return content.replace(token, value);
	}
}
