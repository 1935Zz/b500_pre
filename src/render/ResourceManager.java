package render;

import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.stb.STBImage.*;

public class ResourceManager {
    private Map<String, Shader> shaders = new HashMap();
    private Map<String, Texture> textures = new HashMap();

    public Shader loadShader(String name, String vShaderPath, String fShaderPath) {
        String vertexShaderSource;
        String fragmentShaderSource;
        try {
            vertexShaderSource = Files.readString(Path.of(vShaderPath));
            fragmentShaderSource = Files.readString(Path.of(fShaderPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Shader s = new Shader(vertexShaderSource, fragmentShaderSource);
        shaders.put(name, s);
        System.out.println("loaded shader: " + name);
        return s;
    }

    public Shader getShader(String name) {
        return shaders.get(name);
    }

    public Texture loadTexture(String name, String path) {
        Texture texture = new Texture();
        ByteBuffer data;
        int w, h;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer nChannels = stack.mallocInt(1);
            stbi_set_flip_vertically_on_load(true);
            data = stbi_load(path, width, height, nChannels, 4);
            w = width.get();
            h = height.get();
        }
        texture.generate(w, h, data);
        stbi_image_free(data);
        textures.put(name, texture);
        return texture;
    }

    public Texture getTexture(String name) {
        if(!textures.containsKey(name)) {
            throw new IllegalArgumentException("No loaded texture with id " + name);
        }
        return textures.get(name);
    }

    public void loadResources() {
        loadShader("sprite", "src/vert.glsl", "src/frag.glsl");
//        loadTexture("unknown", "gfx/portrait/unknown.png");
    }

}
