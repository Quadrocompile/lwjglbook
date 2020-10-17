package org.lwjglb.engine.graph;

import java.lang.Math;
import java.util.ArrayList;

import org.joml.*;
import org.lwjglb.engine.graph.LHMP.LHMPBaseMesh;
import org.lwjglb.engine.graph.LHMP.LHMPGameItem;
import org.lwjglb.engine.graph.lights.SpotLight;
import org.lwjglb.engine.graph.lights.PointLight;
import org.lwjglb.engine.graph.lights.DirectionalLight;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import org.lwjglb.engine.items.GameItem;
import org.lwjglb.engine.Scene;
import org.lwjglb.engine.SceneLight;
import org.lwjglb.engine.items.SkyBox;
import org.lwjglb.engine.Utils;
import org.lwjglb.engine.Window;
import org.lwjglb.engine.graph.anim.AnimGameItem;
import org.lwjglb.engine.graph.anim.AnimatedFrame;
import org.lwjglb.engine.graph.particles.IParticleEmitter;
import org.lwjglb.engine.graph.shadow.ShadowCascade;
import org.lwjglb.engine.graph.shadow.ShadowRenderer;
import org.lwjglb.game.DummyGame;

public class Renderer {

    private static final int MAX_POINT_LIGHTS = 5;

    private static final int MAX_SPOT_LIGHTS = 5;

    private final Transformation transformation;

    private final ShadowRenderer shadowRenderer;

    private ShaderProgram sceneShaderProgram;

    private ShaderProgram skyBoxShaderProgram;

    private ShaderProgram particlesShaderProgram;

    private final float specularPower;

    private final FrustumCullingFilter frustumFilter;

    private final List<GameItem> filteredItems;

    public Renderer() {
        transformation = new Transformation();
        specularPower = 10f;
        shadowRenderer = new ShadowRenderer();
        frustumFilter = new FrustumCullingFilter();
        filteredItems = new ArrayList<>();
    }

    public void init(Window window) throws Exception {
        shadowRenderer.init(window);
        setupSkyBoxShader();
        setupSceneShader();
        setupParticlesShader();
    }

    public void render(Window window, Camera camera, Scene scene, boolean sceneChanged) {
        clear();

        if (window.getOptions().frustumCulling) {
            frustumFilter.updateFrustum(window.getProjectionMatrix(), camera.getViewMatrix());
            frustumFilter.filter(scene.getGameMeshes());
            frustumFilter.filter(scene.getGameInstancedMeshes());
        }

        // Render depth map before view ports has been set up
        if (scene.isRenderShadows() && sceneChanged) {
            shadowRenderer.render(window, scene, camera, transformation, this);
        }

        glViewport(0, 0, window.getWidth(), window.getHeight());

        // Update projection matrix once per render cycle
        window.updateProjectionMatrix();

        renderScene(window, camera, scene);
        renderSkyBox(window, camera, scene);
        renderParticles(window, camera, scene);

        //renderAxes(camera);
        //renderCrossHair(window);
    }

    private void setupParticlesShader() throws Exception {
        particlesShaderProgram = new ShaderProgram();
        particlesShaderProgram.createVertexShader(Utils.loadResource("/shaders/particles_vertex.vs"));
        particlesShaderProgram.createFragmentShader(Utils.loadResource("/shaders/particles_fragment.fs"));
        particlesShaderProgram.link();

        particlesShaderProgram.createUniform("viewMatrix");
        particlesShaderProgram.createUniform("projectionMatrix");
        particlesShaderProgram.createUniform("texture_sampler");

        particlesShaderProgram.createUniform("numCols");
        particlesShaderProgram.createUniform("numRows");
    }

    private void setupSkyBoxShader() throws Exception {
        skyBoxShaderProgram = new ShaderProgram();
        skyBoxShaderProgram.createVertexShader(Utils.loadResource("/shaders/sb_vertex.vs"));
        skyBoxShaderProgram.createFragmentShader(Utils.loadResource("/shaders/sb_fragment.fs"));
        skyBoxShaderProgram.link();

        // Create uniforms for projection matrix
        skyBoxShaderProgram.createUniform("projectionMatrix");
        skyBoxShaderProgram.createUniform("modelViewMatrix");
        skyBoxShaderProgram.createUniform("texture_sampler");
        skyBoxShaderProgram.createUniform("ambientLight");
        skyBoxShaderProgram.createUniform("colour");
        skyBoxShaderProgram.createUniform("hasTexture");
    }

