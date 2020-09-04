package org.rcsb.strucmotif;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class MotifSearchContext implements ApplicationContextAware {
    private static ApplicationContext context;

    /**
     * Returns the Spring managed bean instance of the given class type. Returns if no managed bean of that type exists.
     * @param beanClass the requested class
     * @return a managed instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        MotifSearchContext.context = context;
    }
}
