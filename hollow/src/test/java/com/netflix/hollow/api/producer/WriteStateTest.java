package com.netflix.hollow.api.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.hollow.core.write.HollowWriteStateEngine;
import com.netflix.hollow.core.write.objectmapper.HollowObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WriteStateTest {
    @Mock
    private HollowWriteStateEngine writeStateEngine;
    @Mock
    private HollowObjectMapper objectMapper;

    private CloseableWriteState subject;
    private long version;

    @BeforeEach
    public void before() {
        when(objectMapper.getStateEngine()).thenReturn(writeStateEngine);

        version = 13L;
        subject = new CloseableWriteState(version, objectMapper, null);
    }

    @Test
    public void add_delegatesToObjectMapper() {
        subject.add("Yes!");
        verify(objectMapper).add("Yes!");
    }

    @Test
    public void getObjectMapper() {
        assertEquals(objectMapper, subject.getObjectMapper());
    }

    @Test
    public void getStateEngine_delegatesToObjectMapper() throws Exception {
        assertEquals(writeStateEngine, subject.getStateEngine());
    }

    @Test
    public void add_whenClosed() {
        assertThrowsAfterClose(() -> subject.add("Nope!"));
    }

    @Test
    public void getObjectMapper_whenClosed() {
        assertThrowsAfterClose(() -> subject.getObjectMapper());
    }

    @Test
    public void getStateEngine_whenClosed() {
        assertThrowsAfterClose(() -> subject.getStateEngine());
    }

    @Test
    public void getPriorState_whenClosed() {
        assertThrowsAfterClose(() -> subject.getPriorState());
    }

    @Test
    public void getVersion_whenClosed() {
        assertThrowsAfterClose(() -> subject.getVersion());
    }

    private void assertThrowsAfterClose(Runnable code) {
        subject.close();
        try {
            code.run();
            fail("should throw");
        } catch (IllegalStateException e) {
            assertEquals("Write state operated on after the population stage of a cycle; version=" + version, e.getMessage());
        }
    }
}