    private void setupSceneShader() throws Exception {
        // Create shader
        sceneShaderProgram = new ShaderProgram();
        sceneShaderProgram.createVertexShader(Utils.loadResource("/shaders/scene_vertex.vs"));
        sceneShaderProgram.createFragmentShader(Utils.loadResource("/shaders/scene_fragment.fs"));
        sceneShaderProgram.link();

        // Create uniforms for view and projection matrices
        sceneShaderProgram.createUniform("viewMatrix");
        sceneShaderProgram.createUniform("projectionMatrix");
        sceneShaderProgram.createUniform("texture_sampler");
        sceneShaderProgram.createUniform("normalMap");
        // Create uniform for material
        sceneShaderProgram.createMaterialUniform("material");
        // Create lighting related uniforms
        sceneShaderProgram.createUniform("specularPower");
        sceneShaderProgram.createUniform("ambientLight");
        sceneShaderProgram.createPointLightListUniform("pointLights", MAX_POINT_LIGHTS);
        sceneShaderProgram.createSpotLightListUniform("spotLights", MAX_SPOT_LIGHTS);
        sceneShaderProgram.createDirectionalLightUniform("directionalLight");
        sceneShaderProgram.createFogUniform("fog");

        // Create uniforms for shadow mapping
        for (int i = 0; i < ShadowRenderer.NUM_CASCADES; i++) {
            sceneShaderProgram.createUniform("shadowMap_" + i);
        }
        sceneShaderProgram.createUniform("orthoProjectionMatrix", ShadowRenderer.NUM_CASCADES);
        sceneShaderProgram.createUniform("modelNonInstancedMatrix");
        sceneShaderProgram.createUniform("lightViewMatrix", ShadowRenderer.NUM_CASCADES);
        sceneShaderProgram.createUniform("cascadeFarPlanes", ShadowRenderer.NUM_CASCADES);
        sceneShaderProgram.createUniform("renderShadow");

        // Create uniform for joint matrices
        sceneShaderProgram.createUniform("jointsMatrix");

        sceneShaderProgram.createUniform("isInstanced");
        sceneShaderProgram.createUniform("numCols");
        sceneShaderProgram.createUniform("numRows");

        sceneShaderProgram.createUniform("selectedNonInstanced");
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    private void renderParticles(Window window, Camera camera, Scene scene) {
        particlesShaderProgram.bind();

        Matrix4f viewMatrix = camera.getViewMatrix();
        particlesShaderProgram.setUniform("viewMatrix", viewMatrix);
        particlesShaderProgram.setUniform("texture_sampler", 0);
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        particlesShaderProgram.setUniform("projectionMatrix", projectionMatrix);

        IParticleEmitter[] emitters = scene.getParticleEmitters();
        int numEmitters = emitters != null ? emitters.length : 0;

        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        for (int i = 0; i < numEmitters; i++) {
            IParticleEmitter emitter = emitters[i];
            InstancedMesh mesh = (InstancedMesh) emitter.getBaseParticle().getMesh();

            Texture text = mesh.getMaterial().getTexture();
            particlesShaderProgram.setUniform("numCols", text.getNumCols());
            particlesShaderProgram.setUniform("numRows", text.getNumRows());

            mesh.renderListInstanced(emitter.getParticles(), true, transformation, viewMatrix);
        }

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(true);

        particlesShaderProgram.unbind();
    }

    private void renderSkyBox(Window window, Camera camera, Scene scene) {
        SkyBox skyBox = scene.getSkyBox();
        if (skyBox != null) {
            skyBoxShaderProgram.bind();

            skyBoxShaderProgram.setUniform("texture_sampler", 0);

            Matrix4f projectionMatrix = window.getProjectionMatrix();
            skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix);
            Matrix4f viewMatrix = camera.getViewMatrix();
            float m30 = viewMatrix.m30();
            viewMatrix.m30(0);
            float m31 = viewMatrix.m31();
            viewMatrix.m31(0);
            float m32 = viewMatrix.m32();
            viewMatrix.m32(0);

            Mesh mesh = skyBox.getMesh();
            Matrix4f modelViewMatrix = transformation.buildModelViewMatrix(skyBox, viewMatrix);
            skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
            skyBoxShaderProgram.setUniform("ambientLight", scene.getSceneLight().getSkyBoxLight());
            skyBoxShaderProgram.setUniform("colour", mesh.getMaterial().getAmbientColour());
            skyBoxShaderProgram.setUniform("hasTexture", mesh.getMaterial().isTextured() ? 1 : 0);

            mesh.render();


            viewMatrix.m30(m30);
            viewMatrix.m31(m31);
            viewMatrix.m32(m32);
            skyBoxShaderProgram.unbind();
        }
    }

