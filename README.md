aBoot
===


aBoot 是一个轻量级的 Android 异步启动框架，适用于 APP 启动时，需要执行多个存在依赖的启动步骤的场景。

aBoot 中核心的两个类为：
- BootStep：代表一个启动步骤；
- BootTask：由多个启动步骤所构成的一个启动任务，不同的启动步骤之间可能存在依赖，BootTask 能自动按照依赖链的顺序执行启动步骤。BootTask 实现了循环依赖检测的功能。

aBoot 只负责集中管理启动步骤，以及管理步骤之间的依赖，它不会为每一个启动步骤提供线程池调度执行。

启动步骤可以选择在当前线程执行（BootTask 的工作线程，非主线程），也可以选择在其他的线程执行。如果在其他线程执行，那么启动步骤会并发执行，否则为顺序执行。

注意：  
BootStep 是一次性的，执行一次之后就会被废弃。  
BootTask 在执行完成之后，可以重复执行，但是在执行过程中，如果重复执行，会抛出异常。
BootStep 执行失败只会影响依赖它的其他BootStep，使其直接失败，而没有依赖关系的 BootStep 会继续执行。

## 用法

```Java
//创建 BootTask
BootTask bootTask = new BootTask() {
    @Override
    protected void load() {
        loadBootStep(new AStep());
        loadBootStep(new BStep());
    }
};

//如果想获取每个 BootStep 执行完成后的回调，可以调用另一个重载方法：
//bootTask.start(ComplexCallback)
bootTask.start(new BootTask.Callback() {

    @Override
    public void onTaskFinished(TaskResult result) {
        //获取 BootTask 执行的结果
        if (result.isSuccessful()){
            //获取某个启动步骤的执行结果
            Result<?> stepResult = result.getStepResult(BStep.TYPE);
            
        } else if (result.isDependenciesError()){
            //依赖链存在异常：循环依赖、依赖缺失
            TaskDependenciesException exception = 
                    result.getDependenciesException();

        } else {
            //获取执行失败的启动步骤
            List<BootStep.Type> failedSteps = result.getFailedSteps();
            for (BootStep.Type type : failedSteps){
                Result<?> stepResult = result.getStepResult(type);
                String message = stepResult.getMessage();
                
            }
        }
    }
});

//定义 BootStep 实现类：
//1. 提供一个作为BootStep 标识的 Type
//2. 实现 executeImpl() 方法，执行具体的工作
class AStep extends BootStep {
    public static final Type TYPE = typeOfName("AStep");

    AStep(){
        //定义依赖关系
        //第二个参数代表依赖的步骤是否应该在执行完成之后返回一个值
        addDependency(BStep.TYPE, true);
    }

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public void executeImpl(VariableContainer container, Callback callback) {
        executor.execute(() -> {
            //可以从container 中获取依赖的启动步骤执行完之后得到的结果
            String b = (String)container.getVariable(BStep.TYPE);

            //工作执行完成后必须回调 callback，传递执行的结果
            callback.onStepFinished(TYPE, Result.success());
        });
    }
}

static class BStep extends BootStep {
    public static final Type TYPE = typeOfName("BStep");

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public void executeImpl(VariableContainer container, Callback callback) {
        executor.execute(() -> {
            try {
                Thread.sleep(1000);

                //返回执行结果，并传递一个值
                callback.onStepFinished(TYPE, Result.success("haha"));
            } catch (InterruptedException e) {
                //返回执行结果，传递异常信息
                callback.onStepFinished(TYPE, Result.failure(e));
            }
        });
    }
}
```

## License

```
Copyright (C) 2021 UKOM

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```