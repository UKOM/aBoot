package com.ukom.aboot;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskResult {

    private boolean result;
    private TaskDependenciesException dependenciesException;

    private final Map<BootStep.Type, Result> stepResultMap = new HashMap<>();

    TaskResult(){ }

    TaskResult(@NonNull TaskDependenciesException exception){
        this.result = false;
        this.dependenciesException = exception;
    }

    public Result<?> getStepResult(@NonNull BootStep.Type type){
        return stepResultMap.get(type);
    }

    public boolean isSuccessful(){
        return result;
    }

    public boolean isDependenciesError() {
        return dependenciesException != null;
    }

    public List<BootStep.Type> getFailedSteps(){
        return getSteps(false);
    }

    public List<BootStep.Type> getSuccessfulSteps(){
        return getSteps(true);
    }

    private List<BootStep.Type> getSteps(boolean result){
        List<BootStep.Type> list = new ArrayList<>();
        for (BootStep.Type type : stepResultMap.keySet()){
            Result stepResult = stepResultMap.get(type);
            if (stepResult.isSuccessful() == result){
                list.add(type);
            }
        }
        return list;
    }

    void setResult(boolean result){
        this.result = result;
    }

    void setStepResult(@NonNull BootStep.Type type, @NonNull Result<?> result){
        stepResultMap.put(type, result);
    }

    public TaskDependenciesException getDependenciesException() {
        return dependenciesException;
    }

    @Override
    public String toString() {
        return "TaskResult { result: " + result
                + (dependenciesException == null ? "" : ", dependenciesException: " + dependenciesException)
                +  ", stepResultMap: " + stepResultMap
                + '}';
    }
}
