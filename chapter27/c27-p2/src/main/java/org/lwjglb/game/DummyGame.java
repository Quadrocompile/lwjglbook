package org.lwjglb.game;

import org.joml.*;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjglb.engine.IGameLogic;
import org.lwjglb.engine.MouseInput;
import org.lwjglb.engine.Scene;
import org.lwjglb.engine.SceneLight;
import org.lwjglb.engine.Window;
import org.lwjglb.engine.graph.Camera;
import org.lwjglb.engine.graph.LHMP.LHMPBaseMesh;
import org.lwjglb.engine.graph.LHMP.LHMPGameItem;
import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.graph.Renderer;
import org.lwjglb.engine.graph.Texture;
import org.lwjglb.engine.graph.anim.AnimGameItem;
import org.lwjglb.engine.graph.anim.Animation;
import org.lwjglb.engine.graph.lights.DirectionalLight;
import org.lwjglb.engine.graph.weather.Fog;
import org.lwjglb.engine.items.GameItem;
import org.lwjglb.engine.items.SkyBox;
import org.lwjglb.engine.loaders.assimp.AnimMeshesLoader;
import org.lwjglb.engine.loaders.assimp.StaticMeshesLoader;
import org.lwjglb.engine.loaders.assimp.TextureCache;

import java.lang.Math;

public class DummyGame implements IGameLogic {

    private static final float MOUSE_SENSITIVITY = 0.2f;

    private final Vector3f cameraInc;

    private final Renderer renderer;

    private final Camera camera;

    private Scene scene;

    private static final float CAMERA_POS_STEP = 0.40f;

    private float angleInc;

    private float lightAngle;

    private boolean firstTime;

    private boolean sceneChanged;

    private Animation animationDebug;

    private Animation animation1;
    private Animation animation2;

    private AnimGameItem animItemDebug;

    private AnimGameItem animItem1;
    private AnimGameItem animItem2;

    public static float offX = 0.0f;
    public static float offY = 0.0f;
    public static float offZ = 0.0f;

    public static LHMPBaseMesh baseMesh;
    public static Texture baseMeshTestTexture;
    public static LHMPGameItem lhmpGameItem;

    public DummyGame() {
        renderer = new Renderer();
        camera = new Camera();
        cameraInc = new Vector3f(0.0f, 0.0f, 0.0f);
        angleInc = 0;
        lightAngle = 90;
        firstTime = true;
    }

