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
<summary>템플릿 메소드 패턴</summary>
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


![스크린샷 2021-12-06 오전 9 05 32](https://user-images.githubusercontent.com/39195377/144769619-1ed10ea7-0c8a-4c85-ba58-c531f6cdbdbc.png)


1. Context 에 원하는 Strategy 구현체를 주입한다.

2. 클라이언트는 context 를 실행한다.

3. context 는 context 로직을 시작한다.해

4. context 로직 중간에 strategy.call() 을 호출해서 주입 받은 strategy 로직을 실행한다.

5. context 는 나머지 로직을 실행한다.



### 정리

변하지 않는 부분은 Context에 두고 변하는 부분을 Strategy를 구현해서 만든다. 스프링으로 애플리케이션을 개발할 때 애플리케이션 로딩 시점에 의존관계 주입을 통해 필요한 의존관계를 모두 맺어두고 난 다음에 실제 요청을 처리하는 것 과 같은 원리이다.
</div>
</details>
  

<details>
<summary>빈 후처리기</summary>
<div markdown="1">	
## 1. 프록시 팩토리

---

- JDK 동적 프록시
  - 인터페이스 기반
- CGLIB
  - 구체 클래스 기반

JDK 동적 프록시와 CGLIB에서 기반으로 하는 대상이 다르기 때문에 같은 기능을 제공하기 위해서는 JDK 동적 프록시가 제공하는 InvocationHandler와 CGLIB가 제공하는 MethodInterceptor로 따로 만들고 중복으로 관리해야하는 문제가 있습니다.  
스프링에서 제공하는 프록시 팩토리는 이 문제를 해결해줍니다.

![그림1](https://backtony.github.io/assets/img/post/spring/aop/1-1.PNG)  
프록시 팩토리는 인터페이스가 있으면 JDK 동적 프록시를 사용하고, 클래스만 있다면 CGLIB을 사용합니다.(설정 변경 가능)  
프록시 팩토리가 조건에 맞게 선택을 해준다고 하더라도 동작을 정의하는 클래스가 InvocationHandler와 MethodInterceptor로 서로 다르기 때문에 각각 구현해야하는 문제가 있습니다.

![그림2](https://backtony.github.io/assets/img/post/spring/aop/1-2.PNG)  
스프링은 이 문제를 해결하기 위해 부가 기능을 적용할 때, **Advice** 라는 개념을 도입하였습니다.  
프록시 팩토리는 Advice를 호출하는 전용 InvocationHandler와 MethodInterceptor를 내부에서 사용하여 Advice를 호출하도록 구성되어 있습니다.  
따라서, 개발자는 각각을 구현할 필요없이 Advice만 만들어주면 됩니다.

Advice를 구현하기 위해서는 MethodInterceptor 인터페이스를 구현하면 됩니다.

```
package org.aopalliance.intercept;
public interface MethodInterceptor extends Interceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
```

- MethodInterceptor
  - CGLIB의 프록시 기능 정의할 때 사용하는 와 이름이 동일하지만 패키지 명이 다릅니다.
  - MethodInterceptor는 Interceptor를 상속하고 Interceptor는 Advice 인터페이스를 상속합니다.
- MethodInvocation invocation
  - 내부에는 다음 메서드를 호출하는 방법, 현재 프록시 객체 인스턴스, args, 메서드 정보등이 포함되어 있습니다.
  - 기존에 InvocationHandler와 MethodInterceptor를 구성할 때 제공되었던 파라미터들이 invocation안으로 다 들어갔다고 보면 됩니다.

#### 예시

간단하게 실행 시간을 찍는 프록시를 만들겠습니다.

```
@Slf4j
public class TimeAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        log.info("TimeProxy 실행");
        long startTime = System.currentTimeMillis();

        // 실제 로직 실행
        Object result = invocation.proceed();

        long endTime = System.currentTimeMillis();
        log.info("result Time = {}",endTime-startTime);

        return result;
    }
}
```

- invocation.proceed()
  - 실제 target 클래스의 대상 메서드를 호출하고 그 결과를 받습니다.
  - JDK 동적 프록시와 CGLIB를 사용할 때는, 인자로 target과 args를 넣어줘야 했는데 이는 프록시 팩토리에서 프록시를 생성하는 단계에서 전달받기 때문에 invocation이 이미 갖고 있습니다.

**인터페이스 기반**

```
@Slf4j
public class ProxyFactoryTest {

    @Test
    @DisplayName("인터페이스가 있으면 JDK 동적 프록시 사용")
    void interfaceProxy() {
        ServiceInterface target = new ServiceImpl();

        // 프록시 팩토리 생성시, 프록시의 호출 대상을 인자로 넘긴다.
        ProxyFactory proxyFactory = new ProxyFactory(target);

        // 실행 동작 정의한 Advice 주입
        proxyFactory.addAdvice(new TimeAdvice());
        ServiceInterface proxy = (ServiceInterface) proxyFactory.getProxy();

        // 실행
        proxy.save();

        assertThat(AopUtils.isAopProxy(proxy)).isTrue();
        assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
        assertThat(AopUtils.isCglibProxy(proxy)).isFalse();
    }
}
```

- new ProxyFactory(target);
  - 프록시 팩토리 생성시, 프록시의 호출 대상을 인자로 넘깁니다.
  - 이때 인자로 넘기는 인스턴스에 인터페이스가 있다면 프록시를 만들 때 JDK 동적프록시를, 없다면 CGLIB을 통해 프록시를 생성합니다.
  - 위에서는 인터페이스를 넘겼지만, GCLIB을 사용하고 싶다면 인터페이스가 없는 단순 클래스를 넘기면 됩니다.
- addAdvice
  - 프록시 팩토리를 통해서 만든 프록시가 사용할 부가 기능 로직을 세팅합니다.
- proxyFactory.getProxy()
  - 프록시 팩토리에서 프록시 객체를 생성하고 그 결과를 받습니다.

**인터페이스가 있어도 GCLIB 사용하기**

```
@Slf4j
public class ProxyFactoryTest {
    @Test
    @DisplayName("ProxyTargetClass 옵션을 사용하면 인터페이스가 있어도 CGLIB를 사용하고, 클래스 기반 프록시 사용")
    void proxyTargetClass() {
        ServiceInterface target = new ServiceImpl();
        ProxyFactory proxyFactory = new ProxyFactory(target);

        // TargetClass -> 구체 클래스사용하기 -> CGLIB 사용
        proxyFactory.setProxyTargetClass(true);

        proxyFactory.addAdvice(new TimeAdvice());
        ServiceInterface proxy = (ServiceInterface) proxyFactory.getProxy();

        proxy.save();

        assertThat(AopUtils.isAopProxy(proxy)).isTrue();
        assertThat(AopUtils.isJdkDynamicProxy(proxy)).isFalse();
        assertThat(AopUtils.isCglibProxy(proxy)).isTrue();
    }
}
```

- setProxyTargetClass
  - 해당 옵션을 true로 넘기게 되면 인터페이스가 있어도 구체 클래스를 기반으로 CGLIB을 통해 동적 프록시를 생성합니다.

### 포인트컷, 어드바이스, 어드바이저

---

![그림3](https://backtony.github.io/assets/img/post/spring/aop/1-3.PNG)

- 포인트컷(Pointcut)
  - 어디에는 적용하고 어디에는 적용하지 않을지 판단하는 필터링 기능을 합니다.
  - 주로 클래스와 메서드 이름으로 필터링 합니다.
- 어드바이스(Advice)
  - 프록시가 호출하는 부가 기능으로 단순하게 프록시가 수행하는 로직이라고 생각하면 됩니다.
- 어드바이저(Advisor)
  - 포인트컷 1개 + 어드바이스 1개의 쌍을 의미합니다.
  - 어디에 어떤 로직을 적용할지 알고 있는 것을 의미합니다.

#### 예시

![그림4](https://backtony.github.io/assets/img/post/spring/aop/1-4.PNG)  
위와 같은 형태로 간단하게 포인트컷, 어드바이스, 어드바이저를 만들어보겠습니다.

```
public class MultiAdvisorTest {

    @Test
    @DisplayName("하나의 프록시, 여러 어드바이저")
    void multiAdvisorTest2() {
        //client -> proxy -> advisor2 -> advisor1 -> target

        DefaultPointcutAdvisor advisor1 = new DefaultPointcutAdvisor(Pointcut.TRUE, new Advice1());
        DefaultPointcutAdvisor advisor2 = new DefaultPointcutAdvisor(Pointcut.TRUE, new Advice2());

        //프록시 팩토리 생성
        ServiceInterface target = new ServiceImpl();
        ProxyFactory proxyFactory = new ProxyFactory(target);

        // 어드바이저 등록
        proxyFactory.addAdvisor(advisor2);
        proxyFactory.addAdvisor(advisor1);

        // 프록시 생성
        ServiceInterface proxy = (ServiceInterface) proxyFactory.getProxy();

        //실행
        proxy.save();
    }

    @Slf4j
    static class Advice1 implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            log.info("advice1 호출");
            return invocation.proceed();
        }
    }

    @Slf4j
    static class Advice2 implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            log.info("advice2 호출");
            return invocation.proceed();
        }
    }
}
```

간단한 Advice 클래스 2개를 만들고 DefaultPointcutAdvisor를 이용해 Advice와 Pointcut을 인자로 넘겨 Advisor 2개를 만들었습니다.  
프록시 팩토리를 만들고 Advisor 2개를 등록하고 프록시를 만들었습니다.

- DefaultPointcutAdvisor
  - Advisor 인터페이스의 가장 일반적인 구현체로 생성자를 통해 한개의 포인트컷과 한개의 어드바이스를 넣어주면 됩니다.
- Pointcut.TRUE
  - 항상 True를 반환하는 포인트 컷입니다.
- proxyFactory.addAdvisor
  - 프록시 팩토리에 적용할 어드바이저를 저장합니다.
  - 등록한 순서대로 어드바이저가 적용됩니다.

스프링은 AOP를 적용할 때, 최적화를 진행해서 위의 예시처럼 프록시는 하나만 만들고, 하나의 프록시에 여러 Advisor를 적용하게 됩니다.  
즉, **스프링 AOP는 target마다 단 한개의 프록시만 생성합니다.**

참고로 스프링은 무수히 많은 포인트컷을 제공합니다.

- NameMatchMethodPointcut
  - 메서드 이름 기반 매칭
  - PatternMatchUtils 사용
- JdkRegexpMethodPointcut : JDK 정규 표현식 기반 매칭
- TruePointcut : 항상 참
- AnnotationMatchingPointcut : 애노테이션 매칭
- AspectJExpressionPointcut : aspectJ 표현식 매칭

무수히 많은 포인트컷을 제공하지만 가장 중요한 것은 aspectJ이고 거의 aspectJ만 사용하게 됩니다.

### 한계

---

**프록시 팩토리 덕분에 인터페이스 기반, 구체 클래스 기반을 구분하지 않고 프록시를 간편하게 생성할 수 있었습니다.**  
추가로 어드바이저, 어드바이스, 포인트컷 개념으로 **어떤 부가기능** 을 **어디에 적용할지** 명확하게 분리해서 사용할 수 있었습니다.  
**하지만 프록시 팩토리를 만들기 위해 너무 많은 설정을 해야 합니다.**  
만약 스프링 빈이 100개가 있고 여기에 프록시를 등록해 부가 기능을 부여한다고 한다면 100개의 동적 프록시 생성 코드를 만들어 프록시를 반환하도록 해야 합니다.  
이렇게 빈이 100개가 등록되어 있다면 결국 하고자 한다면 할 수는 있지만, 만약 **해당 빈들이 컴포넌트 스캔으로 올라간 경우 위 방법으로는 중간에 끼어들 수가 없기 때문에 프록시 적용이 불가능합니다.**  
이에 대한 해결책은 **빈 후처리기** 입니다.

## 2. 빈 후처리기 

---

![그림5](https://backtony.github.io/assets/img/post/spring/aop/1-5.PNG)  
빈 후처리기는 **스프링이 빈 저장소에 등록할 목적으로 생성한 객체를 빈 저장소에 등록하기 직전에 조작할 때** 사용됩니다.  
동작 과정은 다음과 같습니다.

1. 스프링 빈 대상이 되는 객체를 생성한다.(@Bean, 콤포넌트 스캔 대상)
2. 생성된 객체를 빈 저장소에 등록하기 직전에 빈 후처리기에 전달한다.
3. 빈 후처리기는 전달된 스프링 빈 객체를 조작하거나 다른 객체로 바꿔치기할 수 있다.
4. 빈 후처리기는 객체를 빈 저장소에 반환하고 해당 빈은 빈 저장소에 등록된다.

빈 후처리기에서 바꿔치기 하는 작업에서 프록시를 생성해서 프록시를 반환하게 되면 빈 저장소에는 프록시가 빈으로 등록되게 됩니다.

빈 후처리기를 구현하기 위해서는 BeanPostProcessor 인터페이스를 구현하고 스프링 빈으로 등록하면 됩니다.

```
public interface BeanPostProcessor {
    Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
}
```

- postProcessBeforeInitialization
  - 객체 생성 이후에 @PostConstruct같은 초기화 작업 전에 호출되는 포스트 프로세서
- postProcessAfterInitialization
  - 객체 생성 이후에 @PostConstruct같은 초기화 작업 후에 호출되는 포스트 프로세서

BeanPostProcessor 인터페이스를 직접 구현해서 빈으로 등록해도 되지만, 스프링에서는 더 편리한 방식을 제공합니다.

### 스프링이 제공하는 빈 후처리기

스프링에서 제공하는 빈 후처리기를 사용하기 위해서는 의존성을 추가해야 합니다.

```
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

라이브러리를 추가하면 aspectjweaver 라는 aspectJ 관련 라이브러리를 등록하고, 스프링 부트가 AOP 관련 클래스를 자동으로 스프링 빈에 등록합니다.  
이때 AnnotationAwareAspectJAutoProxyCreator라는 빈 후처리기가 스프링 빈에 자동으로 등록되는데, **프록시를 생성해주는 빈 후처리기** 입니다.  
이 빈 후처리기는 **스프링 빈으로 등록된 Advisor들을 자동으로 찾아서 프록시가 필요한 곳에 자동으로 프록시를 적용하여 프록시를 반환합니다.**  
Advisor만으로 프록시가 필요한 곳을 찾고 적용할수 있는 이유는 Advisor 안에는 Pointcut과 Advice가 이미 포함되어 있어 Pointcut으로 프록시를 적용할지 여부를 판단하고, Advice로 부가 기능을 적용할 수 있습니다.  
참고로 AnnotationAwareAspectJAutoProxyCreator는 @AspectJ와 관련된 AOP 기능도 찾아서 자동으로 처리하고, @Aspect도 자동으로 인식해서 프록시를 만들고 AOP를 적용합니다.

### 동작 과정

![그림6](https://backtony.github.io/assets/img/post/spring/aop/1-6.PNG)

1. 스프링 빈 대상이 되는 객체를 생성한다.(@Bean, 콤포넌트 스캔 대상)
2. 생성된 객체를 빈 저장소에 등록하기 직전에 빈 후처리기에 전달한다.
3. 모든 Advisor 빈을 조회하고 Pointcut을 통해 클래스와 메서드 정보를 매칭해보면서 프록시를 적용할 대상인지 판단합니다.
4. 모든 Advisor 중 하나의 조건에만 만족한다면 프록시를 생성하고 프록시를 빈 저장소로 반환합니다.
5. 만약 프록시 생성 대상이 아니라면 들어온 빈 그대로 빈 저장소로 반환합니다.
6. 빈 저장소는 객체를 받아서 빈으로 등록합니다.

![그림7](https://backtony.github.io/assets/img/post/spring/aop/1-7.PNG)  
여기서 주의할 점은 여러 Advisor의 대상이 된다고 하더라도 **프록시는 1개만** 만들고 그 안에 Advisor을 여러개 담게 된다는 것입니다.

### 예시

```
@Configuration
public class AutoProxyConfig {
    @Bean
    public Advisor advisor(LogTrace logTrace) {
        //pointcut
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* hello.proxy.app..*(..)) && !execution(* hello.proxy.app..noLog(..))");

        //advice 
        LogTraceAdvice advice = new LogTraceAdvice(logTrace);

        // Advisor 반환하여 빈 등록
        return new DefaultPointcutAdvisor(pointcut, advice);
    }
}
```

- AspectJExpressionPointcut
  - AspectJ 포인트컷 표현식
  - - : 모든 반환 타입
  - hello.proxy.app.. : 해당 패키지와 그 하위 패키지
  - *(..)
    - - : 모든 메서드 이름
    - (..) : 파라미터는 무관

AspectJExpressionPointcut을 통해 pointcut을 정의하고 advice 하나를 만들고 DefaultPointcutAdvisor에 인자로 넘겨서 advisor를 만들어 빈으로 등록한 코드입니다.

## 정리

---

- 프록시 팩토리
  - 프록시를 적용하기 위해서는 JDK 동적 프록시, CGLIB 기능이 존재하지만 JDK는 인터페이스 기반, CGLIB는 구체 클래스 기반이기 때문에 공통 로직이라도 두개를 만들어 중복코드를 관리해야 하는 문제점이 있다.
  - **프록시 팩토리는 포인트컷, 어드바이스, 어드바이저 개념을 도입해 JDK 동적 프록시와 CGLIB을 구분하지 않고 사용하는 기능을 제공함으로써 기반이 다른 경우 2개로 관리해야했던 중복 코드를 없애주었다.**
  - **하지만 프록시 팩토리를 사용하기 위해서 빈 등록시 실제 프록시를 반환하는 코드를 직접 작성하는 등 너무 많은 설정이 필요하고, 컴포넌트 스캔 대상이 되는 빈 객체들은 사용할 수 없다.**
- 빈 후처리기
  - **스프링이 빈 저장소에 등록할 목적으로 생성한 객체를 빈 저장소에 등록하기 직전에 조작하는 기능을 제공하여 프록시 팩토리의 문제점을 해결한다.**
  - 스프링이 제공하는 빈 후처리기를 사용하면 **Advisor만 빈으로 등록하면 알아서 대상에 대해 프록시를 생성한다.**
  - 스프링 빈이 되는 대상이 생성될 때, 빈으로 등록되어 있는 모든 Advisor를 조회하여 pointcut을 통해 대상 여부를 판단하고 맞다면 프록시를 반환하여 빈 저장소에서 프록시를 빈으로 등록하게 된다.
  - 여러 Advisor의 대상이 되는 경우, **하나의 프록시** 안에 여러 개의 Advisor가 들어간다.
  - **Pointcut은 2가지에 사용된다.**
    - **빈이 생성되는 단계에서 프록시 적용 여부를 판단할 때**
      - pointcut을 통해 매칭되는 클래스와 해당 클래스 안에 여러 메서드 중 매칭되는 것이 하나라도 있다면 해당 클래스 혹은 인터페이스를 프록시로 생성된다.
    - **해당 빈이 실제 사용되는 시점에 부가 기능의 적용 대상인지 판단할 때**
      - 클래스 혹은 인터페이스가 프록시로 들어와있는데 해당 클래스 혹은 인터페이스 안에 모든 메서드가 프록시 적용 대상은 아니기 때문에 Pointcut으로 한번 더 판단을 해야 한다.

</div>
</details>
