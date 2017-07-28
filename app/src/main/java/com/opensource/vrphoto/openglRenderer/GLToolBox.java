package com.opensource.vrphoto.openglRenderer;

import android.opengl.GLES30;

public class GLToolBox {

    public static int loadShader(int shaderType, String source) {
        int shader = GLES30.glCreateShader(shaderType);
        if (shader != 0) {
            GLES30.glShaderSource(shader, source);
            GLES30.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                String info = GLES30.glGetShaderInfoLog(shader);
                GLES30.glDeleteShader(shader);
                throw new RuntimeException("Could not compile shader " + shaderType + ":" + info);
            }
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES30.glCreateProgram();
        if (program != 0) {
            GLES30.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES30.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES30.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus,
                    0);
            if (linkStatus[0] != GLES30.GL_TRUE) {
                String info = GLES30.glGetProgramInfoLog(program);
                GLES30.glDeleteProgram(program);
                throw new RuntimeException("Could not link program: " + info);
            }
        }
        return program;
    }

    public static void checkGlError(String op) {
        int error = GLES30.glGetError();
        if (error != GLES30.GL_NO_ERROR) {
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public static void initTexParams() {
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE);
    }

}
