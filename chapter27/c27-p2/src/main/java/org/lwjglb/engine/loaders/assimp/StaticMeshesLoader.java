package org.lwjglb.engine.loaders.assimp;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjglb.engine.Utils;
import org.lwjglb.engine.graph.Material;
import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.graph.Texture;

import static org.lwjgl.assimp.Assimp.*;

public class StaticMeshesLoader {

    public static Mesh[] load(String resourcePath, String texturesDir, float tx, float ty, float tz, float rx, float ry, float rz) throws Exception {
        return load(resourcePath, texturesDir,
                aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices | aiProcess_Triangulate
                        | aiProcess_FixInfacingNormals, tx, ty, tz, rx, ry, rz);
    }

    public static Mesh[] load(String resourcePath, String texturesDir, int flags, float tx, float ty, float tz, float rx, float ry, float rz) throws Exception {
        AIScene aiScene = aiImportFile(resourcePath, flags);
        if (aiScene == null) {
            throw new Exception("Error loading model");
        }

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        List<Material> materials = new ArrayList<>();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            processMaterial(aiMaterial, materials, texturesDir);
        }

        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        Mesh[] meshes = new Mesh[numMeshes];
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            Mesh mesh = processMesh(aiMesh, materials, tx, ty, tz, rx, ry, rz);
            meshes[i] = mesh;
        }

        aiFreeScene(aiScene);

        return meshes;
    }

    private static Mesh processMesh(AIMesh aiMesh, List<Material> materials, float tx, float ty, float tz, float rx, float ry, float rz) {
        List<Float> vertices = new ArrayList<>();
        List<Float> textures = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        processVertices(aiMesh, vertices, tx, ty, tz, rx, ry, rz);
        processNormals(aiMesh, normals);
        processTextCoords(aiMesh, textures);
        processIndices(aiMesh, indices);

        Mesh mesh = new Mesh(Utils.listToArray(vertices), Utils.listToArray(textures),
                Utils.listToArray(normals), Utils.listIntToArray(indices));
        Material material;
        int materialIdx = aiMesh.mMaterialIndex();
        if (materialIdx >= 0 && materialIdx < materials.size()) {
            material = materials.get(materialIdx);
        } else {
            material = new Material();
        }
        mesh.setMaterial(material);

        return mesh;
    }

    protected static void processVertices(AIMesh aiMesh, List<Float> vertices, float tx, float ty, float tz, float rx, float ry, float rz) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();

            /*
            bool StaticModelClass::LoadModel(string filename, XMFLOAT3 trans, XMFLOAT3 rot)
            {
                string fbxSuffixLower = ".fbx";
                string fbxSuffixUpper = ".FBX";
                if (0 == filename.compare(filename.size() - fbxSuffixLower.size(), fbxSuffixLower.size(), fbxSuffixLower)
                    || 0 == filename.compare(filename.size() - fbxSuffixUpper.size(), fbxSuffixUpper.size(), fbxSuffixUpper))
                {
                    return LoadModelFBX(filename, trans, rot);
                }
                else
                {
                    ifstream fin;
                    char input;
                    int i;

                    // Calculate affine transformation
                    XMVECTOR zero = XMVectorSet(0.0f, 0.0f, 0.0f, 1.0f);
                    XMVECTOR one = XMLoadFloat3(&XMFLOAT3(1.0f, 1.0f, 1.0f));
                    XMVECTOR quatRotation = XMQuaternionRotationRollPitchYaw(rot.x, rot.y, rot.z);
                    XMVECTOR vecTranslation = XMLoadFloat3(&trans);
                    XMMATRIX affineTransform = XMMatrixAffineTransformation(one, zero, quatRotation, vecTranslation);

            XMVector3Transform(vertexPositionVec, affineTransform));
             */
            aiVertex.set(aiVertex.x() + tx, aiVertex.y() + ty, aiVertex.z() + tz);


            vertices.add(aiVertex.x());
            vertices.add(aiVertex.y());
            vertices.add(aiVertex.z());
        }
    }









    public static Mesh[] load(String resourcePath, String texturesDir) throws Exception {
        return load(resourcePath, texturesDir,
                aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices | aiProcess_Triangulate
                        | aiProcess_FixInfacingNormals);
    }

    public static Mesh[] load(String resourcePath, String texturesDir, int flags) throws Exception {
        AIScene aiScene = aiImportFile(resourcePath, flags);
        if (aiScene == null) {
            throw new Exception("Error loading model");
        }

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        List<Material> materials = new ArrayList<>();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            processMaterial(aiMaterial, materials, texturesDir);
        }

        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        Mesh[] meshes = new Mesh[numMeshes];
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            Mesh mesh = processMesh(aiMesh, materials);
            meshes[i] = mesh;
        }

        aiFreeScene(aiScene);

        return meshes;
    }

    protected static void processIndices(AIMesh aiMesh, List<Integer> indices) {
        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer aiFaces = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) {
            AIFace aiFace = aiFaces.get(i);
            IntBuffer buffer = aiFace.mIndices();
            while (buffer.remaining() > 0) {
                indices.add(buffer.get());
            }
        }
    }

    protected static void processMaterial(AIMaterial aiMaterial, List<Material> materials,
            String texturesDir) throws Exception {
        AIColor4D colour = AIColor4D.create();

        AIString path = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null,
                null, null, null, null, null);
        String textPath = path.dataString();
        Texture texture = null;

        if(texturesDir.startsWith("@")){
            texture = TextureCache.getInstance().getTexture("models/fbx_not_working/" + texturesDir.substring(1));
        }
        else if (textPath.length() > 0) {
            TextureCache textCache = TextureCache.getInstance();
            String textureFile = "";
			if ( texturesDir != null && texturesDir.length() > 0 ) {
				textureFile += texturesDir + "/";
			}
			textureFile += textPath;
            textureFile = textureFile.replace("//", "/");
            texture = textCache.getTexture(textureFile);
        }




        Vector4f ambient = Material.DEFAULT_COLOUR;
        int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0,
                colour);
        if (result == 0) {
            ambient = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Vector4f diffuse = Material.DEFAULT_COLOUR;
        result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0,
                colour);
        if (result == 0) {
            diffuse = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Vector4f specular = Material.DEFAULT_COLOUR;
        result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0,
                colour);
        if (result == 0) {
            specular = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());
        }

        Material material = new Material(ambient, diffuse, specular, 1.0f);
        material.setTexture(texture);
        materials.add(material);

        path.free();
    }

    private static Mesh processMesh(AIMesh aiMesh, List<Material> materials) {
        List<Float> vertices = new ArrayList<>();
        List<Float> textures = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        processVertices(aiMesh, vertices);
        processNormals(aiMesh, normals);
        processTextCoords(aiMesh, textures);
        processIndices(aiMesh, indices);

        Mesh mesh = new Mesh(Utils.listToArray(vertices), Utils.listToArray(textures),
                Utils.listToArray(normals), Utils.listIntToArray(indices));
        Material material;
        int materialIdx = aiMesh.mMaterialIndex();
        if (materialIdx >= 0 && materialIdx < materials.size()) {
            material = materials.get(materialIdx);
        } else {
            material = new Material();
        }
        mesh.setMaterial(material);

        return mesh;
    }

    protected static void processNormals(AIMesh aiMesh, List<Float> normals) {
        AIVector3D.Buffer aiNormals = aiMesh.mNormals();
        while (aiNormals != null && aiNormals.remaining() > 0) {
            AIVector3D aiNormal = aiNormals.get();
            normals.add(aiNormal.x());
            normals.add(aiNormal.y());
            normals.add(aiNormal.z());
        }
    }

    protected static void processTextCoords(AIMesh aiMesh, List<Float> textures) {
        AIVector3D.Buffer textCoords = aiMesh.mTextureCoords(0);
        int numTextCoords = textCoords != null ? textCoords.remaining() : 0;
        for (int i = 0; i < numTextCoords; i++) {
            AIVector3D textCoord = textCoords.get();
            textures.add(textCoord.x());
            textures.add(1 - textCoord.y());
        }
    }

    protected static void processVertices(AIMesh aiMesh, List<Float> vertices) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();

            /*
            final double deg90 = -1.5708;
            int iOffX = 1;
            int iOffY = 0;
            int iOffZ = 0;
            Quaternionf rot = new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffX)), 1.0, 0.0, 0.0));
            rot = rot.mul(new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffY)), 0.0, 1.0, 0.0)));
            rot = rot.mul(new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffZ)), 0.0, 0.0, 1.0)));

            Vector4f pos = new Vector4f(aiVertex.x(), aiVertex.y(), aiVertex.z(), 0.0f);

            Matrix4f transMat = new Matrix4f();

            transMat = transMat.translationRotateScale(
                    0.0f, 0.0f, 0.0f,
                    rot.x, rot.y, rot.z, rot.w,
                    1.0f, 1.0f, 1.0f);

            pos = pos.mul(transMat);

            vertices.add(pos.x);
            vertices.add(pos.y);
            vertices.add(pos.z);

            // */

            // /*
            vertices.add(aiVertex.x());
            vertices.add(aiVertex.y());
            vertices.add(aiVertex.z());
            // */
        }
    }
}
