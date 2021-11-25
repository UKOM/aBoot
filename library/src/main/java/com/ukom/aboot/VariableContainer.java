package com.ukom.aboot;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2018/6/20 0020.
 *
 */
public class VariableContainer {
    private static final String TAG = "VariableContainer";

    private Map<BootStep.Type, Object> container = new ConcurrentHashMap<>();

    public boolean isVariableExist(BootStep.Type key){
        return container.containsKey(key);
    }

    public void putVariable(@NonNull BootStep.Type key, Object object){
        if (object != null) container.put(key, object);
    }

    public @Nullable Object getVariable(@NonNull BootStep.Type key){
        return container.get(key);
    }

    public void clear(){
        container.clear();
    }
}
