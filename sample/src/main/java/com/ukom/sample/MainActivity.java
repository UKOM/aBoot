package com.ukom.sample;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.ukom.aboot.BootStep;
import com.ukom.aboot.BootTask;
import com.ukom.aboot.TaskDependenciesException;
import com.ukom.aboot.TaskResult;
import com.ukom.aboot.VariableContainer;
import com.ukom.aboot.Result;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static Executor executor = Executors.newFixedThreadPool(4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "onCreate: ");

        BootTask bootTask = new BootTask() {
            @Override
            protected void load() {
                loadBootStep(new AStep());
                loadBootStep(new BStep());
                loadBootStep(new CStep());
                loadBootStep(new DStep());
            }
        };

        //如果想获取每个 BootStep 执行完成后的回调，可以调用另一个重载方法 bootTask.start(ComplexCallback)
        bootTask.start(new BootTask.Callback() {

            @Override
            public void onTaskFinished(TaskResult result) {
                Log.i(TAG, "onTaskFinished: " + result);

                if (result.isSuccessful()){
                    Result<?> stepResult = result.getStepResult(BStep.TYPE);

                } else if (result.isDependenciesError()){
                    //依赖链存在异常：循环依赖、依赖缺失
                    TaskDependenciesException exception = result.getDependenciesException();
                } else {
                    List<BootStep.Type> failedSteps = result.getFailedSteps();
                    for (BootStep.Type type : failedSteps){
                        Result<?> stepResult = result.getStepResult(type);
                        String message = stepResult.getMessage();

                    }
                }
            }
        });
    }

    static class AStep extends BootStep {
        public static final Type TYPE = typeOfName("AStep");

        AStep(){
            addDependency(BStep.TYPE, true);
            addDependency(CStep.TYPE, true);
        }

        @Override
        public Type getType() {
            return TYPE;
        }

        @Override
        public void executeImpl(VariableContainer container, Callback callback) {
            executor.execute(() -> {
                String b = (String)container.getVariable(BStep.TYPE);
                Object c = container.getVariable(CStep.TYPE);
                Log.i(TAG, "AStep, executeImpl: b: " + b + ", c: " + c);
                callback.onStepFinished(TYPE, Result.success());
            });
        }
    }

    static class BStep extends BootStep {
        public static final Type TYPE = typeOfName("BStep");

        BStep(){
            addDependency(DStep.TYPE, false);
        }

        @Override
        public Type getType() {
            return TYPE;
        }

        @Override
        public void executeImpl(VariableContainer container, Callback callback) {
            executor.execute(() -> {
                try {
                    Log.i(TAG, "BStep, executeImpl: ");
                    Thread.sleep(1000);
                    Result<String> result = Result.success("haha");
                    callback.onStepFinished(TYPE, result);
                } catch (InterruptedException e) {
                    callback.onStepFinished(TYPE, Result.failure(e));
                }
            });
        }
    }

    static class CStep extends BootStep {
        public static final Type TYPE = typeOfName("CStep");

        @Override
        public Type getType() {
            return TYPE;
        }

        @Override
        public void executeImpl(VariableContainer container, Callback callback) {
            executor.execute(() -> {
                try {
                    Log.i(TAG, "CStep, executeImpl: ");
                    Thread.sleep(5000);
                    callback.onStepFinished(TYPE, Result.success(17378L));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            });
        }
    }

    static class DStep extends BootStep {
        public static final Type TYPE = typeOfName("DStep");

        @Override
        public Type getType() {
            return TYPE;
        }

        @Override
        public void executeImpl(VariableContainer container, Callback callback) {
            executor.execute(() -> {
                Log.i(TAG, "DStep, executeImpl: ");
                callback.onStepFinished(TYPE, Result.success());
            });
        }
    }

}