package com.cheatbreaker.client.kazerui.module.modules.mechanic;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.*;

/** Bounded OS work. The caller consumes completion on the client tick thread. */
final class ChatClipboard {
    private static final ThreadPoolExecutor WORKER=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(2),r->{Thread t=new Thread(r,"Kazer chat clipboard");t.setDaemon(true);return t;});
    static Future<Boolean> copy(String text) {
        return submit(()->{try{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text),null);return true;}
            catch(RuntimeException unavailable){return false;}});
    }
    static Future<Boolean> submit(Callable<Boolean> operation) {
        try{return WORKER.submit(operation);}catch(RejectedExecutionException busy){return CompletableFuture.completedFuture(false);}
    }
    private ChatClipboard(){}
}
