package com.eden.orchid.api.indexing;

import com.eden.orchid.api.OrchidContext;
import com.eden.orchid.api.OrchidService;
import com.eden.orchid.testhelpers.BaseOrchidTest;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashSet;

import static org.mockito.Mockito.*;

public final class IndexServiceTest extends BaseOrchidTest {

    private OrchidContext context;
    private IndexService underTest;
    private IndexServiceImpl service;

    @BeforeEach
    public void setUp() {
        super.setUp();
        // test the service directly
        context = mock(OrchidContext.class);
        service = new IndexServiceImpl(new HashSet<>());
        service.initialize(context);

        // test that the default implementation is identical to the real implementation
        underTest = new IndexService() {
            public void initialize(OrchidContext context) { }
            public <T extends OrchidService> T getService(Class<T> serviceClass) { return (T) service; }
        };
    }

}
