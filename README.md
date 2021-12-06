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
  


<details>
<summary>템플릿 메소드 </summary>
<div markdown="1">

# 📒 템플릿 메소드 패턴

- 좋은 설계는 변하는 것과 변하지 않는 것을 분리하는 것이다.
- 변하지 않는 것은 추상클래스의 메서드로 선언, 변하는 부분은 추상 메서드로 선언하여 자식 클래스가 오버라이딩 하도록 처리한다.
- 이렇듯이 특정 작업을 처리하는 일부분을 서브 클래스로 캡슐화하여 전체적인 구조는 바꾸지 않으면서 특정 단계에서 수행하는 내용을 바꾸는 패턴이다.

가장 큰 장점은 전체적으로는 동일하면서 부분적으로는 다른 구문으로 구성된 메서드의 **코드 중복을 최소화**시킬 수있는 점이다.

##### 템플릿 메서드 패턴의 목적은 다음과 같다.

"작업에서 알고리즘의 골격을 정의하고 일부 단계를 하위 클래스로 연기한다. 템플릿 메서드를 사용하면 하위 클래스가 알고리즘의 구조를 변경하지 않고도 알고리즘의 특정 단게를 재정의할 수 있다."

즉, 부모 클래스에 알고리즘의 골격인 **템플릿** 을 정의하고 일부 변경되는 로직은 자식 클래스에 정의하는 것이다. 이렇게하면 자식 클래스가 알고리즘의 전체 구조를 변경하지 않고 특정 부분만 재정의할 수 있다. 결국 상속과 오버라이딩을 통한 다형성으로 문제를 해결하는 것이다. (변하는 부분을 추상 메소드로 설계)


# 📒 사용 예시

### 📌 추상 클래스

```java
public abstract class AbstractTemplate {
    
    public void execute() {
	System.out.println("템플릿 시작");
	//변해야 하는 로직 시작
	logic();
	//변해야 하는 로직 시작
	System.out.println("템플릿 종료");
    }
    
    protected abstract void logic(); //변경 가능성이 있는 부분은 추상 메소드로 선언한다.
}
```

- 추상 클래스에서 변경 가능성이 있는 부분은 추상 메소드로 작성한다.



### 📌 실제 구현 클래스

```java
public class SubClassLogic1 extends AbstractTemplate {
    @Override
    protected void call() {
	System.out.println("변해야 하는 메서드는 이렇게 오버라이딩으로 사용1.");
    }
}
public class SubClassLogic2 extends AbstractTemplate {
    @Override
    protected void call() {
    	System.out.println("변해야 하는 메서드는 이렇게 오버라이딩으로 사용2.");
    }
}
```

- 추상클래스를 extends하여 변해야 하는 메소드를 Override한다.



### 📌 사용

```java
public class templateMethod1 extends AbstractTemplate {
    public static void main(String[] args) {
	AbstractTemplate template1 = new SubClassLogic1();
	template1.execute();
    
    	System.out.println();
    
	AbstractTemplate template2 = new SubClassLogic2();
	template2.execute();
    }
}


//출력
템플릿 시작
변해야 하는 메서드는 이렇게 오버라이딩으로 사용1
템플릿 종료
    
템플릿 시작
변해야 하는 메서드는 이렇게 오버라이딩으로 사용2
템플릿 종료
```

- 객체 생성 시 어느 구현체를 사용하는지에 따라서 변하는 부분의 메소드가 바뀌게 된다.

- 이로써 코드 중복을 최대한 피하면서 변해야 하는 부분은 구현체 사용에 따라서 유동적으로 바꿀 수 있다.

  

  

## 📒 익명 내부 클래스를 사용

- `SubClassLogic1`, `SubClassLogic2`처럼 구현 클래스를 계속 만들어야 하는 단점이 있다.
- **해결 방법**: 익명 내부 클래스를 사용
- 익명 내부 클래스를 사용하면 객체 인스턴스를 생성하면서 동시에 생성할 클래스를 상속 받은 자식 클래스를 정의할 수 있다.

```java
public class templateMethod1 extends AbstractTemplate {
    public static void main(String[] args) {
	AbstractTemplate template1 = new AbstractTemplate() {
		@Override
		protected void call() {
			System.out.println("변해야 하는 메서드를 이렇게 익명 내부 클래스로 구현할 수 있다1");
		}
	};
	template1.execute();
    
	AbstractTemplate template2 = new AbstractTemplate() {
		@Override
		protected void call() {
			System.out.println("변해야 하는 메서드를 이렇게 익명 내부 클래스로 구현할 수 있다2");
		}
	};
	template2.execute();
    }
}

//출력
템플릿 시작
변해야 하는 메서드를 이렇게 익명 내부 클래스로 구현할 수 있다1
템플릿 종료
    
템플릿 시작
변해야 하는 메서드를 이렇게 익명 내부 클래스로 구현할 수 있다2
템플릿 종료
```



