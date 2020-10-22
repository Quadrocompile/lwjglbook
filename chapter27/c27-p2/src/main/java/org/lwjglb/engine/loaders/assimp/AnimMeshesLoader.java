package org.lwjglb.engine.loaders.assimp;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjglb.engine.Utils;
import org.lwjglb.engine.graph.Material;
import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.graph.anim.AnimGameItem;
import org.lwjglb.engine.graph.anim.AnimatedFrame;
import org.lwjglb.engine.graph.anim.Animation;

import static org.lwjgl.assimp.Assimp.*;

public class AnimMeshesLoader extends StaticMeshesLoader {

    private static final Matrix4f CORRECTION = new Matrix4f().rotate((float)Math.toRadians(90.0), new Vector3f(1,0,0));

    private static void buildTransFormationMatrices(AINodeAnim aiNodeAnim, Node node, String nodeName) {
        int numFrames = Math.max(Math.max(aiNodeAnim.mNumPositionKeys(), aiNodeAnim.mNumScalingKeys()), aiNodeAnim.mNumRotationKeys());
        AIVectorKey.Buffer positionKeys = aiNodeAnim.mPositionKeys();
        AIVectorKey.Buffer scalingKeys = aiNodeAnim.mScalingKeys();
        AIQuatKey.Buffer rotationKeys = aiNodeAnim.mRotationKeys();

        for (int i = 0; i < numFrames; i++) {

            AIVectorKey aiVecKey;
            AIVector3D vec;

            Matrix4f transfMat = new Matrix4f();

            if (i < aiNodeAnim.mNumPositionKeys()) {
                aiVecKey = positionKeys.get(i);
                vec = aiVecKey.mValue();
                transfMat = new Matrix4f().translate(vec.x(), vec.y(), vec.z());
            }
            else if(aiNodeAnim.mNumPositionKeys() > 0){
                aiVecKey = positionKeys.get(0);
                vec = aiVecKey.mValue();
                transfMat = new Matrix4f().translate(vec.x(), vec.y(), vec.z());
            }

            if (i < aiNodeAnim.mNumRotationKeys()) {
                AIQuatKey quatKey = rotationKeys.get(i);
                AIQuaternion aiQuat = quatKey.mValue();
                Quaternionf quat;
                quat = new Quaternionf(aiQuat.x(), aiQuat.y(), aiQuat.z(), aiQuat.w());
                transfMat.rotate(quat);
            }
            else if(aiNodeAnim.mNumRotationKeys() > 0){
                AIQuatKey quatKey = rotationKeys.get(0);
                AIQuaternion aiQuat = quatKey.mValue();
                Quaternionf quat;
                quat = new Quaternionf(aiQuat.x(), aiQuat.y(), aiQuat.z(), aiQuat.w());
                transfMat.rotate(quat);
            }

            if (i < aiNodeAnim.mNumScalingKeys()) {
                aiVecKey = scalingKeys.get(i);
                vec = aiVecKey.mValue();
                transfMat.scale(vec.x(), vec.y(), vec.z());
            }
            else if(aiNodeAnim.mNumScalingKeys() > 0){
                aiVecKey = scalingKeys.get(0);
                vec = aiVecKey.mValue();
                transfMat.scale(vec.x(), vec.y(), vec.z());
            }

            node.addTransformation(transfMat);
        }

    }

    public static AnimGameItem loadAnimGameItem(String resourcePath, String texturesDir, String id)
            throws Exception {
        return loadAnimGameItem(resourcePath, texturesDir,
                aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices | aiProcess_Triangulate
                | aiProcess_FixInfacingNormals | aiProcess_LimitBoneWeights, id);
    }

