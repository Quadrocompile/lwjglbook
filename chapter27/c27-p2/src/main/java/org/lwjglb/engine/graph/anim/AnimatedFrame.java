package org.lwjglb.engine.graph.anim;

import java.util.Arrays;

import org.joml.AxisAngle4d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class AnimatedFrame {

    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();

    public static final int MAX_JOINTS = 150;

    private final Matrix4f[] jointMatrices;

    public AnimatedFrame() {
        jointMatrices = new Matrix4f[MAX_JOINTS];
        Arrays.fill(jointMatrices, IDENTITY_MATRIX);
    }

    public Matrix4f[] getJointMatrices() {

        /*
        Matrix4f[] identityMats = new Matrix4f[MAX_JOINTS];
        for (int i = 0; i < MAX_JOINTS; i++) {
            identityMats[i] = new Matrix4f();
        }
        if(1==1) return identityMats;
        // */

        final double deg90 = -1.5708;
        int iOffX = 0;
        int iOffY = 0;
        int iOffZ = 0;
        Quaternionf rot = new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffX)), 1.0, 0.0, 0.0));
        rot = rot.mul(new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffY)), 0.0, 1.0, 0.0)));
        rot = rot.mul(new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffZ)), 0.0, 0.0, 1.0)));

        Matrix4f transMat = new Matrix4f();
        transMat = transMat.translationRotateScale(
                0.0f, 0.0f, 0.0f,
                rot.x, rot.y, rot.z, rot.w,
                1.0f, 1.0f, 1.0f);

        Matrix4f[] idMats = new Matrix4f[MAX_JOINTS];
        for (int i = 0; i < MAX_JOINTS; i++) {
            Matrix4f mat  = new Matrix4f(jointMatrices[i]);
            idMats[i] = mat.mul(transMat);
        }
        return idMats;
        //return jointMatrices;
    }

    public void setMatrix(int pos, Matrix4f jointMatrix) {
        jointMatrices[pos] = jointMatrix;
    }
}

