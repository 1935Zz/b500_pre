#version 330 core
out vec4 FragColor;

in vec2 texPos;

uniform sampler2D theTexture;

void main()
{
    FragColor = texture(theTexture, texPos);
}