## 단점

템플릿 메서드 패턴은 상속을 사용한다. 따라서 상속에서 오는 단점들을 그대로 안고간다. 특히 자식 클래스가 부모 클래스와 컴파일 시점에 강하게 결합되는 문제가 있다. 이것은 의존관계에 대한 문제이다. 자식 클래스 입장에서는 부모 클래스의 기능을 전혀 사용하지 않는다.

상속을 받는 다는 것은 특정 부모 클래스를 의존하고 있다는 것이다. 자식 클래스의 extends 다음에 바로 부모 클래스가 코드상에 지정되어 있다. 따라서 부모 클래스의 기능을 사용하든 사용하지 않든 간에 부모 클래스를 강하게 의존하게 된다. 여기서 강하게 의존한다는 뜻은 자식 클래스의 코드에 부모 클래스의 코드가 명확하게 적혀 있다는 뜻이다. UML에서 상속을 받으면 삼각형 화살표가 자식 -> 부모 를 향하고 있는 것은 이런 의존관계를 반영하는 것이다.

자식 클래스 입장에서는 부모 클래스의 기능을 전혀 사용하지 않는데, 부모 클래스를 알아야한다. 이것은 좋은 설계가 아니다. 그리고 이런 잘못된 의존관계 때문에 부모 클래스를 수정하면, 자식 클래스에도 영향을 줄 수 있다.

추가로 템플릿 메서드 패턴은 상속 구조를 사용하기 때문에, 별도의 클래스나 익명 내부 클래스를 만들어야 하는 부분도 복잡하다.
 지금까지 설명한 이런 부분들을 더 깔끔하게 개선하려면 어떻게 해야할까?

템플릿 메서드 패턴과 비슷한 역할을 하면서 상속의 단점을 제거할 수 있는 디자인 패턴이 바로 전략 패턴 (Strategy Pattern)이다.
</div>
</details>

<details>
<summary>전략 패턴</summary>
<div markdown="1">
전략 패턴(strategy pattern)


