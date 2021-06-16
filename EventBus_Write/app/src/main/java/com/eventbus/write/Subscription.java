package com.eventbus.write;

/**
 * Author: 信仰年轻
 * Date: 2021-06-16 16:26
 * Email: hydznsqk@163.com
 * Des: Subscription包含两个属性
 *      |-- 一个是subscriber 订阅者（反射执行对象,本例中是MainActivity.class）
 *      |-- 一个是SubscriberMethod 注解方法的所有属性参数值(threadModel线程模型 priority优先级 sticky是否黏性 eventType事件类型class method方法对象)
 */
final class Subscription {
    final Object subscriber; //反射执行对象,本例中是MainActivity.class
    final SubscriberMethod subscriberMethod;//方法上解析的所有信息包装类
    volatile boolean active;

    Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
        active = true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Subscription) {
            Subscription otherSubscription = (Subscription) other;
            return subscriber == otherSubscription.subscriber
                    && subscriberMethod.equals(otherSubscription.subscriberMethod);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode();
    }
}