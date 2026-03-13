package net.studio1122.Algorithm;

import net.studio1122.Backend;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round Robin 전략.
 * AtomicInteger로 요청마다 인덱스를 하나씩 올려 순환 선택한다.
 *
 * getAndIncrement()는 현재 값을 읽고 +1 하는 동작을 원자적으로 수행하므로
 * 여러 스레드가 동시에 호출해도 각자 고유한 값을 가져간다.
 */
public class RoundRobin implements BalanceStrategy {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Optional<Backend> select(List<Backend> backends) {
        List<Backend> healthy = backends.stream()
                .filter(Backend::isHealthy)
                .toList();

        if (healthy.isEmpty()) return Optional.empty();

        // counter가 int 최댓값을 넘어 음수가 될 수 있으므로 Math.abs 처리
        int idx = Math.abs(counter.getAndIncrement() % healthy.size());
        return Optional.of(healthy.get(idx));
    }
}