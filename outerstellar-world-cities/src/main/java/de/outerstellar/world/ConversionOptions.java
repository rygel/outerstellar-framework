package de.outerstellar.world;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration options for the world cities conversion process.
 * 
 * @see WorldCitiesConverter
 */
public final class ConversionOptions {
    
    private final boolean compact;
    private final boolean gzipIntermediate;
    private final boolean cleanupIntermediate;
    private final Path workingDirectory;
    
    private ConversionOptions(Builder builder) {
        this.compact = builder.compact;
        this.gzipIntermediate = builder.gzipIntermediate;
        this.cleanupIntermediate = builder.cleanupIntermediate;
        this.workingDirectory = builder.workingDirectory;
    }
    
    /**
     * Returns default conversion options.
     */
    public static ConversionOptions defaults() {
        return new Builder().build();
    }
    
    /**
     * Creates a new builder for ConversionOptions.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Returns true if compact mode is enabled (removes optional columns).
     */
    public boolean compact() {
        return compact;
    }
    
    /**
     * Returns true if intermediate SQL file should be GZIP compressed.
     */
    public boolean gzipIntermediate() {
        return gzipIntermediate;
    }
    
    /**
     * Returns true if intermediate files should be cleaned up after conversion.
     */
    public boolean cleanupIntermediate() {
        return cleanupIntermediate;
    }
    
    /**
     * Returns the working directory for intermediate files.
     */
    public Path workingDirectory() {
        return workingDirectory;
    }
    
    @Override
    public String toString() {
        return String.format("ConversionOptions{compact=%s, gzip=%s, cleanup=%s, workDir=%s}",
            compact, gzipIntermediate, cleanupIntermediate, workingDirectory);
    }
    
    /**
     * Builder for ConversionOptions.
     */
    public static class Builder {
        private boolean compact = false;
        private boolean gzipIntermediate = false;
        private boolean cleanupIntermediate = true;
        private Path workingDirectory = Paths.get(".");
        
        /**
         * Sets whether to create a compact database (removes optional columns).
         * Default: false
         */
        public Builder compact(boolean compact) {
            this.compact = compact;
            return this;
        }
        
        /**
         * Sets whether to GZIP compress the intermediate SQL file.
         * Default: false
         */
        public Builder gzipIntermediate(boolean gzip) {
            this.gzipIntermediate = gzip;
            return this;
        }
        
        /**
         * Sets whether to cleanup intermediate files after conversion.
         * Default: true
         */
        public Builder cleanupIntermediate(boolean cleanup) {
            this.cleanupIntermediate = cleanup;
            return this;
        }
        
        /**
         * Sets the working directory for intermediate files.
         * Default: current directory
         */
        public Builder workingDirectory(Path directory) {
            this.workingDirectory = directory;
            return this;
        }
        
        /**
         * Builds the ConversionOptions instance.
         */
        public ConversionOptions build() {
            return new ConversionOptions(this);
        }
    }
}
