package cn.kazer.client189;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ModPolicyTest {
    @Test public void onlyOwnedRuntimeAndOfficialOptifineAreSupported() {
        for(String id:new String[]{"minecraft","mcp","FML","Forge","kazerclient","OptiFine"})assertTrue(id,KazerClient189.allowedMod(id));
        for(String id:new String[]{"examplemod","kazer_protected_probe","kazer_owned_perf_probe","keystrokes"})assertFalse(id,KazerClient189.allowedMod(id));
    }
}
