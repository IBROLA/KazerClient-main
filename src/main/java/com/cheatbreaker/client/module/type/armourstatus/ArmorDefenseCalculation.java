package com.cheatbreaker.client.module.type.armourstatus;

/**
 * Armor defense calculation, ported from PolyPatcher's ArmorStatusRenderer.
 * Pure functions — no Minecraft state — so the math is unit-testable.
 */
public final class ArmorDefenseCalculation {

    private ArmorDefenseCalculation() {
    }

    /** Screen placement of the defense readout, in GUI pixels. */
    public static final class Placement {
        public final int x;
        public final int y;

        Placement(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Corner placement for the readout, inset 10px from the chosen screen edges.
     * Positions: "BL" (default), "BR", "TR", "TL". Long-form values are still
     * accepted so profiles saved before the abbreviations keep working.
     */
    public static Placement place(String position, int screenWidth, int screenHeight, int textWidth) {
        String corner = position == null ? "" : position.trim().toUpperCase();
        if (corner.equals("LEFT BOTTOM")) {
            corner = "BL";
        } else if (corner.equals("RIGHT BOTTOM")) {
            corner = "BR";
        } else if (corner.equals("RIGHT TOP")) {
            corner = "TR";
        } else if (corner.equals("LEFT TOP")) {
            corner = "TL";
        }
        boolean right = corner.equals("BR") || corner.equals("TR");
        boolean top = corner.equals("TR") || corner.equals("TL");
        int x = right ? screenWidth - textWidth - 10 : 10;
        int y = top ? 10 : screenHeight - 16;
        return new Placement(x, y);
    }

    /**
     * @param armorPoints total armor points (sum of damageReduceAmount across worn pieces)
     * @param epf         total Enchantment Protection Factor (capped at 25 internally)
     * @param resistance  resistance potion level (amplifier + 1), 0 if none
     * @return damage reduction percentage, truncated to two decimals
     */
    public static double calculate(int armorPoints, int epf, int resistance) {
        double armor = armorPoints * 0.04;
        epf = Math.min(epf, 25);
        double prot = calcProtection(epf);
        double total = armor + (1.0 - armor) * prot * 0.04;
        total += (1.0 - total) * resistance * 0.2;
        total = Math.min(total, 1.0);
        return truncateTwoDecimals(total * 100.0);
    }

    private static double calcProtection(int epf) {
        double protection = 0.0;
        for (int i = 50; i <= 100; i++) {
            protection += Math.min(Math.ceil(epf * i / 100.0), 20.0);
        }
        return protection / 51.0;
    }

    private static double truncateTwoDecimals(double value) {
        if (value == 0.0) {
            return value;
        }
        return (int) (value * 100.0) / 100.0;
    }

    /**
     * Formats the display string per PolyPatcher rules.
     *
     * @param protection   normal protection percentage
     * @param projectile   projectile protection percentage
     * @param showNormal   whether the normal-protection toggle is on
     * @param showProjectile whether the projectile-protection toggle is on
     * @return display string, or null if nothing should be shown
     */
    public static String format(double protection, double projectile, boolean showNormal, boolean showProjectile) {
        if (!showNormal && !showProjectile) {
            return null;
        }
        if (protection == 0.0 && projectile == 0.0) {
            return null;
        }
        if (showNormal && !showProjectile) {
            return protection + "%";
        }
        if (!showNormal) {
            return projectile + "%";
        }
        if (protection == projectile) {
            return protection + "%";
        }
        return protection + "% | " + projectile + "%";
    }
}
