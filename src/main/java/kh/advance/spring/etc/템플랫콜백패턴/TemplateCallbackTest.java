package kh.advance.spring.etc.템플랫콜백패턴;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TemplateCallbackTest {

    public static void main(String[] args) {

        TimeLogTemplate timeLogTemplate = new TimeLogTemplate();

        timeLogTemplate.execute(new Callback() {
            @Override
            public void call() {
                log.info("비즈니스 로직 1 실행 - 템플릿 콜백 패턴");
            }
        });


        TimeLogTemplate timeLogTemplate2 = new TimeLogTemplate();

        timeLogTemplate.execute(() -> log.info("비즈니스 로직 2 실행"));

    }

}
