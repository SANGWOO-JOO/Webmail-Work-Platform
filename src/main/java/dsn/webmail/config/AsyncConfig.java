package dsn.webmail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 *
 * 메일 폴링을 병렬로 처리하기 위한 스레드 풀 설정
 *
 * 설정 가이드:
 * - corePoolSize: 기본 스레드 수 (항상 유지되는 스레드)
 * - maxPoolSize: 최대 스레드 수 (부하 시 증가)
 * - queueCapacity: 대기 큐 크기 (모든 스레드가 바쁠 때 대기)
 */
@Configuration
@EnableAsync  // @Async 어노테이션 활성화
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 메일 폴링 전용 스레드 풀
     */
    @Bean(name = "mailPollingExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수: 동시 활성 사용자 수 고려 (예: 10명)
        executor.setCorePoolSize(10);

        // 최대 스레드 수: 피크 타임 대비 (평소의 2배)
        executor.setMaxPoolSize(20);

        // 대기 큐 크기: 급격한 부하 시 버퍼
        executor.setQueueCapacity(50);

        // 스레드 이름 접두사 (로그에서 식별용)
        // 예: mail-poll-1, mail-poll-2, ...
        executor.setThreadNamePrefix("mail-poll-");

        // 애플리케이션 종료 시 현재 작업 완료까지 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);  // 최대 60초 대기

        executor.initialize();

        logger.info("Mail polling thread pool initialized: core={}, max={}, queue={}",
                   executor.getCorePoolSize(),
                   executor.getMaxPoolSize(),
                   executor.getQueueCapacity());

        return executor;
    }

    /**
     * 비동기 예외 핸들러
     *
     * @Async 메서드에서 발생한 예외를 처리합니다.
     * 예외가 발생해도 다른 사용자 처리는 계속됩니다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            logger.error("=== Async Execution Error ===");
            logger.error("Method: {}", method.getName());
            logger.error("Parameters: {}", params);
            logger.error("Exception: ", ex);
        };
    }
}
