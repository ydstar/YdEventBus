package com.eventbus.write;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: 信仰年轻
 * Date: 2021-06-16 15:19
 * Email: hydznsqk@163.com
 * Des: 先明确几个概念
 *      1.  subscriptionsByEventType 是map集合集合(重要)
 *          |-- key: Event函数的参数的类(本例中是String.class)
 *          |-- value: Subscription 的集合列表
 *                      |-- Subscription包含两个属性
 *                          |-- 一个是subscriber 订阅者（反射执行对象,本例中是MainActivity.class）
 *                          |-- 一个是SubscriberMethod 注解方法的所有属性参数值(threadModel线程模型 priority优先级 sticky是否黏性 eventType事件类型class method方法对象)
 *
 *      2.  typesBySubscriber 是map集合(在unregister()中使用)
 *          |-- key: 所有的订阅者(本例中是MainActivity.class)
 *          |-- value: 所有订阅者里面方法的参数的class() (本例中是 String.class 的集合)
 *          目的是在unregister()解绑中通过key(MainActivity.class)找到value(String.class),
 *          然后在通过value(String.class)找到subscriptionsByEventType中的value()Subscription 的集合列表,
 *          然后就可以进行移除操作了
 */
public class EventBus {


    // subscriptionsByEventType 这个集合存放的是？
    // key 是 Event 参数的类(本例中是String.class)
    // value 存放的是 Subscription 的集合列表
    // Subscription 包含两个属性，一个是 subscriber 订阅者（反射执行对象,本例中是MainActivity.class），一个是 SubscriberMethod 注解方法的所有属性参数值
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;

    // typesBySubscriber 这个集合存放的是？
    // key 是所有的订阅者(MainActivity.class)
    // value 是所有订阅者里面方法的参数的class() (本例中就是 String.class 的集合)
    private final Map<Object, List<Class<?>>> typesBySubscriber;


    private EventBus() {
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
    }

    private volatile static EventBus INSTANCE = null;

