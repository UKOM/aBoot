package com.ukom.aboot;


import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/6/20 0020.
 *
 * 启动步骤
 *
 */
public abstract class BootStep {
    private static final String TAG = "BootStep";

    List<Type> stepDependencies = new ArrayList<>();
    List<Type> variableDependencies = new ArrayList<>();

    private boolean isInvalid = false;

    public abstract Type getType();

    boolean hasStepDependencies(){
        return stepDependencies != null && !stepDependencies.isEmpty();
    }

    boolean containDependency(Type type){
        return stepDependencies != null && stepDependencies.contains(type);
    }

    void stepFinished(Type type){
        if (hasStepDependencies() && containDependency(type)){
            stepDependencies.remove(type);
        }
    }

    //检查是否缺少必须的变量
    private boolean isAllVariablesExist(VariableContainer container){
        if (variableDependencies == null || variableDependencies.isEmpty()){
            return true;
        }
        for (Type type : variableDependencies){
            if (!container.isVariableExist(type)){
                Log.e(TAG, "isAllVariablesExist: variable -- " + type + " not exists");
                return false;
            }
        }

        return true;
    }

    void execute(VariableContainer container, Callback callback){
        synchronized (this){
            if (isInvalid) {
                callback.onStepFinished(getType(), Result.failure("This BootStep has been executed, should create a new one!"));
                return;
            }
            isInvalid = true;
        }

        if (hasStepDependencies()){
            //存在未完成的依赖步骤
            Log.e(TAG, "execute: " + getType() + " has unfinished dependent step: " + stepDependencies);
            callback.onStepFinished(getType(), Result.failure(getType() + " has unfinished dependent step: " + stepDependencies));
        } else if (!isAllVariablesExist(container)){
            //缺乏依赖的变量
            Log.e(TAG, "execute: " + getType() + " lack of variable: " + variableDependencies);
            callback.onStepFinished(getType(), Result.failure(getType() + " lack of variable: " + variableDependencies));
        } else {
            executeImpl(container, callback);
        }
    }

    //由方法实现者决定在当前线程，还是子线程执行
    public abstract void executeImpl(@NonNull VariableContainer container, Callback callback);

    protected void addDependency(@NonNull Type type, boolean needVariable){
        stepDependencies.add(type);
        if (needVariable) variableDependencies.add(type);
    }

    @Override
    public String toString() {
        return getType().getName();
    }

    public interface Callback{
        void onStepFinished(Type type, Result result);
    }

    //启动步骤类型
    public static class Type {
        private String name;
        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    protected static Type typeOfName(String name){
        return new Type(name);
    }

}
