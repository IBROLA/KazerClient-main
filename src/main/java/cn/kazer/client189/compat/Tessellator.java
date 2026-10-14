package cn.kazer.client189.compat;

import org.lwjgl.opengl.GL11;

/** Immediate-mode GUI adapter for the reference's legacy vertex API. */
public final class Tessellator {
    public static final Tessellator instance = new Tessellator();
    private double xOffset;
    private double yOffset;
    private double zOffset;
    private int mode;
    private boolean drawing;
    private final java.util.List<Vertex> vertices = new java.util.ArrayList<>();
    private int vertexCount;
    private float red = 1, green = 1, blue = 1, alpha = 1;
    private float nx, ny, nz;
    private boolean colored, normal;

    public void startDrawingQuads() { startDrawing(GL11.GL_QUADS); }
    public void startDrawing(int mode) {
        if (drawing) throw new IllegalStateException("Already drawing");
        this.mode = mode; vertexCount = 0; drawing = true; colored = false; normal = false;
    }
    public int draw() {
        if (!drawing) throw new IllegalStateException("Not drawing");
        drawing = false;
        GL11.glBegin(mode);
        for (int i=0;i<vertexCount;i++) {
            Vertex v=vertices.get(i);
            if (v.colored) GL11.glColor4f(v.r, v.g, v.b, v.a);
            if (v.normal) GL11.glNormal3f(v.nx, v.ny, v.nz);
            if (v.textured) GL11.glTexCoord2d(v.u, v.v);
            GL11.glVertex3d(v.x, v.y, v.z);
        }
        GL11.glEnd();
        if(colored) net.minecraft.client.renderer.GlStateManager.resetColor();
        return vertexCount;
    }
    public void addVertex(double x, double y, double z) {
        add(x, y, z, 0, 0, false);
    }
    public void addVertexWithUV(double x, double y, double z, double u, double v) {
        add(x, y, z, u, v, true);
    }
    private void add(double x, double y, double z, double u, double v, boolean texture) {
        if (!drawing) throw new IllegalStateException("Not drawing");
        if(vertexCount==vertices.size())vertices.add(new Vertex());
        Vertex vertex = vertices.get(vertexCount++);
        vertex.x = x + xOffset; vertex.y = y + yOffset; vertex.z = z + zOffset;
        vertex.u = u; vertex.v = v; vertex.textured = texture;
        vertex.r = red; vertex.g = green; vertex.b = blue; vertex.a = alpha; vertex.colored = colored;
        vertex.nx = nx; vertex.ny = ny; vertex.nz = nz; vertex.normal = normal;
    }
    public void setColorRGBA_F(float r, float g, float b, float a) { red = r; green = g; blue = b; alpha = a; colored = true; }
    public void setColorOpaque_F(float r, float g, float b) { setColorRGBA_F(r, g, b, 1); }
    public void setColorRGBA(int r, int g, int b, int a) { setColorRGBA_F(r / 255F, g / 255F, b / 255F, a / 255F); }
    public void setColorRGBA_I(int rgb, int a) { setColorRGBA(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, a); }
    public void setColorOpaque_I(int rgb) { setColorRGBA_I(rgb, 255); }
    public void setColorOpaque(int r, int g, int b) { setColorRGBA(r, g, b, 255); }
    public void setNormal(float x, float y, float z) { nx = x; ny = y; nz = z; normal = true; }
    public void setTranslation(double x, double y, double z) { xOffset = x; yOffset = y; zOffset = z; }
    public void setBrightness(int value) {
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
                net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, value & 65535, value >>> 16);
    }
    public void disableColor() { }
    private static final class Vertex {
        double x, y, z, u, v; float r, g, b, a, nx, ny, nz; boolean colored, normal, textured;
    }
}
