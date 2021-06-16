package com.eventbus.write;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: 信仰年轻
 * Date: 2021-06-16 16:26
 * Email: hydznsqk@163.com
 * Des: 线程池的使用
 */
public class AsyncPoster implements Runnable {
    final Subscription subscription;
    final Object event;


    private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    public AsyncPoster(Subscription subscription, Object event) {
        this.subscription = subscription;
        this.event = event;
    }

    public static void enqueue(final Subscription subscription, final Object event) {
        AsyncPoster asyncPoster = new AsyncPoster(subscription, event);
        // 用线程池
        EXECUTOR_SERVICE.execute(asyncPoster);

    }

    @Override
    public void run() {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
