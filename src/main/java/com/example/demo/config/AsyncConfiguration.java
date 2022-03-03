package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    // 单核CPU处理CPU密集型程序，这种情况并不太适合使用多线程
    // 如果是多核CPU 处理 CPU 密集型程序，我们完全可以最大化的利用 CPU 核心数，应用并发编程来提高效率
    // I/O密集型程序 线程等待时间所占比例越高，需要越多线程；线程CPU时间所占比例越高，需要越少线程
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：线程池创建时候初始化的线程数
        // CPU密集型:  线程数量 = CPU逻辑核数 + 1
        // I/O密集型程序: 线程数量 = (1/CPU利用率) = 1 + (I/O耗时/CPU耗时)
        int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;
        executor.setCorePoolSize(corePoolSize);
        // 最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        // 缓冲队列：用来缓冲执行任务的队列
        executor.setQueueCapacity(Integer.MAX_VALUE);
        // 允许线程的空闲时间60秒：当超过了核心线程之外的线程在空闲时间到达之后会被销毁
        executor.setKeepAliveSeconds(60);
        // 线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        executor.setThreadNamePrefix("My ThreadPoolTaskExecutor- ");
        // 缓冲队列满了之后的拒绝策略：由调用线程处理（一般是主线程）
//        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        // 当最大池已满时，此策略保证不会丢失任务请求，但是可能会影响应用程序整体性能
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 设置线程池中任务的等待时间，如果超过这个时候还没有销毁就强制销毁，以确保应用最后能够被关闭，而不是阻塞住
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

}

