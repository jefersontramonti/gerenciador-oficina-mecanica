package com.pitstop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Configuração do executor assíncrono para notificações.
 *
 * Otimizado para envio rápido de emails, WhatsApp e Telegram.
 *
 * @author PitStop Team
 */
@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Executor principal para tarefas assíncronas.
     * Pool de threads dedicado para notificações.
     */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool: threads sempre prontas (mínimo)
        executor.setCorePoolSize(4);

        // Max pool: threads criadas sob demanda
        executor.setMaxPoolSize(10);

        // Fila de tarefas aguardando execução
        executor.setQueueCapacity(50);

        // Tempo que threads extras ficam ociosas antes de morrer
        executor.setKeepAliveSeconds(60);

        // Nome das threads (para logs)
        executor.setThreadNamePrefix("Notificacao-");

        // Aguarda tarefas pendentes ao desligar
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("ThreadPoolTaskExecutor configurado: core={}, max={}, queue={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    /**
     * Handler para exceções não capturadas em métodos @Async.
     */
    private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Erro não capturado em método async {}.{}: {}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                ex.getMessage(),
                ex);
        }
    }
}
