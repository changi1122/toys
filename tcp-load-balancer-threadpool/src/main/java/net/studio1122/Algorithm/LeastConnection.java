package net.studio1122.Algorithm;

import net.studio1122.Backend;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Least Connection 전략.
 * 현재 활성 연결 수가 가장 적은 healthy 백엔드를 선택한다.
 *
 * Stream.min()은 이미 Optional을 반환하므로 별도 처리 없이 그대로 반환한다.
 *
 * 참고: 여러 스레드가 동시에 select()를 호출하면 각 백엔드의 activeConnections를
 * 읽는 시점이 미세하게 다를 수 있다 (non-linearizable). 단, AtomicInteger 덕분에
 * 개별 읽기는 최신 값이 보장되며, 토이 프로젝트 수준에서는 충분히 정확하다.
 */
public class LeastConnection implements BalanceStrategy {

    @Override
    public Optional<Backend> select(List<Backend> backends) {
        return backends.stream()
                .filter(Backend::isHealthy)
                .min(Comparator.comparingInt(Backend::getActiveConnections));
    }
}
