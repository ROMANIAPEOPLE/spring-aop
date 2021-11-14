# spring-aop

<details>
<summary>Thread local</summary>
<div markdown="1">

스프링 프레임워크 내 Bean들은 스프링 컨테이너에 의해 싱글톤으로 관리됩니다. 정리하자면 인스턴스가 단 하나만 존재한다는 것인데 여러 쓰레드가 동시에 해당 인스턴스를 접근할 경우 동시성 이슈가 발생할 가능성이 높습니다.

또한, static 같은 공용 필드에도 위와 동일한 문제가 발생할 수 있는데 이를 해결해주기 위해 Java에는 ThreadLocal이라는 객체가 존재합니다.

#### **ThreadLocal**

- ThreadLocal은 Thread만 접근할 수 있는 특별한 저장소
- 여러 쓰레드가 접근하더라도 ThreadLocal은 Thread들을 식별해서 각각의 Thread 저장소를 구분
  - 따라서 같은 인스턴스의 ThreadLocal 필드에 여러 쓰레드가 접근하더라도 상관없음
- 대표적인 메서드는 get(), set(), 그리고 remove()가 있음
  - get() 메서드를 통해 조회
  - set() 메서드를 통해 저장
  - remove() 메서드를 통해 저장소 초기화

 

**ThreadLocal이 적용되지 않은 상태에서 동시성 문제가 발생하는 경우**

```java
@Slf4j
public class ExampleService {


    private Integer numberStorage;


    public Integer storeNumber(Integer number) {
        log.info("저장할 번호: {}, 기존에 저장된 번호: {}", number, numberStorage);
        numberStorage = number;
        sleep(1000); // 1초 대기
        log.info("저장된 번호 조회: {}", numberStorage);


        return numberStorage;
    }


    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```



 

**ExampleServiceTest.class**



```java
@Slf4j
public class ExampleServiceTest {


    private ExampleService exampleService = new ExampleService();


    @Test
    void field() {
        log.info("main start");


        Runnable storeOne = () -> {
            exampleService.storeNumber(1);
        };
        Runnable storeTwo = () -> {
            exampleService.storeNumber(2);
        };


        Thread threadA = new Thread(storeOne);
        threadA.setName("thread-1");
        Thread threadB = new Thread(storeTwo);
        threadB.setName("thread-2");


        threadA.start();
        sleep(100); // 동시성 문제 발생
        threadB.start();


        sleep(3000); // 메인 쓰레드 종료 대기


        log.info("main exit");
    }


    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```



 

\* 메인 쓰레드가 끝날 떄까지 대기하지 않을 경우 test가 조기에 종료되어 로그가 마지막까지 안 찍힐 수 있으므로 마지막에 sleep(3000);을 추가했습니다.



![img](https://blog.kakaocdn.net/dn/dBq531/btrkQkxq5HO/jWQw9n1M0mWoDXkk6lks30/img.png)



\* 위 사진처럼 동시성 문제가 발생하는 것을 확인할 수 있습니다.

\* thread-1이 2초동안 대기하는 동안 thread-2가 numberStorage에 2를 저장하여 thread-1에서도 2가 조회되는 것을 확인할 수 있습니다.

\* 위와 같은 문제를 ThreadLocal 인스턴스를 통해 해결할 수 있습니다.

 

**ThreadLocal이 적용되어 동시성 문제가 해결되는 예제**

```java
@Slf4j
public class ThreadLocalExampleService {


    private ThreadLocal<Integer> numberStorage = new ThreadLocal<>();


    public Integer storeNumber(Integer number) {
        log.info("저장할 번호: {}, 기존에 저장된 번호: {}", number, numberStorage.get());
        numberStorage.set(number);
        sleep(1000); // 1초 대기
        log.info("저장된 번호 조회: {}", numberStorage.get());


        return numberStorage.get();
    }


    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```



 

**ThreadLocalExampleServiceTest.class**



```java
@Slf4j
public class ThreadLocalExampleServiceTest {


    private ThreadLocalExampleService exampleService = new ThreadLocalExampleService();


    @Test
    void field() {
        log.info("main start");


        Runnable storeOne = () -> {
            exampleService.storeNumber(1);
        };
        Runnable storeTwo = () -> {
            exampleService.storeNumber(2);
        };


        Thread threadA = new Thread(storeOne);
        threadA.setName("thread-1");
        Thread threadB = new Thread(storeTwo);
        threadB.setName("thread-2");


        threadA.start();
        sleep(100); // 동시성 문제 발생
        threadB.start();


        sleep(3000); // 메인 쓰레드 종료 대기


        log.info("main exit");
    }


    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```


![img](https://blog.kakaocdn.net/dn/cJNsr3/btrkJ7r5QIv/prHWKo8t1Ld8RyQMstKse1/img.png)

ThreadLocal 인스턴스를 도입함으로써 동시성 이슈를 해결한 것을 확인할 수 있습니다.

####  

#### *ThreadLocal 사용시 주의점*

- ThreadLocal을 도입하면 동시성 이슈를 해결할 수 있다는 장점이 있지만 조심하지 않으면 메모리 누수를 일으켜 큰 장애를 야기할 수 있음
- 톰캣 같은 WAS의 경우 Thread를 새로 생성하는데 비용이 크기 때문에 자체적으로 ThreadPool을 가지고 있으면서 Thread를 재사용함
  - 이때 하나의 작업 요청이 들어와 Thread-1이 할당되었다가 작업을 마치고 Thread-1이 다시 ThreadPool로 반환되었다고 가정
  - 반환될 때 Thread-1 내 ThreadLocal 초기화를 하지 않을 경우 Thread-1 전용 보관소 데이터가 그대로 남아있음
  - 앞서 말한 것처럼 ThreadPool의 목적은 Thread를 새로 생성하지 않고 재활용하는 것이므로 다른 작업 요청이 들어올 때 전용 보관소가 초기화되지 않은 Thread-1이 다시 할당될 수 있음
  - 이럴 경우 클라이언트는 이전 사용자가 요청한 작업 내용을 조회하는 상황이 발생할 수도 있음 (엄청난 장애)
- 따라서, ThreadLocal은 Thread가 반환될 때 remove 메서드를 통해 반드시 초기화가 되어야 함
  - 구현한 로직의 마지막에 초기화를 진행하거나
  - WAS에 반환될 때 인터셉터 혹은 필터 단에서 초기화하는 방법으로 진행
</div>
</details>