![img](https://images.velog.io/images/pbg0205/post/74c157b1-7d99-4e8a-908c-f03840250231/image.png)

### 1. 전략 패턴과 템플릿 메서드 패턴을 사용하는 이유?

전략 패턴을 사용하는 이유는 **`템플릿(context) 안에 로직을(strategy) 유연하게 변경`**하기 위해서 사용한다. 또한 변하는 부분의 책임을 구분하여 SRP를 만족할 수 있어 좋은 설계를 할 수 있다. 여기서 말하는 **`템플릿은 변경하지 않는 부분, 로직은 변하는 부분`**을 말한다. 이와 유사한 디자인 패턴으로 템플릿 메서드 패턴이 있다. 그렇다면 템플릿 메서드 패턴 대신 전략 패턴을 사용하는 이유는 뭘까?



### 2. 템플릿 메서드 패턴의 단점

**`템플릿 메서드 패턴`**은 변경하는 부분은 자식 클래스에서 재정의하는 방법이다. 템플릿 메서드 패턴과 전략 패턴의 가장 큰 차이는 변하는 부분의 로직의 부모를 **`클래스(abstract)를 사용하는지, 인터페이스(interface)를 사용하는지 차이`**다.
하지만 부모의 구현의 파급 효과는 생각보다 크다. 흔히 알고 있는 상속의 단점인 **`부모 자식 간의 강한 결합`**과 **`원치 않는 데이터 또는 함수의 상속이라는 점`**이다. 프로그래밍 설계 원칙의 기본은 약한 의존성과 강한 결합도이다. 비록 상속 또한 기능 확장이라는 강한 장점이지만 상위 클래스의 데이터와 함수에 대한 변경이 있을 경우, 하위 클래스의 변경 연쇄적으로 이루어질 수 있다. 하나의 변경이 다른 변경을 연쇄적으로 발생한다는 것은 좋은 방법은 아니다.



### 3. 전략 패턴의 장점

이를 해결하기 위해서 도입된 방법이 **`전략 패턴`**이다. 앞서 이야기 했듯이, 변하는 부분의 부모를 인터페이스(interface)로 선언하고 하위 클래스에서 위임하는 방식이다. 이 방식의 장점은 상속에 비해 약간 의존성을 갖는다. 이전의 상속의 경우는 부모가 변경이 일어나면 자식에도 영향이 있을 수 있지만 인터페이스로 구현할 경우, 이 부분을 신경쓰지 않아도 되는 장점이 있다.

또한 **`익명 클래스와 람다식을 사용하기 편리하다.`** 인터페이스에 메서드가 하나만 존재할 경우, 자바에서 메서드를 추론할 수 있기 때문에 람다식을 사용할 수 있다. 람다식을 사용할 경우, 코드가 간결해지는 장점이 있고 개인적으로는 코드가 익명 클래스보다 직관적이어서 선호하는 방식이다.



### 4. 전략 패턴 사용 방식 : 필드 vs 메서드 파라미터

```java
interface Movable {
    void move();
}

public class Walk implments Movable {
    public void move() {
    	System.out.println("걸어간다.");
    }
}

public class Fly implements Movable {
    public void move() {
    	System.out.println("날아한다.");
    }
}

class Robot {
    //파라미터로 받는 방법
    public move(Movable movable) {
    	System.out.println("움직임 시작");
    	movable.move();
        System.out.println("움직임 끝");
    }
}
```

이전에는 전략 패턴을 필드로 선언하고 setter를 사용했지만 강의에서는 파라미터로 받아서 사용했다. 두 방법 모두 동적으로 전략을 사용할 수 있지만 setter 사용은 지양한다. setter를 사용할 경우, 변경하는 코드가 생겨 로직이 흩어지는 단점이 존재한다. 나중에 누군가가 setter로 로직을 변경하고 내가 모르고 그 코드를 맡아서 사용할 경우, 디버깅을 해서 원인을 찾아야 하는 어려움이 발생한다. 그렇기 때문에 **`setter를 지양하고 대신에 별도의 Context를 추가로 생성`**하도록 하자.

하지만 이러면 Context의 중복이 일어나기 때문에 **`오히려 파라미터로 받아서 사용하는 방식이 적합`**한 것 같다. 해당 메서드를 사용할 때만 전략(strategy)를 추가하는 방식이 동적으로 로직을 처리하는 것이 더욱 유연하게 중복 코드없이 처리할 수 있는 방법이라고 생각한다.

---



### 템플릿 메서드 패턴 vs 전략 패턴

탬플릿 메서드 패턴은 부모 클래스에 변하지 않는 템플릿을 두고, 변하는 부분을 자식 클래스에 두어서 상속을 사용해서 문제를 해결했다. 전략 패턴은 변하지 않는 부분을 Context 라는 곳에 두고, 변하는 부분을 Strategy 라는 인터페이스를 만들고 해당 인터페이스를 구현하도록 해서 문제를 해결한다. 상속이 아니라 위임으로 문제를 해결하는 것이다.

전략 패턴에서 Context 는 변하지 않는 템플릿 역할을 하고, Strategy 는 변하는 알고리즘 역할을 한다.



#### Strategy 인터페이스

```java
public interface Strategy {
      void call();
}
```



#### Context 클래스

```java
public class ContextV1 {
    private Strategy strategy;

    public ContextV1(Strategy strategy) {
        this.strategy = strategy;
    }

    public void execute() {
        long startTime = System.currentTimeMillis(); 

        //비즈니스 로직 실행
        strategy.call(); //위임
        //비즈니스 로직 종료
        
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        log.info("resultTime={}", resultTime);
    }
}
```

ContextV1 은 변하지 않는 로직을 가지고 있는 템플릿 역할을 하는 코드이다. 전략 패턴에서는 이것을 컨텍스트(문맥)이라 한다.
 쉽게 이야기해서 컨텍스트(문맥)는 크게 변하지 않지만, 그 문맥 속에서 strategy 를 통해 일부 전략이 변경된다 생각하면 된다.

Context 는 내부에 Strategy strategy 필드를 가지고 있다. 이 필드에 변하는 부분인 Strategy 의 구현체를 주입하면 된다.
 전략 패턴의 핵심은 Context 는 Strategy 인터페이스에만 의존한다는 점이다. 덕분에 Strategy 의 구현체를 변경하거나 새로 만들어도 Context 코드에는 영향을 주지 않는다.

즉, 스프링의 의존관계 주입과 같이  ContextV1 에 Strategy 의 구현체인 strategyLogic1 를 주입하는 것을 확인할 수 있다. 이렇게해서 Context 안에 원하는 전략을 주입한다. 이렇게 원하는 모양으로 조립을 완료하고 난 다음에 context1.execute() 를 호출해서 context 를 실행한다.

	![스크린샷 2021-12-06 오전 9 04 52](https://user-images.githubusercontent.com/39195377/144769596-94d449f1-c2d3-49fd-80bb-dc9314fc722e.png)


1. Context 에 원하는 Strategy 구현체를 주입한다.

2. 클라이언트는 context 를 실행한다.

3. context 는 context 로직을 시작한다.해

4. context 로직 중간에 strategy.call() 을 호출해서 주입 받은 strategy 로직을 실행한다.

5. context 는 나머지 로직을 실행한다.



### 정리

변하지 않는 부분은 Context에 두고 변하는 부분을 Strategy를 구현해서 만든다. 스프링으로 애플리케이션을 개발할 때 애플리케이션 로딩 시점에 의존관계 주입을 통해 필요한 의존관계를 모두 맺어두고 난 다음에 실제 요청을 처리하는 것 과 같은 원리이다.
</div>
</details>
  
