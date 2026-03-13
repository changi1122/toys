package net.studio1122;

import net.studio1122.Algorithm.BalanceStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 백엔드 서버 목록을 관리하고, 전략(BalanceStrategy)에 따라 다음 백엔드를 선택한다.
 *
 * CopyOnWriteArrayList: 읽기(연결마다 발생)는 락 없이 수행되고,
 * 쓰기(헬스 체크로 목록 변경)는 내부적으로 배열을 복사하여 처리한다.
 * 이 프로젝트에서는 startup 이후 목록 자체가 바뀌지 않으므로 사실상 읽기 전용이지만,
 * 다수 스레드가 읽는 상황임을 명시적으로 표현하기 위해 사용한다.
 */
public class BackendPool {
    private final CopyOnWriteArrayList<Backend> backends;
    private final BalanceStrategy strategy;

    public BackendPool(List<Backend> backends, BalanceStrategy strategy) {
        this.backends = new CopyOnWriteArrayList<>(backends);
        this.strategy = strategy;
    }

    /**
     * 전략에 따라 다음 백엔드를 선택한다.
     * 모든 백엔드가 unhealthy면 Optional.empty()를 반환한다.
     */
    public Optional<Backend> next() {
        return strategy.select(backends);
    }

    /** HealthChecker가 전체 목록을 순회할 때 사용 */
    public List<Backend> getAll() {
        return Collections.unmodifiableList(backends);
    }
}