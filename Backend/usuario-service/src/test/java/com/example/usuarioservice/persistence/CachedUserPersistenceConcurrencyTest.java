package com.example.usuarioservice.persistence;

import com.example.usuarioservice.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CachedUserPersistenceConcurrencyTest {

    @Mock
    private IUserPersistence delegate;

    @Test
    @DisplayName("concurrent findById calls result in single delegate invocation and no exceptions")
    void concurrent_findById_singleDelegateCall() throws InterruptedException {
        CachedUserPersistenceDecorator cache = new CachedUserPersistenceDecorator(delegate);

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService ex = Executors.newFixedThreadPool(threads);

        // delegate will return same user after slight delay
        doAnswer(invocation -> {
            Thread.sleep(50);
            return new User(1, "Concurrent", "p", "c@test.com", true);
        }).when(delegate).findById(1);

        for (int i = 0; i < threads; i++) {
            ex.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    User u = cache.findById(1);
                    assertThat(u).isNotNull();
                    assertThat(u.getMail()).isEqualTo("c@test.com");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // wait all ready then start
        ready.await();
        start.countDown();

        // allow tasks to finish
        ex.shutdown();
        while (!ex.isTerminated()) {
            Thread.sleep(20);
        }

        // verify delegate called at least once (ideally once)
        verify(delegate, times(1)).findById(1);
    }
}
