package priv.light.baidu;

import lombok.NonNull;

/**
 * @author Light
 * @date 2022/4/2 9:35
 */
public class ShutDownHook extends Thread{

    private final CrackPasswordPool crackPasswordPool;

    public ShutDownHook(@NonNull CrackPasswordPool crackPasswordPool) {
        this.crackPasswordPool = crackPasswordPool;
    }

    @Override
    public void run() {
        crackPasswordPool.shutdown();
    }

}
