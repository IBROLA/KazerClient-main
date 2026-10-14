package com.cheatbreaker.client.module.type.motionblur;

import javax.vecmath.Matrix4f;
import javax.vecmath.SingularMatrixException;

/**
 * Camera state for the Monkey (depth-reprojection) motion blur, ported from
 * PolyBlur's MonkeyBlur (MIT, DJtheRedstoner). Keeps the current frame's
 * projection/model-view matrices plus camera position, and the previous
 * frame's forward copies, so the fragment shader can reproject each pixel
 * into the previous frame's screen position.
 *
 * Pure value holder + transition rules; all GL interaction lives in
 * {@code MonkeyBlurRenderer}.
 */
public final class MonkeyCameraState {

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f projectionInverse = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final Matrix4f modelViewInverse = new Matrix4f();
    private final Matrix4f previousProjection = new Matrix4f();
    private final Matrix4f previousModelView = new Matrix4f();

    private float cameraX, cameraY, cameraZ;
    private float previousCameraX, previousCameraY, previousCameraZ;

    private int perspective = -1;
    private boolean changedPerspectiveThisFrame;

    /** Retires the captured frame as "previous" and flags perspective switches. */
    public void beginFrame(int perspective) {
        this.previousProjection.set(this.projection);
        this.previousModelView.set(this.modelView);
        this.previousCameraX = this.cameraX;
        this.previousCameraY = this.cameraY;
        this.previousCameraZ = this.cameraZ;
        this.changedPerspectiveThisFrame = this.perspective >= 0 && perspective != this.perspective;
        this.perspective = perspective;
    }

    public boolean changedPerspectiveThisFrame() {
        return this.changedPerspectiveThisFrame;
    }

    /** Captures the live camera matrices (GL column-major content) and interpolants. */
    public void captureCurrent(Matrix4f projection, Matrix4f modelView, float cameraX, float cameraY, float cameraZ) {
        this.projection.set(projection);
        this.modelView.set(modelView);
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        invertOrIdentity(projection, this.projectionInverse);
        invertOrIdentity(modelView, this.modelViewInverse);
    }

    /** Perspective-switch frame: make both frames identical so the velocity field is zero. */
    public void realign() {
        this.beginFrame(this.perspective);
        this.changedPerspectiveThisFrame = false;
    }

    /** {@code glGetFloat} matrices arrive column-major; vecmath fields are row/col named. */
    public static Matrix4f fromGlColumnMajor(float[] gl) {
        Matrix4f matrix = new Matrix4f();
        for (int i = 0; i < 16; i++) {
            matrix.setElement(i % 4, i / 4, gl[i]);
        }
        return matrix;
    }

    static void invertOrIdentity(Matrix4f source, Matrix4f destination) {
        try {
            destination.invert(source);
        } catch (SingularMatrixException singular) {
            destination.setIdentity();
        }
    }

    public Matrix4f getProjectionInverse() {
        return this.projectionInverse;
    }

    public Matrix4f getModelViewInverse() {
        return this.modelViewInverse;
    }

    public Matrix4f getPreviousProjection() {
        return this.previousProjection;
    }

    public Matrix4f getPreviousModelView() {
        return this.previousModelView;
    }

    public float getCameraX() { return this.cameraX; }
    public float getCameraY() { return this.cameraY; }
    public float getCameraZ() { return this.cameraZ; }
    public float getPreviousCameraX() { return this.previousCameraX; }
    public float getPreviousCameraY() { return this.previousCameraY; }
    public float getPreviousCameraZ() { return this.previousCameraZ; }
}
