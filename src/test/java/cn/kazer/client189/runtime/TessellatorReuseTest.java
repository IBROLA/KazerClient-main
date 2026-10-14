package cn.kazer.client189.runtime;
import cn.kazer.client189.compat.Tessellator;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.Assert.*;

public class TessellatorReuseTest {
    @Test public void pooledVerticesRemainDistinctAndAreReusedWithoutExtraEntries() throws Exception {
        Tessellator t=new Tessellator();
        Field store=Tessellator.class.getDeclaredField("vertices");store.setAccessible(true);
        Field drawing=Tessellator.class.getDeclaredField("drawing");drawing.setAccessible(true);
        t.startDrawingQuads();
        for(int i=0;i<4;i++)t.addVertexWithUV(i,i+1,0,i/4D,0);
        List<?> first=(List<?>)store.get(t);
        assertEquals(4,first.size());
        Object[] saved=first.toArray();
        for(int i=0;i<4;i++)for(int j=i+1;j<4;j++)assertNotSame(saved[i],saved[j]);
        Field x=saved[0].getClass().getDeclaredField("x");x.setAccessible(true);
        for(int i=0;i<4;i++)assertEquals(i,x.getDouble(saved[i]),0D);
        drawing.setBoolean(t,false); // Avoid creating an OpenGL context in the unit test.
        t.startDrawingQuads();
        for(int i=0;i<4;i++)t.addVertex(i+10,i+20,0);
        List<?> reused=(List<?>)store.get(t);
        assertEquals(4,reused.size());
        for(int i=0;i<4;i++){
            assertSame(saved[i],reused.get(i));assertEquals(i+10,x.getDouble(reused.get(i)),0D);
        }
    }
}
