package cn.kazer.client189.animation;

import com.cheatbreaker.client.config.Setting;
import com.cheatbreaker.client.module.AbstractModule;
import cn.kazer.client189.config.KazerConfigDocument;
import cn.kazer.client189.config.KazerConfigPaths;

/** Sk1er OldAnimations options, persisted only in the version-specific document. */
public final class BlockHitModule extends AbstractModule {
    public final Setting animations,items,healthFlash,smoothSneaking,longSneaking,redArmor;
    public final Setting blockHitting,itemTransforms,thirdPerson,swordBlock,bows,rods,consuming,punching,itemSwitch;
    private boolean loading;
    public BlockHitModule() {
        super("1.7 Visuals");setDefaultState(true);setDefaultRenderHud(false);isEditable=false;
        animations=toggle("Animations",null);
        smoothSneaking=toggle("Smooth Sneaking",animations);longSneaking=toggle("Longer Unsneak",animations);
        redArmor=toggle("Red Armor",animations);healthFlash=toggle("Remove Health Bar Flashing",animations);
        items=toggle("Items",null);
        itemTransforms=toggle("1.7 Item Positions",items);bows=toggle("1.7 Bow Pullback",items);
        swordBlock=toggle("1.7 Block Animation",items);rods=toggle("1.7 Rod Position",items);
        thirdPerson=toggle("1.7 3rd Person Block Animation",items);consuming=toggle("Consume Animation",items);
        blockHitting=toggle("Block-Hitting Animation",items);punching=toggle("Punching During Usage",items);
        itemSwitch=toggle("Item Switching Animation",items);
        AnimationPreferences.update(this);
    }
    private Setting toggle(String label,Setting parent) {
        Setting setting=new Setting(this,label).setValue(true).onChange(v->save());
        return parent==null?setting:setting.setParent(parent);
    }
    public boolean flag(Setting value){return isEnabled()&&value.isVisible()&&Boolean.TRUE.equals(value.getValue());}
    @Override public void setState(boolean value) { super.setState(value); BlockHitAnimation.setEnabled(value); save(); }
    public static String previousLabel(String label) {
        switch(label) {
            case "1.7 Item Positions": return "Item Transforms";
            case "1.7 Bow Pullback": return "Bows";
            case "1.7 Block Animation": return "Swords";
            case "1.7 Rod Position": return "Fishing Rods";
            case "1.7 3rd Person Block Animation": return "Third Person Held Items";
            case "Consume Animation": return "Food";
            case "Block-Hitting Animation": return "Block Hitting";
            case "Punching During Usage": return "Use Item While Digging";
            case "Smooth Sneaking": return "Sneaking";
            default: return null;
        }
    }
    public static boolean preference(String value) { return "1.7".equals(value)||Boolean.parseBoolean(value); }
    public void load() {
        loading=true;
        try {
            KazerConfigDocument doc=KazerConfigDocument.read(KazerConfigPaths.animation(minecraft.mcDataDir.toPath()));
            String value=doc.get("","BlockHitAnimation"); setState(value==null||Boolean.parseBoolean(value));
            for(Setting setting:getSettingsList()) {
                if(setting==scale)continue;
                value=doc.get("",setting.getLabel());
                String alias=previousLabel(setting.getLabel());
                if(value==null&&alias!=null)value=doc.get("",alias);
                if(value==null&&setting==thirdPerson)value=doc.get("","ThirdPerson");
                if(value!=null)setting.setValue(preference(value),false);
            }
        } catch(java.io.IOException e) { org.apache.logging.log4j.LogManager.getLogger().warn("Could not read animation preferences",e); }
        finally { loading=false; AnimationPreferences.update(this); }
    }
    private void save() {
        if(itemSwitch==null)return;
        AnimationPreferences.update(this);
        if(loading)return;
        try {
            java.nio.file.Path file=KazerConfigPaths.animation(minecraft.mcDataDir.toPath());
            KazerConfigDocument doc=KazerConfigDocument.read(file);doc.set("","BlockHitAnimation",String.valueOf(isEnabled()));
            for(Setting setting:getSettingsList())if(setting!=scale)doc.set("",setting.getLabel(),String.valueOf(setting.getValue()));
            doc.save(file);
        } catch(java.io.IOException e) { org.apache.logging.log4j.LogManager.getLogger().warn("Could not save animation preferences",e); }
    }
}