    public static AnimGameItem loadAnimGameItem(String resourcePath, String texturesDir, int flags, String id)
            throws Exception {
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

        List<Bone> boneList = new ArrayList<>();
        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();

        Mesh[] meshes;
        if(texturesDir.startsWith("@")){
            meshes = new Mesh[1];
            for (int i = 0; i < 1; i++) {
                AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
                Mesh mesh = processMesh(aiMesh, materials, boneList);
                meshes[i] = mesh;
            }
        }
        else{
            meshes = new Mesh[numMeshes];
            for (int i = 0; i < numMeshes; i++) {
                AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
                Mesh mesh = processMesh(aiMesh, materials, boneList);
                meshes[i] = mesh;
            }
        }


        AINode aiRootNode = aiScene.mRootNode();
        Matrix4f rootTransformation = AnimMeshesLoader.toMatrix(aiRootNode.mTransformation());

        /*
        Map<String, String> properties = new LinkedHashMap<>();
        for (int i = 0; i < aiScene.mMetaData().mNumProperties(); i++) {
            // Possible keys are "UpAxis" "UpAxisSign" "FrontAxisSign", "FrontAxis", "CoordAxis", "CoordAxisSign", "OriginalUpAxis", "OriginalUpAxisSign", "UnitScaleFactor", "OriginalUnitScaleFactor", "AmbientColor" etc.
            String key = aiScene.mMetaData().mKeys().get(i).dataString();
            int valType = aiScene.mMetaData().mValues().mType();

            AIMetaDataEntry entry = (aiScene.mMetaData().mValues().get(i));

            // entry is still a buffer with no obvious get method. With other assimp structures this .get(i) did the trick

            java.nio.ByteBuffer buf = entry.mData(entry.sizeof()*2);
            String s = StandardCharsets.US_ASCII.decode(buf).toString();

            // And using the mData version and passing it a ByteBuffer to copy the data did not work either

            //properties.put(key, ...); // what now...
        }


        int upAxis = 0;
        int frontAxis = 1;
        int coordAxis = 2;
        float upAxisSign = 1.0f;
        float frontAxisSign = 1.0f;
        float coordAxisSign = 1.0f;

        Vector3f upVec = upAxis == 0 ? new Vector3f(upAxisSign,0,0) : upAxis == 1 ? new Vector3f(0, upAxisSign,0) : new Vector3f(0, 0, upAxisSign);
        Vector3f forwardVec = frontAxis == 0 ? new Vector3f(frontAxisSign, 0, 0) : frontAxis == 1 ? new Vector3f(0, frontAxisSign, 0) : new Vector3f(0, 0, frontAxisSign);
        Vector3f rightVec = coordAxis == 0 ? new Vector3f(coordAxisSign, 0, 0) : coordAxis == 1 ? new Vector3f(0, coordAxisSign, 0) : new Vector3f(0, 0, coordAxisSign);

        Matrix4f unitVectorTransform = new Matrix4f(
                rightVec.x, rightVec.y, rightVec.z, 0.0f,
                upVec.x, upVec.y, upVec.z, 0.0f,
                forwardVec.x, forwardVec.y, forwardVec.z, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        );

        // EXPERIMENT
        //rootTransformation = rootTransformation.mul(unitVectorTransform);

        System.out.println("Processing nodes");
        */
        Node rootNode = processNodesHierarchy(aiRootNode, null, 0);

        // Add dummy nodes
        boneList.add(new Bone(18,"Dummy Prop Left", new Matrix4f()));
        boneList.add(new Bone(19,"Dummy Prop Head", new Matrix4f()));
        boneList.add(new Bone(20,"Dummy Prop Right", new Matrix4f()));
        boneList.add(new Bone(21,"Dummy Prop Back", new Matrix4f()));

        Map<String, Animation> animations = processAnimations(aiScene, boneList, rootNode, rootTransformation);
        AnimGameItem item = new AnimGameItem(meshes, animations, id);

        return item;
    }

    private static List<AnimatedFrame> buildAnimationFrames(List<Bone> boneList, Node rootNode,
            Matrix4f rootTransformation) {

        int numFrames = rootNode.getAnimationFrames();
        List<AnimatedFrame> frameList = new ArrayList<>();
        for (int i = 0; i < numFrames; i++) {
            AnimatedFrame frame = new AnimatedFrame();
            frameList.add(frame);

            int numBones = boneList.size();
            for (int j = 0; j < numBones; j++) {
                Bone bone = boneList.get(j);
                Node node = rootNode.findByName(bone.getBoneName());
                Matrix4f boneMatrix = Node.getParentTransforms(node, i);
                boneMatrix.mul(bone.getOffsetMatrix());
                boneMatrix = new Matrix4f(rootTransformation).mul(boneMatrix);
                frame.setMatrix(j, boneMatrix);
            }
        }

        return frameList;
    }

