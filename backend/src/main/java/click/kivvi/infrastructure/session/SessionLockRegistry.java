package click.kivvi.infrastructure.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * A bounded, leak-free registry of one {@link ReentrantLock} per session id.
 *
 * <p>Two calls to {@link #acquire(String)} with the same id always return the same {@link
 * ReentrantLock} instance, so callers holding it serialize against each other; two different ids
 * always get independent locks. Each returned lock is reference-counted: the entry for an id is
 * removed from the underlying map the moment its count drops to zero, so a session that stops being
 * contended leaves nothing behind — the registry never grows without bound as sessions come and go.
 * Callers must pair every {@link #acquire(String)} with exactly one {@link #release(String)}, in a
 * {@code finally} block, regardless of whether the lock itself was ever actually obtained via
 * {@link ReentrantLock#tryLock()}.
 *
 * <p>{@link ConcurrentHashMap#compute}/{@link ConcurrentHashMap#computeIfPresent} guarantee that
 * all mutations of a single key's entry (create-and-increment, decrement-and-maybe-remove) are
 * mutually exclusive with each other, so a concurrent {@link #acquire(String)} can never observe an
 * entry that a concurrent {@link #release(String)} is in the middle of evicting, or vice versa.
 */
@Component
class SessionLockRegistry {

  private final ConcurrentHashMap<String, Entry> entriesBySessionId = new ConcurrentHashMap<>();

  ReentrantLock acquire(String sessionId) {
    Entry entry =
        entriesBySessionId.compute(
            sessionId,
            (id, existing) -> {
              Entry target = existing != null ? existing : new Entry();
              target.holders.incrementAndGet();
              return target;
            });
    return entry.lock;
  }

  void release(String sessionId) {
    entriesBySessionId.computeIfPresent(
        sessionId, (id, entry) -> entry.holders.decrementAndGet() == 0 ? null : entry);
  }

  /** Test-only: the number of sessions currently tracked, to assert eviction actually happens. */
  int trackedSessionCount() {
    return entriesBySessionId.size();
  }

  private static final class Entry {
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger holders = new AtomicInteger(0);
  }
}
