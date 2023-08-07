package org.rcsb.strucmotif;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.nio.file.NoSuchFileException;

/**
 * Provides better error messages when data files are missing.
 */
public class NoSuchFileFailureAnalyzer extends AbstractFailureAnalyzer<UnsatisfiedDependencyException> {
    /**
     * Default constructor.
     */
    public NoSuchFileFailureAnalyzer() {
    }

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, UnsatisfiedDependencyException cause) {
        Throwable mostSpecific = cause.getMostSpecificCause();
        if (mostSpecific instanceof NoSuchFileException) {
            return new FailureAnalysis(getDescription(cause), getAction((NoSuchFileException) mostSpecific), cause);
        }

        // this causes the standard, super-verbose stack trace to kick in
        return null;
    }

    private String getDescription(UnsatisfiedDependencyException ex) {
        return String.format("The bean '%s' could not be created, the root cause is '%s'.",
                ex.getBeanName(), ex.getMostSpecificCause());
    }

    private String getAction(NoSuchFileException ex) {
        return String.format("Make sure that the directory containing '%s' exists and this process has the necessary read/write permissions.",
                ex.getFile());
    }
}