    @Override
    public void init(Window window) throws Exception {
        renderer.init(window);

        scene = new Scene();

        Mesh[] terrainMesh = StaticMeshesLoader.load("models/terrain/terrain.obj", "models/terrain");
        GameItem terrain = new GameItem(terrainMesh);
        terrain.setScale(100.0f);

        // This is the model that is used in the toturial
        //animItem = AnimMeshesLoader.loadAnimGameItem("models/bob/boblamp.md5mesh", "");

        // This works
        //animItem = AnimMeshesLoader.loadAnimGameItem("models/fbx_working/toon_chicken-eat.fbx", "");

        System.out.println("Loading models...");

        baseMesh = new LHMPBaseMesh();
        baseMesh.loadBaseMesh("models/fbx_not_working/Base Normal.FBX", aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices | aiProcess_Triangulate
                | aiProcess_FixInfacingNormals | aiProcess_LimitBoneWeights, "BASEMESH");
        //baseMesh.loadBaseMesh("models/fbx_not_working/Base@Jump.FBX", aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices | aiProcess_Triangulate
        //        | aiProcess_FixInfacingNormals | aiProcess_LimitBoneWeights, "BASEMESH");
        baseMesh.loadAnimation("Jump", "models/fbx_not_working/Base@Jump.FBX", aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices | aiProcess_Triangulate
                | aiProcess_FixInfacingNormals | aiProcess_LimitBoneWeights);

        baseMeshTestTexture = TextureCache.getInstance().getTexture("models/fbx_not_working/Male White Barbarian 03 Black.png");

        lhmpGameItem = new LHMPGameItem(baseMesh, baseMeshTestTexture,  "LHMP");
        lhmpGameItem.setPosition(0.0f, 0.5f, 0.0f);
        lhmpGameItem.setScale(0.01f);
        lhmpGameItem.setcurrentAnimationClip(baseMesh.getAnimationClip("Jump"));

        // This does not :(
        //animItemDebug = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base@Idle.FBX", "@Female White Knight 04 Magenta.png", "2HKnight2");
        //animItemDebug = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base@Melee Right Attack 01.FBX", "@Female White Knight 04 Magenta.png", "2HKnight2");
        animItemDebug = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base@Roll Forward Without Root Motion.FBX", "@Female White Knight 04 Magenta.png", "2HKnight2");
        //animItemDebug = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base@Jump.FBX", "@Female White Knight 04 Magenta.png", "2HKnight2");
        //animItemDebug = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base Normal.FBX", "@Female White Knight 04 Magenta.png", "2HKnight2");

        animItem1 = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base@TH Sword Melee Attack 01.FBX", "@Female White Knight 04 Magenta.png", "2HKnight");
        //animItem2 = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base@Melee Right Attack 01.FBX", "@Male Black Knight 04 White.png", "1HKnight");
        //animItem2 = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base@Melee Right Attack 02.FBX", "@Male Black Knight 04 White.png", "1HKnight");
        animItem2 = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base@Jump.FBX", "@Male Black Knight 04 White.png", "1HKnight");
        //animItem = AnimMeshesLoader.loadAnimGameItem("models/fbx_not_working/Base Normal.FBX", "");

        animItemDebug.setScale(0.01f);

        animItem1.setScale(0.01f);
        animItem2.setScale(0.01f);

        double angle = -1.5708; // 90 deg in rad
        double x = 0.0f * Math.sin(angle/2.0);
        double y = 0.0f * Math.sin(angle/2.0);
        double z = 1.0f * Math.sin(angle/2.0);
        double w = Math.cos(angle/2.0);

        Quaternionf rot = new Quaternionf(new AxisAngle4d(angle, 1.0, 0.0, 0.0));
        //rot = rot.mul(new Quaternionf(new AxisAngle4d(angle, 1.0, 0.0, 0.0)));
        //animItemDebug.setRotation(rot);
        //animItem1.setRotation(rot);
        //animItem2.setRotation(rot);

        animItemDebug.setPosition(0.0f, 0.5f, 0.0f);

        animItem1.setPosition(-2.0f, 0.5f, 0.0f);
        animItem2.setPosition(2.0f, 0.5f, 0.0f);

        animationDebug = animItemDebug.getCurrentAnimation();

        animation1 = animItem1.getCurrentAnimation();
        animation2 = animItem2.getCurrentAnimation();

        //Mesh[] baseMesh = StaticMeshesLoader.load("models/fbx_not_working/Base Normal.FBX", "models/terrain");
        //GameItem base = new GameItem(baseMesh);

        Mesh[] houseMesh = StaticMeshesLoader.load("models/house/house.obj", "models/house");
        GameItem house = new GameItem(houseMesh);
        Mesh[] swordMesh = StaticMeshesLoader.load("models/fbx_not_working/TH Sword 01.FBX", "@TH Sword 01 White.png");
        GameItem sword2H = new GameItem(swordMesh, "2HSword");
        sword2H.setScale(0.01f);
        sword2H.setPosition(-2.0f, 0.5f, 0.0f);

        //Mesh[] swordMesh2 = StaticMeshesLoader.load("models/fbx_not_working/TH Sword 01.FBX", "@TH Sword 01 White.png");
        Mesh[] swordMesh2 = StaticMeshesLoader.load("models/fbx_not_working/Sword 04.FBX", "@Sword 04 Purple.png"); //, -0.9199994f *10.0f, 0.35999992f*10.0f, -0.43999985f*10.0f, 0.0f, 0.0f, 0.0f);
        GameItem sword2H2 = new GameItem(swordMesh2, "2HSword2");
        sword2H2.setScale(0.01f);
        rot = rot.mul(new Quaternionf(new AxisAngle4d(angle, 0.0, 1.0, 0.0)));
        //sword2H2.setRotation(rot);
        //sword2H2.setPosition(-2.0f, 0.5f, 0.0f);
        //sword2H2.setPosition(-0.9199994f *1.0f, 0.35999992f*1.0f, -0.43999985f*1.0f);

        //scene.setGameItems(new GameItem[]{animItem, terrain, base});
        //scene.setGameItems(new GameItem[]{lhmpGameItem, animItemDebug, animItem1, animItem2,  sword2H, sword2H2, terrain});
        scene.setGameItems(new GameItem[]{animItem2});

        // Shadows
        scene.setRenderShadows(true);

        // Fog
        Vector3f fogColour = new Vector3f(0.5f, 0.5f, 0.5f);
        scene.setFog(new Fog(true, fogColour, 0.02f));

        // Setup  SkyBox
        float skyBoxScale = 100.0f;
        SkyBox skyBox = new SkyBox("models/skybox.obj", new Vector4f(0.65f, 0.65f, 0.65f, 1.0f));
        skyBox.setScale(skyBoxScale);
        scene.setSkyBox(skyBox);

        // Setup Lights
        setupLights();

        camera.getPosition().x = -1.5f;
        camera.getPosition().y = 3.0f;
        camera.getPosition().z = 4.5f;
        camera.getRotation().x = 15.0f;
        camera.getRotation().y = 390.0f;
    }

