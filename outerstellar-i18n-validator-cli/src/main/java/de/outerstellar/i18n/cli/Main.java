package de.outerstellar.i18n.cli;

import de.outerstellar.i18n.core.Config;
import de.outerstellar.i18n.core.I18nValidator;
import de.outerstellar.i18n.core.Statistics;
import de.outerstellar.i18n.core.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CLI entry point for i18n validation.
 *
 * <p>Usage: {@code java -jar i18n-cli.jar -r src/main/resources -p . --fail}</p>
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length == 0 || hasFlag(args, "-h", "--help")) {
            printHelp();
            return;
        }

        String resourcesPath = getArg(args, "-r", "--resources");
        String projectPath = getArg(args, "-p", "--project");
        String baseFile = getArg(args, "-b", "--base-file");
        String patternsArg = getArg(args, "--patterns", null);
        String extensionsArg = getArg(args, "--extensions", null);
        String ignoreArg = getArg(args, "--ignore", null);
        boolean failOnError = hasFlag(args, "-f", "--fail");
        boolean fix = hasFlag(args, "--fix", null);
        boolean verbose = hasFlag(args, "-v", "--verbose");

        if (resourcesPath == null) {
            System.err.println("Error: -r/--resources is required");
            System.exit(1);
        }

        Config.Builder builder = Config.builder()
                .resourcesPath(resourcesPath)
                .projectPath(projectPath);

        if (baseFile != null) builder.baseFileName(baseFile);
        if (patternsArg != null) builder.sourcePatterns(List.of(patternsArg.split(",")));
        if (extensionsArg != null) builder.scanExtensions(List.of(extensionsArg.split(",")));
        if (ignoreArg != null) builder.ignorePatterns(List.of(ignoreArg.split(",")));

        Config config = builder.build();
        I18nValidator validator = new I18nValidator(config);

        System.out.println("i18n Validator");
        System.out.println("==============");
        System.out.println("Resources: " + resourcesPath);
        if (projectPath != null) System.out.println("Project:   " + projectPath);
        System.out.println();

        List<ValidationResult> results = validator.validate(
                verbose ? msg -> System.out.println("  > " + msg) : null
        );

        // Print issues
        boolean hasIssues = false;
        for (ValidationResult r : results) {
            if (r.status() != ValidationResult.Status.OK) {
                if (!hasIssues) {
                    System.out.println("\nIssues:");
                    hasIssues = true;
                }
                System.out.println("  " + r);
            }
        }

        Statistics stats = validator.getStatistics();
        System.out.println("\n" + stats);

        if (fix && stats.missing() > 0) {
            System.out.println("\n=== Fix suggestions ===");
            Map<String, List<String>> missing = validator.getMissingKeysByLocale();
            for (var entry : missing.entrySet()) {
                System.out.println("\nAdd to messages_" + entry.getKey() + ".properties:");
                for (String key : entry.getValue()) {
                    System.out.println("  " + key + "=<TRANSLATE>");
                }
            }
        }

        if (failOnError && !stats.isValid()) {
            System.err.println("\nValidation FAILED: " + stats.totalIssues() + " issues");
            System.exit(1);
        } else if (!hasIssues) {
            System.out.println("\nAll validations passed!");
        }
    }

    private static String getArg(String[] args, String shortFlag, String longFlag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(shortFlag) || (longFlag != null && args[i].equals(longFlag))) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static boolean hasFlag(String[] args, String shortFlag, String longFlag) {
        for (String arg : args) {
            if (arg.equals(shortFlag) || (longFlag != null && arg.equals(longFlag))) return true;
        }
        return false;
    }

    private static void printHelp() {
        System.out.println("i18n Validator — Check translation property files for consistency");
        System.out.println();
        System.out.println("Usage: java -jar i18n-cli.jar [options]");
        System.out.println();
        System.out.println("Required:");
        System.out.println("  -r, --resources <path>    Directory containing .properties files");
        System.out.println();
        System.out.println("Optional:");
        System.out.println("  -p, --project <path>      Project root (for source code scanning)");
        System.out.println("  -b, --base-file <name>    Base file name (default: messages.properties)");
        System.out.println("  --patterns <p1,p2,...>     Source code patterns (default: i18n.translate,I18n.get)");
        System.out.println("  --extensions <e1,e2,...>   File extensions to scan (default: java,kt,kte,jte)");
        System.out.println("  --ignore <p1,p2,...>       Key patterns to ignore (e.g., debug.*,test.*)");
        System.out.println("  -f, --fail                Exit with code 1 on validation errors");
        System.out.println("  --fix                     Show fix suggestions for missing keys");
        System.out.println("  -v, --verbose             Verbose output");
        System.out.println("  -h, --help                Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar i18n-cli.jar -r src/main/resources -p . -f --fix");
        System.out.println("  java -jar i18n-cli.jar -r src/main/resources --patterns \"i18n.translate\" -v");
    }
}
