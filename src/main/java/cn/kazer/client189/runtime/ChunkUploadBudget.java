package cn.kazer.client189.runtime;

/** 1.7-style frame priority adapted to 1.8's asynchronous chunk upload queue. */
public final class ChunkUploadBudget {
    private ChunkUploadBudget(){}
    public static long deadline(long now,long nativeDeadline,boolean enabled,int slowPercent) {
        if(!enabled||slowPercent<=0||nativeDeadline<=now)return nativeDeadline;
        int slow=Math.max(0,Math.min(100,slowPercent));
        // Leave urgent/nearby chunk rebuilds untouched; bound only this queued work.
        long budget=250000L+(100-slow)*17500L;
        return Math.min(nativeDeadline,now+budget);
    }
}
