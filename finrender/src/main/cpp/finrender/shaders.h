//
// Created by iFinVer on 2016/11/22.
//

#ifndef MYOPENGLES_GL_SHADERS_H
#define MYOPENGLES_GL_SHADERS_H

#include <GLES2/gl2.h>

const GLfloat VERTICES_COORD[] =
        {
                -1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f,
        };

//无镜像，左上-左下-右下-右上  左上-左下-右下-右上
//循环一次是为了处理旋转问题，
const GLfloat TEXTURE_COORD_NOR[] =
        {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f, // new loop for rotate
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
        };

//镜像，左上-左下-右下-右上  左上-左下-右下-右上
//循环一次是为了处理旋转问题，
const GLfloat TEXTURE_COORD_MIRROR[] =
        {
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,// new loop for rotate
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
        };

class ShaderBase{
public:
    const char *vertexShader;
    const char *fragmentShader;

    ShaderBase(){
        vertexShader =
                        "attribute vec4 aPosition;                          \n"
                        "attribute vec2 aTexCoord;                          \n"
                        "varying vec2 vTexCoord;                            \n"
                        "void main(){                                       \n"
                        "   vTexCoord = aTexCoord;                          \n"
                        "   gl_Position = aPosition;                        \n"
                        "}                                                  \n";
    }

//    ~ShaderBase(){
//        delete vertexShader;
//        delete fragmentShader;
//    }
};
class ShaderRGBA : public ShaderBase {
public:
    ShaderRGBA() {
        fragmentShader =
                        "#extension GL_OES_EGL_image_external : require     \n"
                        "precision mediump float;                           \n"
                        "varying vec2 vTexCoord;                            \n"
                        "uniform sampler2D rgbTexture;                      \n"
                        "void main(){                                       \n"
                        "   gl_FragColor = texture2D(rgbTexture,vTexCoord); \n"
                        "}                                                  \n";

    }
};

class ShaderNV21 : public ShaderBase {
public:
    ShaderNV21() {
        fragmentShader =
                        "#extension GL_OES_EGL_image_external : require     \n"
                        "precision mediump float;                           \n"
                        "varying vec2 vTexCoord;                            \n"
                        "uniform sampler2D yTexture;                        \n"
                        "uniform sampler2D uvTexture;                       \n"
                        "                                                   \n"
                        "vec4 getBaseColor(in vec2 coord){                  \n"
                        "   float r,g,b,y,u,v;                              \n"
                        "   y = texture2D(yTexture,coord).r;                \n"
                        "   vec4 uvColor = texture2D(uvTexture,coord);      \n"
                        "   u = uvColor.a - 0.5;                            \n"
                        "   v = uvColor.r - 0.5;                            \n"
                        "   r = y + 1.13983*v;                              \n"
                        "   g = y - 0.39465*u - 0.58060*v;                  \n"
                        "   b = y + 2.03211*u;                              \n"
                        "   return vec4(r, g, b, 1.0);                      \n"
                        "}                                                  \n"
                        "                                                   \n"
                        "void main(){                                       \n"
                        "   vec4 color = getBaseColor(vTexCoord);           \n"
                        "   gl_FragColor = color;                           \n"
                        "}                                                  \n";
    }
};
#endif