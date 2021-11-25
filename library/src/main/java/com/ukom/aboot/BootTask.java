package com.ukom.aboot;


import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 2018/6/20 0020.
 *
 * 一次启动任务(BootTask)由一系列启动步骤(BootStep)构成
 *
 * 注：不支持重复的同类型启动步骤
 *
 */
public abstract class BootTask implements BootStep.Callback {
    private static final String TAG = "BootTask";

    private final VariableContainer container;
    private final Map<BootStep.Type, BootStep> stepMap;

    private final List<BootStep.Type> executingSteps;
    private final List<BootStep.Type> failedSteps;

    private ComplexCallback callback;

    private TaskResult taskResult;

    private volatile HandlerThreadExecutor executor;

    public BootTask(){
        executingSteps = new ArrayList<>();
        failedSteps = new ArrayList<>();
        container = new VariableContainer();
        stepMap = new HashMap<>();
    }

    //装载 BootStep
    protected abstract void load();

    protected void loadBootStep(BootStep step){
        stepMap.put(step.getType(), step);
    }

    public void start(Callback callback){
        start(new CallbackWrapper(callback));
    }

    public void start(final ComplexCallback callback){
        synchronized (this){
            if (executor != null) throw new IllegalStateException("The task is executing");
            executor = new HandlerThreadExecutor();
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!executingSteps.isEmpty()){
                    Log.w(TAG, "start: executingSteps is not empty, did last execution finish normally ?");
                    executingSteps.clear();
                }

                if (!stepMap.isEmpty()){
                    Log.w(TAG, "start: stepMap is not empty, last execution unfinished ?");
                    stepMap.clear();
                }

                load();
                if (stepMap.isEmpty()){
                    Log.w(TAG, "start: did not load any BootStep !");
                    TaskResult taskResult = new TaskResult();
                    taskResult.setResult(true);
                    callback.onTaskFinished(taskResult);
                    return;
                }

                //检查依赖链是否完备，是否存在循环依赖
                Result result = checkDependencyChains();
                if (!result.isSuccessful()){
                    Log.w(TAG, "start: checkDependencyChains failed: " + result.getMessage());
                    callback.onTaskFinished(
                            new TaskResult(new TaskDependenciesException(result.getMessage())));
                    return;
                }

                taskResult = new TaskResult();
                BootTask.this.callback = callback;
                executeStepsWithoutDependency();
            }
        });
    }

    //检查依赖链是否完备，是否存在循环依赖
    private Result checkDependencyChains(){
        LinkedList<BootStep> list = new LinkedList<>(stepMap.values());
        Log.d(TAG, "checkDependencyChains: " + list);
        BootStep cursor;
        Set<BootStep.Type> chain = new HashSet<>();
        Set<BootStep> checked = new LinkedHashSet<>();
        while ((cursor = list.poll()) != null){
            if (!checked.contains(cursor)){
                chain.add(cursor.getType());
                Result result = checkDependencyChains(cursor, chain, checked);
                if (!result.isSuccessful()) return result;
                chain.clear();
            }
        }
        return Result.success();
    }

    private Result checkDependencyChains(BootStep cursor,
                                         Set<BootStep.Type> chain, Set<BootStep> checked){
        if (!cursor.hasStepDependencies()) {
            checked.add(cursor);
            Log.d(TAG, "checkDependencyChains: check over a chain: " + chain);
            return Result.success();
        }

        List<BootStep.Type> stepDependencies = cursor.stepDependencies;
        for (BootStep.Type type: stepDependencies){
            //缺少依赖
            if (!stepMap.containsKey(type))
                return Result.failure(String.format("lack of dependency[%s] by type[%s]", type, cursor.getType()));

            //存在循环依赖
            if (chain.contains(type))
                return Result.failure("circular dependencies, chain: " + chain + ", tail: " + type);

            BootStep step = stepMap.get(type);
            Set<BootStep.Type> newChain = new LinkedHashSet<>(chain);
            newChain.add(type);

            Result result = checkDependencyChains(step, newChain, checked);
            if (!result.isSuccessful()) return result;
        }

        checked.add(cursor);
        return Result.success();
    }

    private void executeStepsWithoutDependency(){
        for (Map.Entry<BootStep.Type, BootStep> entry : stepMap.entrySet()) {
            BootStep step = entry.getValue();
            if (!executingSteps.contains(step.getType()) && !step.hasStepDependencies()){
                executingSteps.add(step.getType());
                Log.d(TAG, "executeStepsWithoutDependency: " + step);
                step.execute(container, this);
            }
        }
    }

    @Override
    public void onStepFinished(BootStep.Type type, Result result) {
        //避免同一个Step 有多个依赖同时完成，造成同步安全问题，所有回调在同一个线程处理
        executor.execute(new Runnable() {
            @Override
            public void run() {
                taskResult.setStepResult(type, result);
                callback.onStepFinished(type, result);
                handleStepResult(type, result);
            }
        });
    }

    private void handleStepResult(BootStep.Type type, Result result){
        Log.d(TAG, "handleStepResult: type: " + type + ", result: " + result);
        if (result.isSuccessful()){
            //将产生的变量存入到container
            container.putVariable(type, result.getValue());
        } else {
            failedSteps.add(type);
        }

        if (stepMap.containsKey(type)){
            stepMap.remove(type);
            if (stepMap.isEmpty()){
                //所有步骤都完成
                taskFinished();
                return;
            }
        } else {
            Log.w(TAG, "handleStepResult: not contain this step, may has already been removed");
        }

        Iterator<Map.Entry<BootStep.Type, BootStep>> iterator = stepMap.entrySet().iterator();
        BootStep step;
        while (iterator.hasNext()){
            step = iterator.next().getValue();
            if (step.containDependency(type)){
                if (result.isSuccessful()){
                    step.stepFinished(type);
                } else {
                    Log.i(TAG, "handleStepResult: directly failed step: " + step.getType());
                    onStepFinished(step.getType(),
                            Result.failure("依赖的步骤 [ " + type + " ] 失败"));
                }
            }
        }

        executeStepsWithoutDependency();
    }

    private void taskFinished(){
        Log.d(TAG, "taskFinished: failedSteps: " + failedSteps);

        taskResult.setResult(failedSteps.isEmpty());
        callback.onTaskFinished(taskResult);
        taskResult = null;

        //清空状态
        failedSteps.clear();
        executingSteps.clear();
        container.clear();

        executor.release();
        executor = null;
    }

    public interface Callback {
        void onTaskFinished(TaskResult result);
    }

    public interface ComplexCallback extends Callback, BootStep.Callback { }

    private static class CallbackWrapper implements ComplexCallback {
        private Callback callback;

        CallbackWrapper(@NonNull Callback c){
            this.callback = c;
        }

        @Override
        public void onTaskFinished(TaskResult result) {
            callback.onTaskFinished(result);
        }

        @Override
        public void onStepFinished(BootStep.Type type, Result result) { }
    }

}
