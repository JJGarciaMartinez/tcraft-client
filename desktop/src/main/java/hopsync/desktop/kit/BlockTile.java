package hopsync.desktop.kit;

import com.formdev.flatlaf.icons.FlatAbstractIcon;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A Minecraft-like block that stands for a pack: grass, stone or netherrack, chosen from the pack id so a
 * pack always gets the same one.
 */
public final class BlockTile extends FlatAbstractIcon {

    public enum Variant { GRASS, STONE, NETHER }

    private static final Color GRASS = new Color(0x5DBB63);
    private static final Color GRASS_EDGE = new Color(0x4A9A50);
    private static final Color DIRT = new Color(0x6E4B33);

    private final Variant variant;

    private BlockTile(int size, Variant variant) {
        super(size, size, null);
        this.variant = variant;
    }

    public static BlockTile forPack(String packId, int size) {
        return new BlockTile(size, variantOf(packId));
    }

    public static Variant variantOf(String packId) {
        Variant[] variants = Variant.values();
        return variants[Math.floorMod(packId.hashCode(), variants.length)];
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        float size = width;
        // Bigger tiles get rounder corners, as in the design (12 px at 64, 7 px at 34)
        float arc = Math.max(10f, size * 0.38f);
        Shape tile = new RoundRectangle2D.Float(0, 0, size, size, arc, arc);
        g.clip(tile);
        switch (variant) {
            case GRASS -> {
                g.setColor(DIRT);
                g.fill(tile);
                g.setColor(GRASS_EDGE);
                g.fillRect(0, 0, width, Math.round(size * 0.42f));
                g.setColor(GRASS);
                g.fillRect(0, 0, width, Math.round(size * 0.34f));
            }
            case STONE -> {
                g.setPaint(new GradientPaint(0, 0, new Color(0x8B9095), size, size, new Color(0x6B7075)));
                g.fill(tile);
            }
            case NETHER -> {
                g.setPaint(new GradientPaint(0, 0, new Color(0x8E3434), size, size, new Color(0x5E2121)));
                g.fill(tile);
            }
        }
        // Pixel texture: darker every other column, lighter every other row, in steps of 1/8 of the tile
        float step = size / 8;
        g.setColor(new Color(0, 0, 0, variant == Variant.NETHER ? 46 : 31));
        for (int i = 0; i < 8; i += 2) {
            g.fill(new Rectangle2D.Float(i * step, 0, step, size));
        }
        g.setColor(new Color(255, 255, 255, 15));
        for (int i = 0; i < 8; i += 2) {
            g.fill(new Rectangle2D.Float(0, i * step, size, step));
        }
    }
}
