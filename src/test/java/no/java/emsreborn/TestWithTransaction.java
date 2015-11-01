package no.java.emsreborn;

import org.junit.After;
import org.junit.Before;

public class TestWithTransaction {

    private ServiceLocator serviceLocator;

    @Before
    public void startTransaction() throws Exception {
        serviceLocator = ServiceLocator.startTransaction();
    }

    @After
    public void endTransaction() throws Exception {
        serviceLocator.close();
    }
}
