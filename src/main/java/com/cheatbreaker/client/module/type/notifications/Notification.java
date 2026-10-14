package com.cheatbreaker.client.module.type.notifications;

import net.minecraft.client.gui.ScaledResolution;

class Notification {
    public CBNotificationType type;
    public String lIIIIIIIIIlIllIIllIlIIlIl;
    public long IlllIIIlIlllIllIlIIlllIlI;
    public long IIIIllIlIIIllIlllIlllllIl = System.currentTimeMillis();
    public int IIIIllIIllIIIIllIllIIIlIl = 2;
    public int IlIlIIIlllIIIlIlllIlIllIl;
    public int IIIllIllIlIlllllllIlIlIII = 20;
    public int IllIIIIIIIlIlIllllIIllIII = 0;
    final CBNotificationsModule notificationsModule;

    Notification(CBNotificationsModule notificationsModule, ScaledResolution scaledResolution, CBNotificationType notificationType, String string, long l) {
        this.notificationsModule = notificationsModule;
        this.type = notificationType;
        this.lIIIIIIIIIlIllIIllIlIIlIl = string;
        this.IlllIIIlIlllIllIlIIlllIlI = l;
        this.IIIllIllIlIlllllllIlIlIII = notificationType == CBNotificationType.DEFAULT ? 16 : 20;
        this.IlIlIIIlllIIIlIlllIlIllIl = scaledResolution.getScaledHeight() - 14 - this.IIIllIllIlIlllllllIlIlIII;
        this.IIIIllIIllIIIIllIllIIIlIl = scaledResolution.getScaledHeight() + this.IIIllIllIlIlllllllIlIlIII;
    }

    public void lIIIIlIIllIIlIIlIIIlIIllI() {
        if (this.IlIlIIIlllIIIlIlllIlIllIl != -1) {
            ++this.IllIIIIIIIlIlIllllIIllIII;
            float f = (float)this.IllIIIIIIIlIlIllllIIllIII * ((float)this.IllIIIIIIIlIlIllllIIllIII / (float)5) / (float)7;
            if (this.IIIIllIIllIIIIllIllIIIlIl > this.IlIlIIIlllIIIlIlllIlIllIl) {
                if ((float)this.IIIIllIIllIIIIllIllIIIlIl - f < (float)this.IlIlIIIlllIIIlIlllIlIllIl) {
                    this.IIIIllIIllIIIIllIllIIIlIl = this.IlIlIIIlllIIIlIlllIlIllIl;
                    this.IlIlIIIlllIIIlIlllIlIllIl = -1;
                } else {
                    this.IIIIllIIllIIIIllIllIIIlIl = (int)((float)this.IIIIllIIllIIIIllIllIIIlIl - f);
                }
            } else if (this.IIIIllIIllIIIIllIllIIIlIl < this.IlIlIIIlllIIIlIlllIlIllIl) {
                if ((float)this.IIIIllIIllIIIIllIllIIIlIl + f > (float)this.IlIlIIIlllIIIlIlllIlIllIl) {
                    this.IIIIllIIllIIIIllIllIIIlIl = this.IlIlIIIlllIIIlIlllIlIllIl;
                    this.IlIlIIIlllIIIlIlllIlIllIl = -1;
                } else {
                    this.IIIIllIIllIIIIllIllIIIlIl = (int)((float)this.IIIIllIIllIIIIllIllIIIlIl + f);
                }
            } else {
                this.IlIlIIIlllIIIlIlllIlIllIl = -1;
            }
        }
    }

    public void lIIIIlIIllIIlIIlIIIlIIllI(int n) {
        CBNotificationRenderer.draw(this.type, this.lIIIIIIIIIlIllIIllIlIIlIl, n, this.IIIIllIIllIIIIllIllIIIlIl,
            this.IIIIllIlIIIllIlllIlllllIl, this.IlllIIIlIlllIllIlIIlllIlI, System.currentTimeMillis());
    }
}
