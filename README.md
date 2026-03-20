# Outerstellar Framework

[![CI](https://github.com/rygel/outerstellar-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/rygel/outerstellar-framework/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Headless, framework-agnostic Kotlin utilities for modern JVM development.

## Modules

| Module | Description |
|--------|-------------|
| **outerstellar-i18n** | Hot-reloading translation service with Spring-style parameter replacement and locale-based ResourceBundle caching |
| **outerstellar-theme** | JSON-based color parsing engine with smart shading, CSS variable generation, and multi-source theme loading |
| **outerstellar-plugin** | SPI extension engine for runtime plugin discovery, lifecycle management, and caching via ServiceLoader |
| **outerstellar-i18n-validator** | i18n validation engine — detects missing translations, unused keys, and undefined keys in source code |
| **outerstellar-i18n-validator-maven-plugin** | Maven plugin for build-time i18n key validation |
| **outerstellar-i18n-validator-cli** | Command-line interface for i18n validation |
| **outerstellar-i18n-validator-gui** | Swing GUI for interactive i18n validation |
| **outerstellar-world-cities** | Utility for converting PostgreSQL world cities data to H2 format with SQL compression |

## Installation

### GitHub Packages

Add the repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github-rygel</id>
        <url>https://maven.pkg.github.com/rygel/outerstellar-framework</url>
    </repository>
</repositories>
```

Then add the modules you need:

```xml
<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>outerstellar-i18n</artifactId>
    <version>1.0.3</version>
</dependency>

<dependency>
    <groupId>io.github.rygel</groupId>
    <artifactId>outerstellar-theme</artifactId>
    <version>1.0.3</version>
</dependency>
```

## Quick Start

### i18n

```kotlin
val i18n = I18nService.create("messages") // loads messages.properties
val greeting = i18n.translate("welcome.message", userName)
```

### Theme

```kotlin
val theme = ThemeService.create().loadFromClasspath("themes/dark.json")
val css = theme.toCssForSelector(":root")
```

## Requirements

- JDK 21+
- Kotlin 2.0+

## Kotlin Version Policy

The framework compiles against Kotlin 2.0 intentionally. Kotlin guarantees forward compatibility — code compiled with 2.0 works in projects using 2.3+. By targeting the oldest supported version, consumers are free to use any Kotlin 2.x version without conflicts. The framework Kotlin version will only be bumped when a newer language feature is required.

## Building

```bash
mvn clean install
```

## License

[Apache License 2.0](LICENSE)
