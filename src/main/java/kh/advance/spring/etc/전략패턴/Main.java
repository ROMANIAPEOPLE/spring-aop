package kh.advance.spring.etc.전략패턴;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {


    public static void main(String[] args) {

        case_1();
        case_2();
        case_3();

    }

    // 구현체를 직접 주입 받아서 사용
    public static void case_1() {

        Strategy strategy = new StrategyLogic1();

        ContextV1 context = new ContextV1(strategy);

        context.execute();

    }

    // 익명 내부 클래스 사용
    public static void case_2() {

        Strategy strategy = new Strategy() {
            @Override
            public void call() {
                log.info("익명 내부 클래스로 전략 패턴 사용하기");
            }
        };
        ContextV1 context = new ContextV1(strategy);
        context.execute();
    }

    // 파라미터로 주입 받기
    public static void case_3() {

        Strategy strategy = new Strategy() {
            @Override
            public void call() {
                log.info("파라미터로 전략 주입 받기");
            }
        };

        ContextV2 context = new ContextV2();

        context.execute(strategy);
    }


}
