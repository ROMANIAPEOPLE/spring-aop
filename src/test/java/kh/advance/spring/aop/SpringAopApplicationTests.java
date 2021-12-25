package kh.advance.spring.aop;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;

@SpringBootTest
@Slf4j
class SpringAopApplicationTests {

    @Test
    void reflection() throws Exception {

        Class classHello = Class.forName("kh.advance.spring.aop.SpringAopApplicationTests$Hello");
        Hello target = new Hello();

        Method methodCallA = classHello.getMethod("callA");
        Object result1 = methodCallA.invoke(target);
        log.info("result1 = {}", result1);

        Method methodCallB = classHello.getMethod("callB");
        Object result2 = methodCallB.invoke(target);
        log.info("result = {}", result2);

    }

    @Test
    void reflection_test() {
        Hello target = new Hello();

        log.info("start");
        String result1 = target.callA();
        log.info("result={}" , result1);

        log.info("start");
        String result2 = target.callB();
        log.info("result={}", result2);

    }

    @Slf4j
    static class Hello {
        public String callA() {
            log.info("callA");
            return "A";
        }

        public String callB() {
            log.info("callB");
            return "B";
        }
    }
}