    private static Map<String, Animation> processAnimations(AIScene aiScene, List<Bone> boneList,
            Node rootNode, Matrix4f rootTransformation) {
        Map<String, Animation> animations = new HashMap<>();

        // Process all animations
        int numAnimations = aiScene.mNumAnimations();
        PointerBuffer aiAnimations = aiScene.mAnimations();
        for (int i = 0; i < numAnimations; i++) {
            AIAnimation aiAnimation = AIAnimation.create(aiAnimations.get(i));

            // Calculate transformation matrices for each node
            int numChanels = aiAnimation.mNumChannels();
            PointerBuffer aiChannels = aiAnimation.mChannels();
            for (int j = 0; j < numChanels; j++) {
                AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(j));
                String nodeName = aiNodeAnim.mNodeName().dataString();
                System.out.println("Lookup node '" + nodeName + "'");
                Node node = rootNode.findByName(nodeName);
                buildTransFormationMatrices(aiNodeAnim, node, nodeName);
            }

            List<AnimatedFrame> frames = buildAnimationFrames(boneList, rootNode, rootTransformation);
            Animation animation = new Animation(aiAnimation.mName().dataString(), frames, aiAnimation.mDuration());
            animations.put(animation.getName(), animation);
        }
        return animations;
    }

    private static void processBones(AIMesh aiMesh, List<Bone> boneList, List<Integer> boneIds,
            List<Float> weights) {
        Map<Integer, List<VertexWeight>> weightSet = new HashMap<>();
        int numBones = aiMesh.mNumBones();
        PointerBuffer aiBones = aiMesh.mBones();
        for (int i = 0; i < numBones; i++) {
            AIBone aiBone = AIBone.create(aiBones.get(i));
            int id = boneList.size();
            Bone bone = new Bone(id, aiBone.mName().dataString(), toMatrix(aiBone.mOffsetMatrix()));
            boneList.add(bone);
            int numWeights = aiBone.mNumWeights();
            AIVertexWeight.Buffer aiWeights = aiBone.mWeights();
            for (int j = 0; j < numWeights; j++) {
                AIVertexWeight aiWeight = aiWeights.get(j);
                VertexWeight vw = new VertexWeight(bone.getBoneId(), aiWeight.mVertexId(),
                        aiWeight.mWeight());
                List<VertexWeight> vertexWeightList = weightSet.get(vw.getVertexId());
                if (vertexWeightList == null) {
                    vertexWeightList = new ArrayList<>();
                    weightSet.put(vw.getVertexId(), vertexWeightList);
                }
                vertexWeightList.add(vw);
            }
        }

        int numVertices = aiMesh.mNumVertices();
        for (int i = 0; i < numVertices; i++) {
            List<VertexWeight> vertexWeightList = weightSet.get(i);
            int size = vertexWeightList != null ? vertexWeightList.size() : 0;
            for (int j = 0; j < Mesh.MAX_WEIGHTS; j++) {
                if (j < size) {
                    VertexWeight vw = vertexWeightList.get(j);
                    weights.add(vw.getWeight());
                    boneIds.add(vw.getBoneId());
                } else {
                    weights.add(0.0f);
                    boneIds.add(0);
                }
            }
        }
    }

    private static Mesh processMesh(AIMesh aiMesh, List<Material> materials, List<Bone> boneList) {
        List<Float> vertices = new ArrayList<>();
        List<Float> textures = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Integer> boneIds = new ArrayList<>();
        List<Float> weights = new ArrayList<>();

        processVertices(aiMesh, vertices);
        processNormals(aiMesh, normals);
        processTextCoords(aiMesh, textures);
        processIndices(aiMesh, indices);
        processBones(aiMesh, boneList, boneIds, weights);
        if ( textures.size() == 0) {
            int numElements = (vertices.size() / 3) * 2;
            for (int i=0; i<numElements; i++) {
                textures.add(0.0f);
            }
        }

        Mesh mesh = new Mesh(Utils.listToArray(vertices), Utils.listToArray(textures),
                Utils.listToArray(normals), Utils.listIntToArray(indices),
                Utils.listIntToArray(boneIds), Utils.listToArray(weights));
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

    /*
    private static Node processNodesHierarchy(AINode aiNode, Node parentNode) {
        String nodeName = aiNode.mName().dataString();
        Node node = new Node(nodeName, parentNode);

        int numChildren = aiNode.mNumChildren();
        PointerBuffer aiChildren = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            AINode aiChildNode = AINode.create(aiChildren.get(i));
            Node childNode = processNodesHierarchy(aiChildNode, node);
            node.addChild(childNode);
        }

        return node;
    }
     */

    private static Node processNodesHierarchy(AINode aiNode, Node parentNode, int depth) {
        String nodeName = aiNode.mName().dataString();
        Node node = new Node(nodeName, parentNode, toMatrix(aiNode.mTransformation()));

        for (int i = 0; i < depth; i++) {
            if(i==depth-1) {
                //System.out.print("|-");
            }
            else{
                //System.out.print("  ");
            }
            System.out.print("\t");
        }
        //System.out.print("+");
        //System.out.println(" " + nodeName + "");
        System.out.println("" + nodeName + "");

        int numChildren = aiNode.mNumChildren();
        PointerBuffer aiChildren = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            AINode aiChildNode = AINode.create(aiChildren.get(i));
            Node childNode = processNodesHierarchy(aiChildNode, node, depth+1);
            node.addChild(childNode);
        }

        return node;
    }

    private static Matrix4f toMatrix(AIMatrix4x4 aiMatrix4x4) {
        Matrix4f result = new Matrix4f();
        result.m00(aiMatrix4x4.a1());
        result.m10(aiMatrix4x4.a2());
        result.m20(aiMatrix4x4.a3());
        result.m30(aiMatrix4x4.a4());
        result.m01(aiMatrix4x4.b1());
        result.m11(aiMatrix4x4.b2());
        result.m21(aiMatrix4x4.b3());
        result.m31(aiMatrix4x4.b4());
        result.m02(aiMatrix4x4.c1());
        result.m12(aiMatrix4x4.c2());
        result.m22(aiMatrix4x4.c3());
        result.m32(aiMatrix4x4.c4());
        result.m03(aiMatrix4x4.d1());
        result.m13(aiMatrix4x4.d2());
        result.m23(aiMatrix4x4.d3());
        result.m33(aiMatrix4x4.d4());

        return result;
    }
}
