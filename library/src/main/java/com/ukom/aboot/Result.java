package com.ukom.aboot;


/**
 * Created by Administrator on 2018/6/16 0013.
 *
 */
public class Result<T> {
    private final boolean result;
    private final T value;
    private final String msg;
    private final Throwable throwable;

    private Result(boolean result, T value, String msg, Throwable throwable) {
        this.result = result;
        this.value = value;
        this.msg = msg;
        this.throwable = throwable;
    }

    public boolean isSuccessful(){
        return result;
    }

    public T getValue(){
        return value;
    }

    public String getMessage(){
        return msg;
    }

    public Throwable throwable(){
        return throwable;
    }

    @Override
    public String toString() {
        return "{ result: " + result
                + (msg == null || msg == "" ? "" : ", message: " + msg)
                + (value == null ? "" : ", object type: " + value.getClass() + ", object: { " + value + " }")
                + (throwable == null ? "" : ", throwable: " + throwable)
                + " }";

    }

    public static <T> Result<T> of(boolean result, T value, String msg, Throwable throwable){
        return new Result<>(result, value, msg, throwable);
    }

    public static Result success(){
        return success(null);
    }

    public static <T> Result<T> success(T value){
        return new Result<>(true, value, null, null);
    }

    public static Result failure(String msg){
        return failure(msg, null);
    }

    public static Result failure(Throwable throwable){
        return failure("some exception happened", throwable);
    }

    public static Result failure(String msg, Throwable throwable){
        return new Result(false, null, msg, throwable);
    }

}
