package priv.light.baidu;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Light
 * @date 2022/3/30 21:37
 */

@Slf4j
@Data
public class PasswordTask implements Runnable {

    private final CrackPasswordPool crackPasswordPool;
    private final AtomicReference<String> password;

    public PasswordTask(CrackPasswordPool crackPasswordPool) {
        this.crackPasswordPool = crackPasswordPool;
        this.password = new AtomicReference<>();
    }

    public static void injectCrackPasswordPool(CrackPasswordPool crackPasswordPool) {
        if (crackPasswordPool == null) {
            throw new IllegalArgumentException("CrackPasswordPool cannot be null");
        }
        PasswordTask task = new PasswordTask(crackPasswordPool);
        crackPasswordPool.setPasswordTask(task);
    }

    @Override
    public void run() {
        while (crackPasswordPool.shouldContinue()) {
            if (password.get() == null) {
                String nextPassword = crackPasswordPool.getPasswords().iterator().next();
                password.set(nextPassword);
                crackPasswordPool.getPasswords().remove(nextPassword);
            }
            crackPasswordPool.getHttpUtil().executeRequest(password.get());
        }
    }
}