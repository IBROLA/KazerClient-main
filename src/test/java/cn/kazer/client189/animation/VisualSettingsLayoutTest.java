package cn.kazer.client189.animation;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;

public class VisualSettingsLayoutTest {
    @Test public void fullLayoutContainsEverySettingExactlyOnceAndOrderedGroups(){
        VisualSettingsLayout layout=new VisualSettingsLayout(true,true);
        HashSet<String> settings=new HashSet<String>();int last=-1;
        for(VisualSettingsLayout.Row row:layout.rows){
            assertTrue(row.y>=last);last=row.y;
            if(!row.heading)assertTrue(settings.add(row.label));
            assertTrue(row.y+15<=layout.height);
        }
        assertEquals(15,settings.size());assertTrue(settings.contains("1.7 3rd Person Block Animation"));
        assertTrue(settings.contains("Punching During Usage"));assertFalse(settings.contains("Always Swing"));
        assertTrue(layout.height>200);
    }
    @Test public void collapsedGroupsOnlyExposeTheirMasterSwitch(){
        VisualSettingsLayout layout=new VisualSettingsLayout(false,false);
        assertEquals(2,layout.rows.size());assertEquals("Animations",layout.rows.get(0).label);
        assertEquals("Items",layout.rows.get(1).label);assertTrue(layout.height<60);
    }
}
