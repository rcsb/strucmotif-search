package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.SearchContext;

/**
 * Step-wise builder of the search context.
 * @param <C> type of the context to create
 */
public interface ContextBuilder<C extends SearchContext<?, ?, ?>> {
    /**
     * Parameters are considered mandatory arguments (in the sense that some value has to be given - nonetheless,
     * default values will be used). But internally these values are strictly required. No input validation is performed
     * whatsoever.
     * @param <C> type of the context to create
     */
    interface MandatoryBuilder<C> {
        /**
         * Create the parameter object and move on to the optional step.
         * @return the optional builder instance
         */
        OptionalBuilder<C> buildParameters();
    }

    /**
     * Optional values.
     * @param <C> type of the context to create
     */
    interface OptionalBuilder<C> {
        /**
         * Create the context.
         * @return a context
         */
        C buildContext();
    }
}