    public void renderScene(Window window, Camera camera, Scene scene) {
        sceneShaderProgram.bind();

        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = window.getProjectionMatrix();
        sceneShaderProgram.setUniform("viewMatrix", viewMatrix);
        sceneShaderProgram.setUniform("projectionMatrix", projectionMatrix);

        List<ShadowCascade> shadowCascades = shadowRenderer.getShadowCascades();
        for (int i = 0; i < ShadowRenderer.NUM_CASCADES; i++) {
            ShadowCascade shadowCascade = shadowCascades.get(i);
            sceneShaderProgram.setUniform("orthoProjectionMatrix", shadowCascade.getOrthoProjMatrix(), i);
            sceneShaderProgram.setUniform("cascadeFarPlanes", ShadowRenderer.CASCADE_SPLITS[i], i);
            sceneShaderProgram.setUniform("lightViewMatrix", shadowCascade.getLightViewMatrix(), i);
        }

        SceneLight sceneLight = scene.getSceneLight();
        renderLights(viewMatrix, sceneLight);

        sceneShaderProgram.setUniform("fog", scene.getFog());
        sceneShaderProgram.setUniform("texture_sampler", 0);
        sceneShaderProgram.setUniform("normalMap", 1);
        int start = 2;
        for (int i = 0; i < ShadowRenderer.NUM_CASCADES; i++) {
            sceneShaderProgram.setUniform("shadowMap_" + i, start + i);
        }
        sceneShaderProgram.setUniform("renderShadow", scene.isRenderShadows() ? 1 : 0);

        renderNonInstancedMeshes(scene);

        renderInstancedMeshes(scene, viewMatrix);

        sceneShaderProgram.unbind();
    }

    private Matrix4f joint2H = null;
    private int switchJointCount = 0;
    private int jointCounter = 0;


    private long start = 0L;
    private int iOffX = 0;
    private int iOffY = 0;
    private int iOffZ = 0;

