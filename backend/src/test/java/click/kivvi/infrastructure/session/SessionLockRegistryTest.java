package click.kivvi.infrastructure.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

/**
 * {@link SessionLockRegistry} in isolation, with no servlet or Spring context: same id serializes,
 * different ids run fully concurrently, and every entry is evicted once its last holder releases it
 * — the four properties the repair packet calls out for the lock registry itself.
 */
class SessionLockRegistryTest {

  private final SessionLockRegistry registry = new SessionLockRegistry();

  @Test
  void acquireReturnsTheSameLockInstanceForTheSameSessionId() {
    ReentrantLock first = registry.acquire("session-a");
    ReentrantLock second = registry.acquire("session-a");

    assertThat(second).isSameAs(first);

    registry.release("session-a");
    registry.release("session-a");
  }

  @Test
  void acquireReturnsDifferentLockInstancesForDifferentSessionIds() {
    ReentrantLock lockA = registry.acquire("session-a");
    ReentrantLock lockB = registry.acquire("session-b");

    assertThat(lockA).isNotSameAs(lockB);

    registry.release("session-a");
    registry.release("session-b");
  }

  @Test
  void sameSessionIdSerializesConcurrentCriticalSections() throws Exception {
    AtomicInteger concurrentHolders = new AtomicInteger();
    AtomicInteger maxObservedConcurrency = new AtomicInteger();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<?> first =
          pool.submit(() -> runInLock("shared-session", concurrentHolders, maxObservedConcurrency));
      Future<?> second =
          pool.submit(() -> runInLock("shared-session", concurrentHolders, maxObservedConcurrency));
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);
    } finally {
      pool.shutdown();
    }

    assertThat(maxObservedConcurrency)
        .as("two requests for the same session must never run inside the lock at the same time")
        .hasValue(1);
    assertThat(registry.trackedSessionCount()).isZero();
  }

  @Test
  void differentSessionIdsRunFullyConcurrently() throws Exception {
    CountDownLatch bothInsideTheirOwnLock = new CountDownLatch(2);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<?> first = pool.submit(() -> runAndRendezvous("session-x", bothInsideTheirOwnLock));
      Future<?> second = pool.submit(() -> runAndRendezvous("session-y", bothInsideTheirOwnLock));

      // If two different session ids shared a lock, the second thread could never reach the
      // rendezvous point while the first still holds it, and this would time out instead of
      // both futures completing.
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);
    } finally {
      pool.shutdown();
    }

    assertThat(registry.trackedSessionCount()).isZero();
  }

  @Test
  void entryIsEvictedOnceItsLastHolderReleasesIt() {
    ReentrantLock first = registry.acquire("session-a");
    assertThat(registry.trackedSessionCount()).isEqualTo(1);

    registry.release("session-a");
    assertThat(registry.trackedSessionCount())
        .as("no holders left, so the entry must be evicted")
        .isZero();

    ReentrantLock second = registry.acquire("session-a");
    assertThat(second)
        .as("a fresh acquire after eviction must create a brand new lock, not reuse a stale one")
        .isNotSameAs(first);
    registry.release("session-a");
  }

  @Test
  void entrySurvivesUntilEveryHolderHasReleased() {
    registry.acquire("session-a");
    registry.acquire("session-a");

    registry.release("session-a");
    assertThat(registry.trackedSessionCount())
        .as("one holder remains, so the entry must still be tracked")
        .isEqualTo(1);

    registry.release("session-a");
    assertThat(registry.trackedSessionCount()).isZero();
  }

  private void runInLock(String sessionId, AtomicInteger concurrentHolders, AtomicInteger max) {
    ReentrantLock lock = registry.acquire(sessionId);
    lock.lock();
    try {
      int now = concurrentHolders.incrementAndGet();
      max.updateAndGet(previous -> Math.max(previous, now));
      Thread.sleep(50);
      concurrentHolders.decrementAndGet();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    } finally {
      lock.unlock();
      registry.release(sessionId);
    }
  }

  private void runAndRendezvous(String sessionId, CountDownLatch bothInsideTheirOwnLock) {
    ReentrantLock lock = registry.acquire(sessionId);
    lock.lock();
    try {
      bothInsideTheirOwnLock.countDown();
      boolean reached = bothInsideTheirOwnLock.await(2, TimeUnit.SECONDS);
      assertThat(reached)
          .as("both threads must be able to hold their own session's lock at the same time")
          .isTrue();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    } finally {
      lock.unlock();
      registry.release(sessionId);
    }
  }
}
