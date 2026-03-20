package de.outerstellar.i18n.maven;

import de.outerstellar.i18n.core.Config;
import de.outerstellar.i18n.core.I18nValidator;
import de.outerstellar.i18n.core.Statistics;
import de.outerstellar.i18n.core.ValidationResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Validates i18n property files for missing keys, blank values,
 * placeholder consistency, and source code cross-referencing.
 *
 * <p>Usage in pom.xml:</p>
 * <pre>{@code
 * <plugin>
 *   <groupId>de.outerstellar</groupId>
 *   <artifactId>outerstellar-i18n-maven-plugin</artifactId>
 *   <version>1.0.2</version>
 *   <configuration>
 *     <resourcesDir>src/main/resources</resourcesDir>
 *     <baseFile>messages.properties</baseFile>
 *     <sourcePatterns>
 *       <pattern>i18n.translate</pattern>
 *     </sourcePatterns>
 *     <scanExtensions>
 *       <extension>kt</extension>
 *       <extension>kte</extension>
 *     </scanExtensions>
 *   </configuration>
 *   <executions>
 *     <execution>
 *       <goals><goal>validate</goal></goals>
 *     </execution>
 *   </executions>
 * </plugin>
 * }</pre>
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ValidateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/resources", property = "i18n.resourcesDir")
    private File resourcesDir;

    @Parameter(defaultValue = "${project.basedir}", property = "i18n.projectDir")
    private File projectDir;

    @Parameter(defaultValue = "messages.properties", property = "i18n.baseFile")
    private String baseFile;

    @Parameter(property = "i18n.sourcePatterns")
    private List<String> sourcePatterns;

    @Parameter(property = "i18n.scanExtensions")
    private List<String> scanExtensions;

    @Parameter(property = "i18n.ignorePatterns")
    private List<String> ignorePatterns;

    @Parameter(defaultValue = "true", property = "i18n.checkPlaceholders")
    private boolean checkPlaceholders;

    @Parameter(defaultValue = "true", property = "i18n.failOnMissing")
    private boolean failOnMissing;

    @Parameter(defaultValue = "false", property = "i18n.failOnUndefined")
    private boolean failOnUndefined;

    @Parameter(defaultValue = "true", property = "i18n.showFixSuggestions")
    private boolean showFixSuggestions;

    @Parameter(defaultValue = "false", property = "i18n.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            getLog().info("i18n validation skipped");
            return;
        }

        Config config = Config.builder()
                .resourcesPath(resourcesDir.getAbsolutePath())
                .projectPath(projectDir.getAbsolutePath())
                .baseFileName(baseFile)
                .sourcePatterns(sourcePatterns != null ? sourcePatterns : List.of("i18n.translate", "I18n.get", "I18n.format"))
                .scanExtensions(scanExtensions != null ? scanExtensions : List.of("java", "kt", "kte", "jte"))
                .ignorePatterns(ignorePatterns != null ? ignorePatterns : List.of())
                .checkPlaceholders(checkPlaceholders)
                .failOnMissing(failOnMissing)
                .failOnUndefined(failOnUndefined)
                .build();

        I18nValidator validator = new I18nValidator(config);
        List<ValidationResult> results = validator.validate(msg -> getLog().info(msg));

        // Print issues
        boolean hasIssues = false;
        for (ValidationResult r : results) {
            if (r.status() != ValidationResult.Status.OK) {
                if (!hasIssues) {
                    getLog().warn("i18n validation issues:");
                    hasIssues = true;
                }
                switch (r.status()) {
                    case MISSING, UNDEFINED, PLACEHOLDER_MISMATCH -> getLog().error("  " + r);
                    case BLANK, UNUSED -> getLog().warn("  " + r);
                    default -> getLog().info("  " + r);
                }
            }
        }

        Statistics stats = validator.getStatistics();
        getLog().info("");
        getLog().info("i18n validation: " + stats);

        // Fix suggestions
        if (showFixSuggestions && stats.missing() > 0) {
            getLog().info("");
            getLog().info("=== Fix suggestions ===");
            Map<String, List<String>> missing = validator.getMissingKeysByLocale();
            for (var entry : missing.entrySet()) {
                getLog().info("Add to " + baseFile.replace(".properties", "_" + entry.getKey() + ".properties") + ":");
                for (String key : entry.getValue()) {
                    getLog().info("  " + key + "=<TRANSLATE>");
                }
            }
        }

        // Fail build if configured
        if (!stats.isValid()) {
            String msg = "i18n validation failed: " + stats.totalIssues() + " issues found";
            if ((failOnMissing && stats.missing() > 0) ||
                    (failOnUndefined && stats.undefined() > 0) ||
                    stats.placeholderMismatch() > 0) {
                throw new MojoFailureException(msg);
            }
            getLog().warn(msg);
        } else {
            getLog().info("i18n validation passed!");
        }
    }
}