    public static EventBus getDefault() {
        if (INSTANCE == null) {
            synchronized (EventBus.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EventBus();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 注册
     * 思路:
     * 第一步:
     * 1.先拿到object的字节码class对象
     * 2.获取到该class对象的所有标记@Subscribe注解的函数
     * 3.解析被注解标记的函数成SubscriberMethod对象(threadModel线程模型 priority优先级 sticky是否黏性 eventType事件类型class method方法对象)
     * 4.然后存到subscriberMethods集合中
     * 第二步:
     * 1.遍历subscriberMethods的List集合
     * 2.按照规则存放到subscriptionsByEventType的map集合中(key:String.class  value:CopyOnWriteArrayList<Subscription>)
     * 第三步:
     * 1.typesBySubscriber的map集合也要存一份(key:MainActivity.class value:List<Class<?> String.class 的集合)
     */
    public void register(Object object) {
        //第一步
        ArrayList<SubscriberMethod> subscriberMethods = new ArrayList<>();
        //1.先拿到object的字节码class对象
        Class<?> objectClass = object.getClass();
        Method[] declaredMethods = objectClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            //2.获取到该class对象的所有标记@Subscribe注解的函数
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if(annotation!=null){
                // 所有的Subscribe属性 解析出来
                Class<?>[] parameterTypes = method.getParameterTypes();
                //3.解析被注解标记的函数成SubscriberMethod对象(threadModel线程模型 priority优先级 sticky是否黏性 eventType事件类型class method方法对象)
                SubscriberMethod model = new SubscriberMethod(method, parameterTypes[0], annotation.threadMode(), annotation.priority(), annotation.sticky());
                //4.然后存到subscriberMethods集合中
                subscriberMethods.add(model);
            }
        }
        //第二步
        //1.遍历subscriberMethods的List集合
        for (SubscriberMethod method : subscriberMethods) {
            subscribe(object,method);
        }
    }

    private void subscribe(Object object, SubscriberMethod method) {
        //2.按照规则存放到subscriptionsByEventType的map集合中(key:String.class  value:CopyOnWriteArrayList<Subscription>)
        Class<?> eventType = method.eventType;
        //先从subscriptionsByEventType 的map中取一下List,如果为空则创建一个,然后在添加
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if(subscriptions==null){
            subscriptions=new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType,subscriptions);
        }
        Subscription subscription = new Subscription(object, method);
        subscriptions.add(subscription);

        //第三步:
        //1.typesBySubscriber的map集合也要存一份(key:MainActivity.class value:List<Class<?> String.class 的集合)
        List<Class<?>> eventTypeList = typesBySubscriber.get(object);
        if(eventTypeList==null){
            eventTypeList=new ArrayList<>();
            typesBySubscriber.put(object,eventTypeList);
        }
        if(!eventTypeList.contains(eventType)){
            eventTypeList.add(eventType);
        }
    }


    /**
     * 解除注册
     * 思路:
     * 1.通过typesBySubscriber这个map得到所有的String.class集合
     * 2.遍历这个String.class集合
     * 3.通过subscriptionsByEventType这个map获取到CopyOnWriteArrayList<Subscription>
     * 4.遍历CopyOnWriteArrayList集合
     * 5.得到Subscription,然后subscription.subscriber==object 进行比较,如果相同则移除
     */
    public void unregister(Object object) {
        //1.通过typesBySubscriber这个map得到所有的String.class集合
        List<Class<?>> eventTypList = typesBySubscriber.get(object);
        if(eventTypList!=null){
            //2.遍历这个String.class集合
            for (Class<?> eventType : eventTypList) {
                removeObject(eventType,object);
            }
        }
    }

    private void removeObject(Class<?> eventType, Object object) {
        //3.通过subscriptionsByEventType这个map获取到CopyOnWriteArrayList<Subscription>
        CopyOnWriteArrayList<Subscription> subscriptionList = subscriptionsByEventType.get(eventType);
        if(subscriptionList!=null){
            int size = subscriptionList.size();
            //4.遍历CopyOnWriteArrayList集合,然后挨个移除
            for(int x=0;x<size;x++){
                //5.得到Subscription,然后subscription.subscriber==object 进行比较,如果相同则移除
                Subscription subscription = subscriptionList.get(x);
                if(subscription.subscriber==object){
                    subscriptionList.remove(x);
                    x--;
                    size--;
                }
            }
        }
    }

    /**
     * 发送事件
     * 思路:
     * 1.先获取event的字节码对象,这其实就是eventType
     * 2.从subscriptionsByEventType中根据eventType得到Subscription 的集合列表
     * 3.遍历这个集合
     * 4.找到符合的方法调用方法的 method.invoke() 执行,要注意线程切换
     */
    public void post(Object event) {
        //1.先获取event的字节码对象,这其实就是eventType
        Class<?> evenType = event.getClass();
        //2.从subscriptionsByEventType中根据eventType得到Subscription 的集合列表
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(evenType);
        if(subscriptions!=null){
            //3.遍历这个集合
            for (Subscription subscription : subscriptions) {
                executeMethod(subscription,event);
            }
        }
    }

    //4.找到符合的方法调用方法的 method.invoke() 执行,要注意线程切换
    private void executeMethod(final Subscription subscription, final Object event) {
        SubscriberMethod subscriberMethod = subscription.subscriberMethod;

        //是否在主线程
        boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
        switch (subscriberMethod.threadMode){
            //同一个线程，在哪个线程发送事件，那么该方法就在哪个线程执行
            case POSTING:
                invokeMethod(subscription,event);
                break;

                // 在主线程中执行
            case MAIN:
                if(isMainThread){
                    invokeMethod(subscription,event);
                }else{
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            invokeMethod(subscription,event);
                        }
                    });
                }
                break;

                //子线程：如果发布事件的线程是主线程，那么调用线程池中的子线程来执行订阅方法；否则直接执行；
            case BACKGROUND:
                if(isMainThread){
                    AsyncPoster.enqueue(subscription,event);
                }else{
                    invokeMethod(subscription,event);
                }
                break;

                //异步线程：无论发布事件执行在主线程还是子线程，都利用一个异步线程来执行订阅方法。
            case ASYNC:
                AsyncPoster.enqueue(subscription,event);
                break;

            default:
                break;
        }

    }

    /**
     * 真正的反射调用方法
     * @param subscription
     * @param event
     */
    private void invokeMethod(Subscription subscription, Object event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber,event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
