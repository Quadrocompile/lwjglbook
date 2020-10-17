package org.lwjglb.engine.graph.LHMP;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.assimp.*;
import org.lwjglb.engine.Utils;
import org.lwjglb.engine.graph.Material;
import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.graph.anim.AnimatedFrame;

import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.assimp.Assimp.aiImportFile;

public class LHMPBaseMesh {

    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();
    public static final int MAX_JOINTS = 150;

    public List<Integer> boneHierarchy = new ArrayList<>();
    public List<Matrix4f> boneOffsets = new ArrayList<>();
    public Map<String, Integer> boneIDMap = new HashMap<>();
    public List<Matrix4f> nodeTransforms = new ArrayList<>();

    public Mesh mesh;

    public Map<String, LHMPAnimation> animationMap = new HashMap<>();

    public Map<String, AnimationClip> animationClips = new HashMap<>();

    public AnimationClip getAnimationClip(String animatioName){
        return animationClips.get(animatioName);
    }

    public void loadBaseMesh(String path, int flags, String id) throws Exception {
        long t1 = System.currentTimeMillis();

        Set<String> requiredNodes = new HashSet<>();
        Map<String, Matrix4f> boneOffsetMap = new HashMap<>();
        Map<Integer, Map<String, Float>> weightMap = new HashMap<>();

        AIScene aiScene = aiImportFile(path, flags);
        if (aiScene == null) {
            throw new Exception("Error loading model");
        }

        AINode aiRootNode = aiScene.mRootNode();
        AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(0));

        loadBones(aiScene, aiMesh, requiredNodes, boneOffsetMap, weightMap);
        loadDummyProps(aiScene, requiredNodes);
        loadBoneHierarchy(aiScene, requiredNodes, boneOffsetMap);

        mesh = processMesh(aiMesh, weightMap);

        System.out.println("Mesh loaded in " + (System.currentTimeMillis()-t1) + "ms");

        // Load Bones
        // Load Dummy Props
        // LoadBoneHierarchy