    private void setupLights() {
        SceneLight sceneLight = new SceneLight();
        scene.setSceneLight(sceneLight);

        // Ambient Light
        sceneLight.setAmbientLight(new Vector3f(0.3f, 0.3f, 0.3f));
        sceneLight.setSkyBoxLight(new Vector3f(1.0f, 1.0f, 1.0f));

        // Directional Light
        float lightIntensity = 1.0f;
        Vector3f lightDirection = new Vector3f(0, 1, 1);
        DirectionalLight directionalLight = new DirectionalLight(new Vector3f(1, 1, 1), lightDirection, lightIntensity);
        sceneLight.setDirectionalLight(directionalLight);
    }

    @Override
    public void input(Window window, MouseInput mouseInput) {
        sceneChanged = false;
        cameraInc.set(0, 0, 0);
        if (window.isKeyPressed(GLFW_KEY_W)) {
            sceneChanged = true;
            cameraInc.z = -1;
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            sceneChanged = true;
            cameraInc.z = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            sceneChanged = true;
            cameraInc.x = -1;
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            sceneChanged = true;
            cameraInc.x = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_Z)) {
            sceneChanged = true;
            cameraInc.y = -1;
        } else if (window.isKeyPressed(GLFW_KEY_X)) {
            sceneChanged = true;
            cameraInc.y = 1;
        }
        if (window.isKeyPressed(GLFW_KEY_LEFT)) {
            sceneChanged = true;
            angleInc -= 0.05f;
        } else if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
            sceneChanged = true;
            angleInc += 0.05f;
        } else {
            angleInc = 0;            
        }
        if (window.isKeyPressed(GLFW_KEY_SPACE)) {
            sceneChanged = true;
            animationDebug.nextFrame();
            animation1.nextFrame();
            animation2.nextFrame();
        }
        /*
        if (window.isKeyPressed(GLFW_KEY_1)) {
            offX -= 0.01f;
        } else if (window.isKeyPressed(GLFW_KEY_2)) {
            offX += 0.01f;
        }
        if (window.isKeyPressed(GLFW_KEY_3)) {
            offY -= 0.01f;
        } else if (window.isKeyPressed(GLFW_KEY_4)) {
            offY += 0.01f;
        }
        if (window.isKeyPressed(GLFW_KEY_5)) {
            offZ -= 0.01f;
        } else if (window.isKeyPressed(GLFW_KEY_6)) {
            offZ += 0.01f;
        }
        if (window.isKeyPressed(GLFW_KEY_9)) {
            System.out.println(offX + " | " +  offY + " | " + offZ);
        }

         */
        if (window.isKeyPressed(GLFW_KEY_1)) {
            offX -= 0.1f;
        } else if (window.isKeyPressed(GLFW_KEY_2)) {
            offX += 0.1f;
        }
        if (window.isKeyPressed(GLFW_KEY_3)) {
            offY -= 0.1f;
        } else if (window.isKeyPressed(GLFW_KEY_4)) {
            offY += 0.1f;
        }
        if (window.isKeyPressed(GLFW_KEY_5)) {
            offZ -= 0.1f;
        } else if (window.isKeyPressed(GLFW_KEY_6)) {
            offZ += 0.1f;
        }
        if (window.isKeyPressed(GLFW_KEY_9)) {
            System.out.println(offX + " | " +  offY + " | " + offZ);
        }
    }

    @Override
    public void update(float interval, MouseInput mouseInput, Window window) {
        if (mouseInput.isRightButtonPressed()) {
            // Update camera based on mouse            
            Vector2f rotVec = mouseInput.getDisplVec();
            camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);
            sceneChanged = true;
        }

        // Update camera position
        camera.movePosition(cameraInc.x * CAMERA_POS_STEP, cameraInc.y * CAMERA_POS_STEP, cameraInc.z * CAMERA_POS_STEP);

        lightAngle += angleInc;
        if (lightAngle < 0) {
            lightAngle = 0;
        } else if (lightAngle > 180) {
            lightAngle = 180;
        }
        float zValue = (float) Math.cos(Math.toRadians(lightAngle));
        float yValue = (float) Math.sin(Math.toRadians(lightAngle));
        Vector3f lightDirection = this.scene.getSceneLight().getDirectionalLight().getDirection();
        lightDirection.x = 0;
        lightDirection.y = yValue;
        lightDirection.z = zValue;
        lightDirection.normalize();

        // Update view matrix
        camera.updateViewMatrix();
    }

    @Override
    public void render(Window window) {
        if (firstTime) {
            sceneChanged = true;
            firstTime = false;
        }
        renderer.render(window, camera, scene, sceneChanged);
    }

    @Override
    public void cleanup() {
        renderer.cleanup();

        scene.cleanup();
    }
}
