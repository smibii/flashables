#version 150

out vec4 fragColor;

void main()
{
    /*
     * Color is masked off (glColorMask(false,...)) for the whole
     * shadow pass - only depth actually matters here.
     */
    fragColor = vec4(1.0);
}