    private double rotAngle = 0.0;
    private void renderNonInstancedMeshes(Scene scene) {
        sceneShaderProgram.setUniform("isInstanced", 0);

        // Render LHMP
        //
        {
            if(joint2H != null) {
                Mesh mesh = DummyGame.baseMesh.mesh;
                Texture tex = DummyGame.baseMeshTestTexture;
                Material mat = new Material(tex);
                sceneShaderProgram.setUniform("material", mat);
                sceneShaderProgram.setUniform("numCols", tex.getNumCols());
                sceneShaderProgram.setUniform("numRows", tex.getNumRows());
                shadowRenderer.bindTextures(GL_TEXTURE2);

                sceneShaderProgram.setUniform("selectedNonInstanced", 0.0f);

                Matrix4f modelMatrix = new Matrix4f();
                sceneShaderProgram.setUniform("modelNonInstancedMatrix", modelMatrix);

            }
        }




        // Render each mesh with the associated game Items
        Map<Mesh, List<GameItem>> mapMeshes = scene.getGameMeshes();
        for (Mesh mesh : mapMeshes.keySet()) {
            sceneShaderProgram.setUniform("material", mesh.getMaterial());

            Texture text = mesh.getMaterial().getTexture();
            if (text != null) {
                sceneShaderProgram.setUniform("numCols", text.getNumCols());
                sceneShaderProgram.setUniform("numRows", text.getNumRows());
            }

            shadowRenderer.bindTextures(GL_TEXTURE2);

            mesh.renderList(mapMeshes.get(mesh), (GameItem gameItem) -> {
                sceneShaderProgram.setUniform("selectedNonInstanced", gameItem.isSelected() ? 1.0f : 0.0f);

                float dist = 2.5f;

                if (gameItem instanceof AnimGameItem) {

                    if( ((AnimGameItem)gameItem).getID().equals("2HKnight") ){
                        rotAngle += 0.001;
                        Quaternionf rot = new Quaternionf(new AxisAngle4d(rotAngle, 0.0, 1.0, 0.0));
                        rot = rot.mul(new Quaternionf(new AxisAngle4d(-1.5708, 1.0, 0.0, 0.0)));
                        //gameItem.setRotation(rot);
                        gameItem.setPosition((float)(Math.sin(rotAngle - 3.14159f))*dist, 0.0f, (float)(Math.cos(rotAngle - 3.14159f))*dist);
                    }
                    else if( ((AnimGameItem)gameItem).getID().equals("1HKnight") ){
                        double counterRotAngle = rotAngle + 3.14159f;
                        Quaternionf rot = new Quaternionf(new AxisAngle4d(counterRotAngle, 0.0, 1.0, 0.0));
                        rot = rot.mul(new Quaternionf(new AxisAngle4d(-1.5708, 1.0, 0.0, 0.0)));
                        //gameItem.setRotation(rot);
                        //gameItem.setPosition((float)(Math.sin(counterRotAngle - 3.14159f))*dist, 0.0f, (float)(Math.cos(counterRotAngle - 3.14159f))*dist);
                        gameItem.setPosition(0.0f, 0.0f, 0.0f);
                    }
                    else if( ((AnimGameItem)gameItem).getID().equals("2HKnight2") ){
                        double counterRotAngle = rotAngle + 3.14159f;
                        Quaternionf rot = new Quaternionf(new AxisAngle4d(-1.5708, 1.0, 0.0, 0.0));
                        //gameItem.setRotation(rot);
                        gameItem.setPosition(0.0f, 0.0f, 0.0f);
                    }
                }

                Matrix4f modelMatrix; // = transformation.buildModelMatrix(gameItem);
                if(gameItem.getId().equals("2HSword2")){

                    /*
                    Quaternionf rot = new Quaternionf(new AxisAngle4d(rotAngle, 0.0, 1.0, 0.0));

                    rot = rot.mul(new Quaternionf(new AxisAngle4d(-1.5708, 1.0, 0.0, 0.0)));
                    gameItem.setRotation(rot);
                    gameItem.setPosition((float)(Math.sin(rotAngle - 3.14159f))*dist, 0.5f, (float)(Math.cos(rotAngle - 3.14159f))*dist);
                    */

                    //gameItem.setPosition(DummyGame.offX,DummyGame.offY,DummyGame.offZ);
                    //gameItem.setPosition(-0.9199994f *1.0f, 0.35999992f*1.0f, -0.43999985f*1.0f);
                    //gameItem.setPosition(-0.6099997f, 1.3299991f, 0.0f);

                    final double deg90 = -1.5708;
                    //double angle = -1.5708; // 90 deg in rad

                    if(start == 0L){
                        start = System.currentTimeMillis();
                        iOffX = 3;
                        iOffY = 3;
                        iOffZ = 2;

                        DummyGame.offX = 0;
                        DummyGame.offY = 0;
                        DummyGame.offZ = 0;
                    }
                    if(System.currentTimeMillis()-start > 50000L){
                        ++iOffX;
                        if(iOffX == 4){
                            iOffX = 0;
                            ++iOffY;
                        }
                        if(iOffY == 4){
                            iOffY = 0;
                            ++iOffZ;
                        }
                        if(iOffZ == 4){
                            iOffZ = 0;
                        }
                        start = System.currentTimeMillis();
                        System.out.println(iOffX + "|" + iOffY + "|" + iOffZ);

                        /*
                        iOffX = 0;
                        iOffY = 0;
                        iOffZ = 2;
                         */
                    }


/*
                    Quaternionf rot = new Quaternionf(new AxisAngle4d(deg90 * ((float)(Math.round(DummyGame.offX)%4)), 1.0, 0.0, 0.0));
                    rot = rot.mul(new Quaternionf(new AxisAngle4d(deg90 * ((float)(Math.round(DummyGame.offY)%4)), 0.0, 1.0, 0.0)));
                    rot = rot.mul(new Quaternionf(new AxisAngle4d(deg90 * ((float)(Math.round(DummyGame.offZ)%4)), 0.0, 0.0, 1.0)));
                    gameItem.setRotation(rot);

 */

                    Quaternionf rot = new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffX)), 1.0, 0.0, 0.0));
                    rot = rot.mul(new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffY)), 0.0, 1.0, 0.0)));
                    rot = rot.mul(new Quaternionf(new AxisAngle4d(deg90 * ((float)(iOffZ)), 0.0, 0.0, 1.0)));
                    gameItem.setRotation(rot);

                    modelMatrix = transformation.buildModelMatrix(gameItem);

                    if(joint2H != null) {
                        //Matrix4f trans = joint2H.mul(modelMatrix); // modelMatrix.mul(joint2H);
                        Matrix4f trans = modelMatrix.mul(joint2H);
                        modelMatrix = trans;
                    }
                }
                else{
                    modelMatrix = transformation.buildModelMatrix(gameItem);
                }

                sceneShaderProgram.setUniform("modelNonInstancedMatrix", modelMatrix);
                if (gameItem instanceof AnimGameItem) {
                    AnimGameItem animGameItem = (AnimGameItem) gameItem;
                    AnimatedFrame frame = animGameItem.getCurrentAnimation().getCurrentFrame();
                    sceneShaderProgram.setUniform("jointsMatrix", frame.getJointMatrices());

                    //if( ((AnimGameItem)gameItem).getID().equals("2HKnight2") ){
                    if( ((AnimGameItem)gameItem).getID().equals("1HKnight") ){
                        /*
                        switchJointCount++;
                        if(switchJointCount > 100){
                            switchJointCount = 0;
                            jointCounter++;
                            System.out.println("Counter: " + jointCounter);
                            if(jointCounter > 17){
                                jointCounter = 0;
                            }
                        }
                        */
                        joint2H = frame.getJointMatrices()[5]; // 11
                    }
                }
                else if(gameItem instanceof LHMPGameItem){
                    LHMPGameItem lhmp = (LHMPGameItem)gameItem;

                    LHMPBaseMesh.AnimationClip clip = lhmp.getCurrentAnimationClip();

                    long now = System.currentTimeMillis();
                    long div0 = now / clip.duration;
                    long t = now - (clip.duration*div0);


                    LHMPBaseMesh baseMesh = DummyGame.baseMesh;
                    Matrix4f[] finalTransforms = baseMesh.getFinalTransforms("Jump", t);

                    Matrix4f[] identityJoints = new Matrix4f[150];
                    for (int i = 0; i < identityJoints.length; i++) {
                        identityJoints[i] = new Matrix4f();
                    }
                    finalTransforms = identityJoints;

                    sceneShaderProgram.setUniform("jointsMatrix", finalTransforms);
                }

            }
            );
        }
    }

    private static void rotateJointMatrices(Matrix4f[] jointMatrices){
        Quaternionf rot = new Quaternionf(new AxisAngle4d(-1.5708, 1.0, 0.0, 0.0));
        //gameItem.setRotation(rot);
        //gameItem.setPosition(0.0f, 0.0f, 0.0f);
    }

    private void renderInstancedMeshes(Scene scene, Matrix4f viewMatrix) {
        sceneShaderProgram.setUniform("isInstanced", 1);

        // Render each mesh with the associated game Items
        Map<InstancedMesh, List<GameItem>> mapMeshes = scene.getGameInstancedMeshes();
        for (InstancedMesh mesh : mapMeshes.keySet()) {
            Texture text = mesh.getMaterial().getTexture();
            if (text != null) {
                sceneShaderProgram.setUniform("numCols", text.getNumCols());
                sceneShaderProgram.setUniform("numRows", text.getNumRows());
            }

            sceneShaderProgram.setUniform("material", mesh.getMaterial());

            filteredItems.clear();
            for (GameItem gameItem : mapMeshes.get(mesh)) {
                if (gameItem.isInsideFrustum()) {
                    filteredItems.add(gameItem);
                }
            }
            shadowRenderer.bindTextures(GL_TEXTURE2);

            mesh.renderListInstanced(filteredItems, transformation, viewMatrix);
        }
    }

    private void renderLights(Matrix4f viewMatrix, SceneLight sceneLight) {

        sceneShaderProgram.setUniform("ambientLight", sceneLight.getAmbientLight());
        sceneShaderProgram.setUniform("specularPower", specularPower);

        // Process Point Lights
        PointLight[] pointLightList = sceneLight.getPointLightList();
        int numLights = pointLightList != null ? pointLightList.length : 0;
        for (int i = 0; i < numLights; i++) {
            // Get a copy of the point light object and transform its position to view coordinates
            PointLight currPointLight = new PointLight(pointLightList[i]);
            Vector3f lightPos = currPointLight.getPosition();
            Vector4f aux = new Vector4f(lightPos, 1);
            aux.mul(viewMatrix);
            lightPos.x = aux.x;
            lightPos.y = aux.y;
            lightPos.z = aux.z;
            sceneShaderProgram.setUniform("pointLights", currPointLight, i);
        }

        // Process Spot Ligths
        SpotLight[] spotLightList = sceneLight.getSpotLightList();
        numLights = spotLightList != null ? spotLightList.length : 0;
        for (int i = 0; i < numLights; i++) {
            // Get a copy of the spot light object and transform its position and cone direction to view coordinates
            SpotLight currSpotLight = new SpotLight(spotLightList[i]);
            Vector4f dir = new Vector4f(currSpotLight.getConeDirection(), 0);
            dir.mul(viewMatrix);
            currSpotLight.setConeDirection(new Vector3f(dir.x, dir.y, dir.z));

            Vector3f lightPos = currSpotLight.getPointLight().getPosition();
            Vector4f aux = new Vector4f(lightPos, 1);
            aux.mul(viewMatrix);
            lightPos.x = aux.x;
            lightPos.y = aux.y;
            lightPos.z = aux.z;

            sceneShaderProgram.setUniform("spotLights", currSpotLight, i);
        }

        // Get a copy of the directional light object and transform its position to view coordinates
        DirectionalLight currDirLight = new DirectionalLight(sceneLight.getDirectionalLight());
        Vector4f dir = new Vector4f(currDirLight.getDirection(), 0);
        dir.mul(viewMatrix);
        currDirLight.setDirection(new Vector3f(dir.x, dir.y, dir.z));
        sceneShaderProgram.setUniform("directionalLight", currDirLight);
    }

    private void renderCrossHair(Window window) {
        if (window.getWindowOptions().compatibleProfile) {
            glPushMatrix();
            glLoadIdentity();

            float inc = 0.05f;
            glLineWidth(2.0f);

            glBegin(GL_LINES);

            glColor3f(1.0f, 1.0f, 1.0f);

            // Horizontal line
            glVertex3f(-inc, 0.0f, 0.0f);
            glVertex3f(+inc, 0.0f, 0.0f);
            glEnd();

            // Vertical line
            glBegin(GL_LINES);
            glVertex3f(0.0f, -inc, 0.0f);
            glVertex3f(0.0f, +inc, 0.0f);
            glEnd();

            glPopMatrix();
        }
    }

    /**
     * Renders the three axis in space (For debugging purposes only
     *
     * @param camera
     */
    private void renderAxes(Window window, Camera camera) {
        Window.WindowOptions opts = window.getWindowOptions();
        if (opts.compatibleProfile) {
            glPushMatrix();
            glLoadIdentity();
            float rotX = camera.getRotation().x;
            float rotY = camera.getRotation().y;
            float rotZ = 0;
            glRotatef(rotX, 1.0f, 0.0f, 0.0f);
            glRotatef(rotY, 0.0f, 1.0f, 0.0f);
            glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
            glLineWidth(2.0f);

            glBegin(GL_LINES);
            // X Axis
            glColor3f(1.0f, 0.0f, 0.0f);
            glVertex3f(0.0f, 0.0f, 0.0f);
            glVertex3f(1.0f, 0.0f, 0.0f);
            // Y Axis
            glColor3f(0.0f, 1.0f, 0.0f);
            glVertex3f(0.0f, 0.0f, 0.0f);
            glVertex3f(0.0f, 1.0f, 0.0f);
            // Z Axis
            glColor3f(1.0f, 1.0f, 1.0f);
            glVertex3f(0.0f, 0.0f, 0.0f);
            glVertex3f(0.0f, 0.0f, 1.0f);
            glEnd();

            glPopMatrix();
        }
    }

    public void cleanup() {
        if (shadowRenderer != null) {
            shadowRenderer.cleanup();
        }
        if (skyBoxShaderProgram != null) {
            skyBoxShaderProgram.cleanup();
        }
        if (sceneShaderProgram != null) {
            sceneShaderProgram.cleanup();
        }
        if (particlesShaderProgram != null) {
            particlesShaderProgram.cleanup();
        }
    }
}
