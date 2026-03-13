package net.studio1122.Algorithm;

import net.studio1122.Backend;

import java.util.List;
import java.util.Optional;

/**
 * 백엔드 선택 전략 인터페이스.
 * 전달받는 List는 전체 백엔드 목록(unhealthy 포함)이며,
 * 각 구현체가 isHealthy() 필터링을 직접 담당한다.
 */
public interface BalanceStrategy {
    Optional<Backend> select(List<Backend> backends);
}