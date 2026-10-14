package com.cheatbreaker.client.ui.element.module;

import com.cheatbreaker.client.CheatBreaker;
import com.cheatbreaker.client.module.AbstractModule;
import com.cheatbreaker.client.ui.element.AbstractScrollableElement;
import com.cheatbreaker.client.ui.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class ModulePreviewContainer
        extends AbstractScrollableElement {
    private static final int COLUMNS = 3;
    private static final int ROW_HEIGHT = 112;

    private final List<ModulePreviewElement> elements = new ArrayList<>();

    public ModulePreviewContainer(float f, int n, int n2, int n3, int n4) {
        super(f, n, n2, n3, n4);
        for (AbstractModule module : CheatBreaker.getInstance().moduleManager.modules) {
            if (module == CheatBreaker.getInstance().moduleManager.notifications) continue;
            ModulePreviewElement element = new ModulePreviewElement(this, module, f);
            this.elements.add(element);
        }
    }

    @Override
    public boolean lIIIIlIIllIIlIIlIIIlIIllI(AbstractModule cBModule) {
        return false;
    }

    @Override
    public void lIIIIIIIIIlIllIIllIlIIlIl(AbstractModule cBModule) {
    }

    @Override
    public void handleDrawElement(int mouseX, int mouseY, float partialTicks) {
        super.handleDrawElement(mouseX, mouseY, partialTicks);
        RenderUtil.lIIIIlIIllIIlIIlIIIlIIllI(this.x, this.y, this.x + this.width, this.y + this.height + 2, (double)8, -657931);
        this.preDraw(mouseX, mouseY);
        int n3 = 0;
        int n4 = 0;
        for (ModulePreviewElement element : this.elements) {
            element.yOffset = this.lIIIIllIIlIlIllIIIlIllIlI;
            element.setDimensions(this.x + 4 + n3 * 120, this.y + 4 + n4 * ROW_HEIGHT, 116, 108);
            element.handleDrawElement(mouseX, mouseY, partialTicks);
            if (++n3 != COLUMNS) continue;
            ++n4;
            n3 = 0;
        }
        this.IlllIllIlIIIIlIIlIIllIIIl = contentHeight(this.elements.size());
        this.postDraw(mouseX, mouseY);
    }

    static int contentHeight(int elementCount) {
        int rows = (elementCount + COLUMNS - 1) / COLUMNS;
        return 4 + rows * ROW_HEIGHT;
    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int button) {
        super.handleMouseClick(mouseX, mouseY, button);
        for (ModulePreviewElement element : this.elements) {
            if (!element.isMouseInside(mouseX, mouseY)) continue;
            element.handleMouseClick(mouseX, mouseY, button);
        }
    }
}
