import map.ProvinceMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import render.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    // openGL window handle
    private long window;
    Vector2f mousePos = new Vector2f(0, 0);
    Vector2f mouseWorldPos = new Vector2f(0, 0);
    Vector3f cameraPos = new Vector3f(6, 4.25f, 13);
    Vector3f cameraFront = new Vector3f(0, 0, -1);
    Vector3f up = new Vector3f(0, 1, 0);
    ResourceManager resourceManager;
    SpriteRenderer spriteRenderer;
    ProvinceMap provinceMap;

    private double lastTurnChange;

    public static final int WINDOW_WIDTH = 1000;
    public static final int WINDOW_HEIGHT = 1000;
    public static final float ASPECT_RATIO = WINDOW_WIDTH/(float)WINDOW_HEIGHT;

    float debugFactor = 1;
    float deltaTime = 0.0f;
    float lastFrame = 0.0f;
    int visualizationMode = 1;  // 1 = current visuals, 2 = distance to cities, 3 = rating

    public void run() {
        initGL();
        initResources();
        initGame();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private ResourceManager loadResources() {
        resourceManager = new ResourceManager();
        resourceManager.loadResources();
        return resourceManager;
    }

    private void initGL() {
        GLFWErrorCallback.createPrint(System.err).set();

        if(!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Map Distances", NULL, NULL);
        if(window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if(key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true);
            // Switch visualization modes with 1, 2, 3 keys
            if(key == GLFW_KEY_1 && action == GLFW_RELEASE)
                visualizationMode = 1;
            if(key == GLFW_KEY_2 && action == GLFW_RELEASE)
                visualizationMode = 2;
            if(key == GLFW_KEY_3 && action == GLFW_RELEASE)
                visualizationMode = 3;
        });
        glfwSetScrollCallback(window, this::scrollCallback);
        glfwSetCursorPosCallback(window, this::mouseCallback);

        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        glfwShowWindow(window);
        GL.createCapabilities();
//        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void initResources() {
        loadResources();
        spriteRenderer = new SpriteRenderer(resourceManager);
        spriteRenderer.initRenderData();
    }

    private void initGame() {

    }

    private Vector2f mouseToNDC() {
        return new Vector2f(2*mousePos.x/WINDOW_WIDTH - 1, 1 - 2*mousePos.y/WINDOW_HEIGHT);
    }

    private Vector3f mouseToWorld(float depth, Matrix4f view, Matrix4f proj) {
        Vector2f ndc = mouseToNDC();
        Vector4f vec = new Vector4f(ndc.x, ndc.y, depth, 1);
        Matrix4f vi = new Matrix4f(proj).mul(view).invert();
        vec.mul(vi);
        vec.div(vec.w);
        return new Vector3f(vec.x, vec.y, vec.z);
    }

    private void loop() {
        provinceMap = MapGenerator.generateHexMap();
        provinceMap.populate();

        glClearColor(0.2f, 0.2f, 0.6f, 0.0f);

        MapRenderer mapRenderer = new MapRenderer();
        mapRenderer.init(resourceManager);

        while(!glfwWindowShouldClose(window)) {
            processInput();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            double currentFrame = glfwGetTime();
            deltaTime = (float) currentFrame - lastFrame;
            lastFrame = (float) currentFrame;

            Vector3f dir = new Vector3f();
            cameraPos.add(cameraFront, dir);
            Matrix4f view = new Matrix4f().lookAt(cameraPos, dir, up);

            Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(45), WINDOW_WIDTH/(float)WINDOW_HEIGHT, 0.1f, 100f);

            Vector4f testvec = new Vector4f(0, 0, 0, 1);
            testvec.mul(view).mul(proj);


            Vector3f v1 = mouseToWorld(0, view, proj);
            Vector3f v2 = mouseToWorld(1, view, proj);
            float lerp = -v2.z/(v1.z - v2.z);

            v1.mul(lerp).add(v2.mul(1-lerp)).xy(mouseWorldPos);

            float[] aView = new float[16];
            view.get(aView);
            float[] aProj = new float[16];
            proj.get(aProj);
            Shader shader = resourceManager.getShader("sprite");
            spriteRenderer.shader = shader;
            int vLoc = glGetUniformLocation(shader.id, "view");
            int pLoc = glGetUniformLocation(shader.id, "projection");

            glUniformMatrix4fv(vLoc, false, aView);
            glUniformMatrix4fv(pLoc, false, aProj);

            mapRenderer.renderMap(provinceMap, visualizationMode);

            float[] ident = new float[16];
            float[] ident2 = new float[16];
            new Matrix4f().get(ident);
            new Matrix4f().get(ident2);
            glUniformMatrix4fv(vLoc, false, ident);
            glUniformMatrix4fv(pLoc, false, ident2);

            //render UI

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void scrollCallback(long window, double xoffset, double yoffset) {
        Vector3f v = new Vector3f();
        cameraPos.add(cameraFront.mul((float) yoffset, v));
        cameraPos.z = Math.min(Math.max(cameraPos.z, 0.5f), 45f);
    }

    private void mouseCallback(long window, double xpos, double ypos) {
        mousePos.set(xpos, ypos);
    }

    private void processInput() {
        float cameraSpeed = 4f * deltaTime;
        Vector3f v = new Vector3f();
        if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS)
            debugFactor = 1f;
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS)
            debugFactor = 0f;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            cameraPos.add(cameraFront.cross(up, v).normalize(v).mul(-cameraSpeed, v));
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            cameraPos.add(cameraFront.cross(up, v).normalize(v).mul(cameraSpeed, v));
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            cameraPos.add(up.mul(cameraSpeed, v));
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            cameraPos.add(up.mul(-cameraSpeed, v));
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            if (glfwGetTime() - lastTurnChange > 0.3) {  // Debounce to prevent rapid firing
                lastTurnChange = glfwGetTime();
                provinceMap.addConnectedCity();
            }
        }
    }

    public static void main(String[] args) {
        new Main().run();
    }

}