        // LoadVertexData

    }

    private void loadBones(AIScene aiScene, AIMesh aiMesh,
                           Set<String> requiredNodes,
                           Map<String, Matrix4f> boneOffsetMap,
                           Map<Integer, Map<String, Float>> weightMap) throws Exception {
        /*
        for (UINT boneIdx = 0; boneIdx < mesh->mNumBones; ++boneIdx)
        {
            aiBone* bone = mesh->mBones[boneIdx];
		const char* boneName = bone->mName.C_Str();

            Logger::Instance().Log("PROCESS BONE: " + string(boneName), "DBG");

            // Store offsets
            boneOffsetMap[boneName] = Assimp4X4toXM(bone->mOffsetMatrix);
            //m_dbgBoneOffsetMap[boneName] = Assimp4X4toXM(bone->mOffsetMatrix);

            // Store weights
            for (unsigned int weightIdx = 0; weightIdx < bone->mNumWeights; ++weightIdx)
            {
                unsigned int vertexID = bone->mWeights[weightIdx].mVertexId;
                float weight = bone->mWeights[weightIdx].mWeight;
                weightMap[vertexID].push_back(make_pair(boneName, weight));
            }

            // Mark corresponding node as required and traverse its parents
            aiNode* correspondingNode = scene->mRootNode->FindNode(boneName);
            TraverseRequiredParents(correspondingNode, requiredNodes);
        }
         */

        for (int boneIdx = 0; boneIdx < aiMesh.mNumBones(); boneIdx++) {
            AIBone aiBone = AIBone.create(aiMesh.mBones().get(boneIdx));
            String aiBoneName = aiBone.mName().dataString();

            System.out.println("Processing bone: " + aiBoneName);

            // Store offset
            boneOffsetMap.put(aiBoneName, toMatrix(aiBone.mOffsetMatrix()));

            // Store weights
            for (int weightIdx = 0; weightIdx < aiBone.mNumWeights(); weightIdx++) {
                int vertexID = aiBone.mWeights().get(weightIdx).mVertexId();
                float weight = aiBone.mWeights().get(weightIdx).mWeight();

                Map<String, Float> weights = weightMap.computeIfAbsent(vertexID, (e) -> new LinkedHashMap<>());
                weights.put(aiBoneName, weight);
            }

            AINode aiNode = findNode(aiScene.mRootNode(), aiBoneName);
            traverseRequiredParents(aiNode, requiredNodes);
        }
    }
    private void loadDummyProps(AIScene aiScene, Set<String> requiredNodes){
        Queue<AINode> queue = new LinkedList<>();
        queue.offer(aiScene.mRootNode());

        while(!queue.isEmpty()){
            AINode node = queue.poll();

            if(node.mName().dataString().contains("Dummy")){
                traverseRequiredParents(node, requiredNodes);
            }

            for (int i = 0; i < node.mNumChildren(); i++) {
                AINode child = AINode.create(node.mChildren().get(i));
                queue.offer(child);
            }
        }

    }
    private void loadBoneHierarchy(AIScene aiScene, Set<String> requiredNodes, Map<String, Matrix4f> boneOffsetMap) throws Exception {
        Queue<AINode> skeletonQueue = new LinkedList<>();
        skeletonQueue.offer(aiScene.mRootNode());

        int boneHierarchyCounter = 0;
        while(!skeletonQueue.isEmpty()){
            AINode node = skeletonQueue.poll();

            String boneName = node.mName().dataString();
            boneIDMap.put(boneName, boneHierarchyCounter);
            System.out.println("Set bone id '" + boneName + "' to " + boneHierarchyCounter);

            AINode parent = node.mParent();
            if(parent == null) {
                boneHierarchy.add(0);
            }
            else{
                int parentID = boneIDMap.get(parent.mName().dataString());
                boneHierarchy.add(parentID);
            }

            Matrix4f matBoneOffset = boneOffsetMap.get(boneName);
            if(matBoneOffset == null){
                matBoneOffset = new Matrix4f(); //identity matrix
                boneOffsets.add(matBoneOffset);
                nodeTransforms.add(matBoneOffset);
            }
            else{
                boneOffsets.add(matBoneOffset);

                Matrix4f transOffsetMatrix = toMatrix(node.mTransformation());
                // TODO: Transpose?
                transOffsetMatrix = transOffsetMatrix.transpose();
                nodeTransforms.add(transOffsetMatrix);
            }

            // Add children to the queue that are marked as required
            for (int i = 0; i < node.mNumChildren(); i++) {
                AINode aiChildNode = AINode.create(node.mChildren().get(i));
                if(requiredNodes.contains(aiChildNode.mName().dataString())){
                    skeletonQueue.offer(aiChildNode);
                }
            }

            ++boneHierarchyCounter;
        }

    }

    private static void traverseRequiredParents(AINode child, Set<String> requiredNodes){
        Queue<AINode> queue = new LinkedList<>();
        queue.offer(child);

        while(!queue.isEmpty()){
            AINode node = queue.poll();
            requiredNodes.add(node.mName().dataString());

            AINode parent = node.mParent();
            if(parent != null) {
                queue.offer(parent);
            }
        }

    }
    private static AINode findNode(AINode aiRootNode, String name){
        AINode target = null;

        if(name.equals(aiRootNode.mName().dataString())){
            return aiRootNode;
        }
        else{
            for (int i = 0; i < aiRootNode.mNumChildren(); i++) {
                AINode aiChildNode = AINode.create(aiRootNode.mChildren().get(i));
                target = findNode(aiChildNode, name);
                if(target != null){
                    return target;
                }
            }
        }

        // Not found...
        return target;
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

    private Mesh processMesh(AIMesh aiMesh, Map<Integer, Map<String, Float>> weightMap) throws Exception {
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
        processBones(aiMesh, weightMap, /*boneList,*/ boneIds, weights);
        if ( textures.size() == 0) {
            int numElements = (vertices.size() / 3) * 2;
            for (int i=0; i<numElements; i++) {
                textures.add(0.0f);
            }
        }

        Mesh mesh = new Mesh(Utils.listToArray(vertices), Utils.listToArray(textures),
                Utils.listToArray(normals), Utils.listIntToArray(indices),
                Utils.listIntToArray(boneIds), Utils.listToArray(weights));
        Material material = new Material();
        mesh.setMaterial(material);

        return mesh;
    }

    protected static void processVertices(AIMesh aiMesh, List<Float> vertices) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            vertices.add(aiVertex.x());
            vertices.add(aiVertex.y());
            vertices.add(aiVertex.z());
        }
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
    private void processBones(AIMesh aiMesh, Map<Integer, Map<String, Float>> weightMap, List<Integer> boneIds, List<Float> weights) {
        /*
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
         */

        int numVertices = aiMesh.mNumVertices();
        for (int i = 0; i < numVertices; i++) {
            /*
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
             */
            Map<String, Float> vertexWeights = weightMap.get(i);
            int weightCount = 0;
            for(Map.Entry<String, Float> entry : vertexWeights.entrySet()){
                weights.add(entry.getValue());
                boneIds.add(boneIDMap.get(entry.getKey()));
                ++weightCount;
            }
            while(weightCount < Mesh.MAX_WEIGHTS){
                weights.add(0.0f);
                boneIds.add(0);
                ++weightCount;
            }

        }
    }




    private static class KeyFrameTranslation{
        long timePos = -1L;
        Vector3f translation = new Vector3f();
    }
    private static class KeyFrameRotation{
        long timePos = -1L;
        Quaternionf qrotation = new Quaternionf();
    }
    private static class KeyFrameScale{
        long timePos = -1L;
        Vector3f scale = new Vector3f();
    }
    private static class BoneAnimation{
        String jointName = "";
        List<KeyFrameTranslation> translations = new ArrayList<>();
        List<KeyFrameRotation> rotations = new ArrayList<>();
        List<KeyFrameScale> scales = new ArrayList<>();

        public Matrix4f interpolate(long time, long duration){
            // Get translation(t)
            double t = (double)time;
            double dur = (double)duration;

            int transIdx = (int)( ((double)translations.size()) / dur * t );
            KeyFrameTranslation trans = translations.get(transIdx);

            int rotIdx = (int)( ((double)rotations.size()) / dur * t );
            KeyFrameRotation rot = rotations.get(rotIdx);

            int scaleIdx = (int)( ((double)scales.size()) / dur * t );
            KeyFrameScale scale = scales.get(scaleIdx);

            Matrix4f transfMat = new Matrix4f();
            transfMat.translate(trans.translation);
            transfMat.rotate(rot.qrotation);
            transfMat.scale(scale.scale);
            return transfMat;
        }
    }
    public static class AnimationClip{
        public String name;
        public long duration;
        public List<BoneAnimation> boneAnimations = new ArrayList<>();

        public Matrix4f[] interpolate(long t){
            Matrix4f[] transforms = new Matrix4f[boneAnimations.size()];
            int i = 0;
            for (BoneAnimation boneAnimation : boneAnimations){
                transforms[i] = boneAnimation.interpolate(t, duration);
                ++i;
            }
            return transforms;
        }

    }

    public Matrix4f[] getFinalTransforms(String animationName, long t){
        AnimationClip clip = getAnimationClip(animationName);

        int numBones = boneOffsets.size();

        Matrix4f[] jointMatrices = new Matrix4f[MAX_JOINTS];
        Arrays.fill(jointMatrices, IDENTITY_MATRIX);

        // Interpolate all the bones of this clip at the given moment in time.
        Matrix4f[] toParentTransforms = clip.interpolate(t);

        assert(numBones==toParentTransforms.length);

        //
        // Traverse the hierarchy and transform all the bones to the root space.
        //

        Matrix4f[] toRootTransforms =  new Matrix4f[numBones];

        // The root bone has index 0. The root bone has no parent, so its toRootTransform
        // is just its local bone transform.
        toRootTransforms[0] = toParentTransforms[0];

        // Now calculate the toRootTransform of all children.
        for (int i = 1; i < numBones; i++) {
            Matrix4f toParent = new Matrix4f(toParentTransforms[i]);

            int parentIdx = boneHierarchy.get(i);
            Matrix4f parentToRoot = new Matrix4f(toRootTransforms[parentIdx]);

            Matrix4f toRoot = toParent.mul(parentToRoot);
            toRootTransforms[i] = toRoot;
        }

        // Premultiply by the bone offset transform to get the final transform.
        for (int i = 0; i < numBones; i++) {
            Matrix4f offset = new Matrix4f(boneOffsets.get(i));
            Matrix4f toRoot = new Matrix4f(toRootTransforms[i]);
            jointMatrices[i] = offset.mul(toRoot);
        }


        //for (int i = 0; i < toRootTransforms.length; i++) {
        //    jointMatrices[i] = toRootTransforms[i];
        //}
        return jointMatrices;
    }

    public void loadAnimation(String animationName, String path, int flags) throws Exception {
        List<AnimatedFrame> frames = new ArrayList<>();
        long duration = 0L;

        AIScene aiScene = aiImportFile(path, flags);
        if (aiScene == null) {
            throw new Exception("Error loading model");
        }
        AINode aiRootNode = aiScene.mRootNode();

        AIAnimation aiAnimation = AIAnimation.create(aiScene.mAnimations().get(0));

        // Calculate duration in ms
        double aiDuration = aiAnimation.mDuration();
        double aiTicksPerSecond = aiAnimation.mTicksPerSecond();
        duration = (long)((1000.0 / aiTicksPerSecond) * aiDuration);

        // Load animation

        Map<Integer, BoneAnimation> boneAnimationMap = new LinkedHashMap<>();

        if(aiAnimation.mNumChannels() > 0){
            for (int boneChannelIdx = 0; boneChannelIdx < aiAnimation.mNumChannels(); boneChannelIdx++) {
                AINodeAnim boneAnim = AINodeAnim.create(aiAnimation.mChannels().get(boneChannelIdx));
                String jointName = boneAnim.mNodeName().dataString();

                if(jointName.equals("RigPelvis_$AssimpFbx$_Scaling")){
                    //continue;
                }

                // Fix different rig names
                //if(jointName.equals("RigNeck")){
                //    jointName = "RigNeck1";
                //}

                if(jointName.equals("RigNeck")){
                    System.out.println("Joint 'RigNeck' gefunden. Soll das in RigNeck1 umbenannt werden?");
                }

                if(!boneIDMap.containsKey(jointName)){
                    System.out.println("Skipping irrelevant joint: '" + jointName + "'");
                }
                else{
                    BoneAnimation boneAnimation = new BoneAnimation();
                    boneAnimation.jointName=jointName;

                    int translationKeys = boneAnim.mNumPositionKeys();
                    int rotationKeys = boneAnim.mNumRotationKeys();
                    int scalingKeys = boneAnim.mNumScalingKeys();

                    // translation
                    for (int animKey = 0; animKey < translationKeys; animKey++) {
                        KeyFrameTranslation trans = new KeyFrameTranslation();

                        double time = boneAnim.mPositionKeys().get(animKey).mTime();
                        trans.timePos = (long)((1000.0 / aiTicksPerSecond) * time);
                        AIVector3D val = boneAnim.mPositionKeys().get(animKey).mValue();

                        Vector3f vTrans;
                        /*
                        if(jointName.equals("RigPelvis_$AssimpFbx$_Translation")){
                            vTrans = new Vector3f(val.x(), val.z(), -val.y());
                        }
                        else{
                            vTrans = new Vector3f(val.x(), val.y(), val.z());
                        }
                         */
                        vTrans = new Vector3f(val.x(), val.y(), val.z());
                        trans.translation = vTrans;
                        boneAnimation.translations.add(trans);
                    }

                    // rotation
                    for (int animKey = 0; animKey < rotationKeys; animKey++) {
                        KeyFrameRotation rot = new KeyFrameRotation();

                        double time = boneAnim.mRotationKeys().get(animKey).mTime();
                        rot.timePos = (long)((1000.0 / aiTicksPerSecond) * time);
                        AIQuaternion val = boneAnim.mRotationKeys().get(animKey).mValue();

                        Quaternionf qRot = new Quaternionf(val.x(), val.y(), val.z(), val.z());
                        rot.qrotation = qRot;
                        boneAnimation.rotations.add(rot);
                    }

                    // scale
                    for (int animKey = 0; animKey < scalingKeys; animKey++) {
                        KeyFrameScale scale = new KeyFrameScale();

                        double time = boneAnim.mScalingKeys().get(animKey).mTime();
                        scale.timePos = (long)((1000.0 / aiTicksPerSecond) * time);
                        AIVector3D val = boneAnim.mScalingKeys().get(animKey).mValue();

                        Vector3f vScale = new Vector3f(val.x(), val.y(), val.z());
                        scale.scale = vScale;
                        boneAnimation.scales.add(scale);
                    }

                    boneAnimationMap.put(boneIDMap.get(jointName), boneAnimation);
                }
            }
        }

        AnimationClip clip = new AnimationClip();
        clip.name = animationName;
        clip.duration = duration;

        //for(Integer boneID : boneIDMap.values()){
        for (int i = 0; i < boneIDMap.size(); i++) {
            int boneID = i;

            BoneAnimation animation = boneAnimationMap.get(boneID);
            if(animation == null){
                animation = new BoneAnimation();
                animation.translations.add(new KeyFrameTranslation());
                animation.rotations.add(new KeyFrameRotation());
                animation.scales.add(new KeyFrameScale());
            }
            clip.boneAnimations.add(animation);
        }

        animationClips.put(animationName, clip);

    }
}
