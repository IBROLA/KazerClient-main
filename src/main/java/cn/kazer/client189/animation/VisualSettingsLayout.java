package cn.kazer.client189.animation;

import java.util.ArrayList;
import java.util.List;

/** Shared geometry for drawing, hit testing and scrolling the version-only settings. */
public final class VisualSettingsLayout {
    public static final class Row {
        public final String label;
        public final int y,column;
        public final boolean heading,wide;
        Row(String label,int y,int column,boolean heading,boolean wide){
            this.label=label;this.y=y;this.column=column;this.heading=heading;this.wide=wide;
        }
    }
    public final List<Row> rows=new ArrayList<Row>();
    public int height;
    public VisualSettingsLayout(boolean animations,boolean items){
        wide("Animations");
        if(animations){
            pair("Red Armor","Remove Health Bar Flashing");
            heading("SNEAKING");pair("Smooth Sneaking","Longer Unsneak");
        }
        height+=6;wide("Items");
        if(items){
            heading("ITEM TRANSFORMS");pair("1.7 Item Positions","1.7 Block Animation");
            wide("1.7 3rd Person Block Animation");pair("1.7 Bow Pullback","1.7 Rod Position");
            pair("Consume Animation","Item Switching Animation");
            heading("BLOCK HITTING");pair("Block-Hitting Animation","Punching During Usage");
        }
    }
    private void wide(String label){rows.add(new Row(label,height,0,false,true));height+=21;}
    private void pair(String first,String second){rows.add(new Row(first,height,0,false,false));
        if(second!=null)rows.add(new Row(second,height,1,false,false));height+=21;}
    private void heading(String label){rows.add(new Row(label,height,0,true,true));height+=15;}
}
