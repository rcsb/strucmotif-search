package org.rcsb.strucmotif;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * https://gist.github.com/virasak/3798194
 */
public class GuiceJUnit4Runner extends BlockJUnit4ClassRunner {
    public GuiceJUnit4Runner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Object createTest() throws Exception {
        Object object = super.createTest();
        Injector injector = Guice.createInjector(new MockMotifSearch.MockModule());
        injector.injectMembers(object);
        return object;
    }
}
