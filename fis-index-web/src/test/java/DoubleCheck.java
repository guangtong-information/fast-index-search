
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程锁的应用
 * 叙述：double check 模式，防并发被动缓存
 * 作者:Winlone
 */
public class DoubleCheck {
    private static String data = "";
    private Lock lock = new ReentrantLock();// 锁对象


    /**
     * double check: 处理并发的线程锁
     *
     * @return
     */
    public String doSomeThing(String name) {
        System.out.println("1、进入时间:" + Comm.getData() + ",线程执行：" + name);
        String returnData = "";

        try {
            //第一次check:判断缓存是否为空，为空则进行缓存加载
            if (data == "") {
                //线程锁，锁住1秒
                if (lock.tryLock(3, TimeUnit.SECONDS)) {
                    System.out.println("2、" + name + "获取锁，获取锁的时间:" + Comm.getData());

                    //第二次check:判断缓存是否为空，为空则进行缓存加载
                    if (data == "") {
                        int sec = 2000;
                        //读取数据 -> 制作缓存 耗费的时间
                        Thread.sleep(sec);
                        System.out.println("3、" + name + "制作缓存，当前时间:" + Comm.getData() + ",制作缓存时间" + sec + "ms完毕");
                        returnData = name + "做的缓存-数据库读取完成";
                        //赋值缓存
                        data = returnData;

                        lock.unlock();
                        System.out.println("4、解锁的线程：" + name + ",解锁时间:" + Comm.getData());
                    } else {
                        //使用缓存
                        returnData = data;
                        System.out.println("3、" + name + "读取到有缓存,当前时间:" + Comm.getData());
                    }
                } else {
                    //使用缓存
                    returnData = data;
                    System.out.println("3、" + name + "锁超时,存在缓存时间:" + Comm.getData());
                }
            } else {
                //使用缓存
                returnData = data;
                System.out.println("3、" + name + "读取到有缓存,获取缓存时间:" + Comm.getData());
            }

        } catch (Exception e) {
            System.out.println("error：" + name + "线程错误：" + e.getMessage());
        } finally {

        }
        System.out.println("end：" + name + "线程结束，离开时间:" + Comm.getData());
        return returnData;
    }

    @Test
    public void getTest() throws Exception {
        //线程1
        Thread a1 = new Thread() {
            public void run() {
                String cache = doSomeThing("线程1");
                System.out.println("线程1读取缓存数据为：" + cache);
            }
        };
        a1.setName("a1:" + System.currentTimeMillis());
        a1.start();


        //线程2
        Thread a2 = new Thread() {
            public void run() {
                String cache = doSomeThing("线程2");
                System.out.println("线程2读取缓存数据为：" + cache);
            }
        };
        a2.setName("a2:" + System.currentTimeMillis());
        a2.start();


        Thread.sleep(3000);

        //线程3
        Thread a3 = new Thread() {
            public void run() {
                String cache = doSomeThing("线程3");
                System.out.println("线程3读取缓存数据为：" + cache);
            }
        };
        a3.setName("a3:" + System.currentTimeMillis());
        a3.start();


//        a1.join();
//        a2.join();
//        a3.join();
        //永久等待主线程
        try {
            Thread.sleep(50000000);
        } catch (Exception e) {
        } finally {
        }
    }

}