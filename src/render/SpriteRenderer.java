package render;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class SpriteRenderer {

    private int quadVAO;
    public Shader shader;
    private int ebo;

    private int batchVAO;
    private int batchVBO;
    private FloatBuffer batchVerts;
    private int numBatchVerts;
    private static final int BATCH_INITIAL_SIZE = 4096;
    private boolean batchActive = false;
    private Matrix4f batchTransformer = new Matrix4f();


    public ResourceManager resourceManager;

    public SpriteRenderer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void initRenderData() {
        quadVAO = glGenVertexArrays();
        int vbo = glGenBuffers();
        ebo = glGenBuffers();
        glBindVertexArray(quadVAO);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        float[] vertices = {
                -0.5f, -0.5f, 0.0f,   0f, 0f,
                0.5f, -0.5f, 0.0f,    1f, 0f,
                -0.5f,  0.5f, 0.0f,   0f, 1f,
                0.5f,  0.5f, 0.0f,    1f, 1f
        };

        int[] indices = {
                0, 3, 2,
                0, 1, 3
        };
        // Put our vertices into the bound VBO buffer
        // Static draw hint -> set once, use many
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);



        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0); // unbind

        initBatchRender();
    }

    public void initBatchRender() {
        batchVAO = glGenVertexArrays();
        glBindVertexArray(batchVAO);

        batchVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, batchVBO);

        batchVerts = MemoryUtil.memAllocFloat(BATCH_INITIAL_SIZE);

        glBufferData(GL_ARRAY_BUFFER, BATCH_INITIAL_SIZE * Float.BYTES, GL_DYNAMIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0); // unbind
    }

    public void draw(Texture texture, Vector2f position, Vector2f size) {
        draw(texture, position, size, 0);
    }

    public void draw(Texture texture, Vector2f position, Vector2f size, float rotateDeg) {
        draw(texture, position, size, 0, new Matrix4f());
    }

    public void draw(Texture texture, Vector2f position, Vector2f size, float rotateDeg, Matrix4f transform) {
        texture.bind();
        shader.use();
        int mLoc = glGetUniformLocation(shader.id, "model");

        Matrix4f model = new Matrix4f().mul(transform).translate(position.x, position.y, 0);
        model.rotate((float) Math.toRadians(rotateDeg), 1f, 0f, 0f).scale(size.x, size.y, 1f);
        float[] aModel = new float[16];
        model.get(aModel);
        glUniformMatrix4fv(mLoc, false, aModel);
        glBindVertexArray(quadVAO);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT,0);
    }

    private String debugVector(float xi, float xf, float yi, float yf) {
        return "(" + xi + ":" + xf + " , " + yi + ":" + yf + ")";
    }

    public void drawRegion(Texture texture, Vector2f position, Vector2f positionOnAtlas, Vector2f sizeOnAtlas, float scale) {
        if (batchVerts.remaining() < 5 * 6) { // 6 verts each with 5 floats
            batchFlush();
        }
        float xi = position.x;
        float yi = position.y;
        float xf = xi + sizeOnAtlas.x / scale;
        float yf = yi + sizeOnAtlas.y / scale;

        float si = positionOnAtlas.x/texture.width;
        float ti = positionOnAtlas.y/texture.height;
        float sf = si + sizeOnAtlas.x/texture.width;
        float tf = ti + sizeOnAtlas.y/texture.height;

//        System.out.println(debugVector(xi,xf,yi,yf) + "   " + debugVector(si,sf,ti,tf));

        batchVerts.put(xi).put(yi).put(0).put(si).put(ti);
        batchVerts.put(xi).put(yf).put(0).put(si).put(tf);
        batchVerts.put(xf).put(yf).put(0).put(sf).put(tf);

        batchVerts.put(xi).put(yi).put(0).put(si).put(ti);
        batchVerts.put(xf).put(yf).put(0).put(sf).put(tf);
        batchVerts.put(xf).put(yi).put(0).put(sf).put(ti);

        numBatchVerts += 6;
    }

    public void startBatch() {
        numBatchVerts = 0;
        batchActive = true;
    }

    public void finishBatch() {
        batchFlush();
        batchActive = false;
    }

    public void setBatchTransformer(Matrix4f transform) {
        batchTransformer = transform;
    }

    private void batchFlush() {
        if(numBatchVerts == 0) {
            return;
        }
        batchVerts.flip();
        shader.use();
        int mLoc = glGetUniformLocation(shader.id, "model");
        float[] aModel = new float[16];
        batchTransformer.get(aModel);
        glUniformMatrix4fv(mLoc, false, aModel);

        glBindVertexArray(batchVAO);
        glBindBuffer(GL_ARRAY_BUFFER, batchVBO);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBufferSubData(GL_ARRAY_BUFFER, 0, batchVerts); // should use something better than this for >1 MB upload
        glDrawArrays(GL_TRIANGLES, 0, numBatchVerts);
        batchVerts.clear();
        numBatchVerts = 0;
    }
}
