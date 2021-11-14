package kh.advance.spring.trace.callback;

public interface TraceCallback<T> {
    T call();
}
