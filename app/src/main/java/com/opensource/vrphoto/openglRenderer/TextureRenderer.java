package com.opensource.vrphoto.openglRenderer;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


public class TextureRenderer {

    private int mProgram;
    private int mTexSamplerHandle;
    private int mTexCoordHandle;
    private int mPosCoordHandle;

    private FloatBuffer mTexVertices;
    private FloatBuffer mPosVertices;

    private int mViewWidth;
    private int mViewHeight;

    private int mTexWidth;
    private int mTexHeight;

    private static final String VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
                    "attribute vec2 a_texcoord;\n" +
                    "varying vec2 v_texcoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = a_position;\n" +
                    "  v_texcoord = a_texcoord;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "uniform sampler2D tex_sampler;\n" +
                    "varying vec2 v_texcoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(tex_sampler, v_texcoord);\n" +
                    "}\n";

    private static final float[] TEX_VERTICES = {
            0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
    };

    private static final float[] POS_VERTICES = {
            -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f
    };

    private static final int FLOAT_SIZE_BYTES = 4;

    public void init() {
        // Create program
        mProgram = GLToolBox.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        // Bind attributes and uniforms
        mTexSamplerHandle = GLES30.glGetUniformLocation(mProgram,
                "tex_sampler");
        mTexCoordHandle = GLES30.glGetAttribLocation(mProgram, "a_texcoord");
        mPosCoordHandle = GLES30.glGetAttribLocation(mProgram, "a_position");

        // Setup coordinate buffers
        mTexVertices = ByteBuffer.allocateDirect(
                TEX_VERTICES.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexVertices.put(TEX_VERTICES).position(0);
        mPosVertices = ByteBuffer.allocateDirect(
                POS_VERTICES.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPosVertices.put(POS_VERTICES).position(0);
    }

    public void tearDown() {
        GLES30.glDeleteProgram(mProgram);
    }

    public void updateTextureSize(int texWidth, int texHeight) {
        mTexWidth = texWidth;
        mTexHeight = texHeight;
        computeOutputVertices();
    }

    public void updateViewSize(int viewWidth, int viewHeight) {
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
        computeOutputVertices();
    }

    public void renderTexture(int texId) {
        // Bind default FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        // Use our shader program
        GLES30.glUseProgram(mProgram);
        GLToolBox.checkGlError("glUseProgram");

        // Set viewport
        GLES30.glViewport(0, 0, mViewWidth, mViewHeight);
        GLToolBox.checkGlError("glViewport");

        // Disable blending
        GLES30.glDisable(GLES30.GL_BLEND);

        // Set the vertex attributes
        GLES30.glVertexAttribPointer(mTexCoordHandle, 2, GLES30.GL_FLOAT, false,
                0, mTexVertices);
        GLES30.glEnableVertexAttribArray(mTexCoordHandle);
        GLES30.glVertexAttribPointer(mPosCoordHandle, 2, GLES30.GL_FLOAT, false,
                0, mPosVertices);
        GLES30.glEnableVertexAttribArray(mPosCoordHandle);
        GLToolBox.checkGlError("vertex attribute setup");

        // Set the input texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLToolBox.checkGlError("glActiveTexture");
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);
        GLToolBox.checkGlError("glBindTexture");
        GLES30.glUniform1i(mTexSamplerHandle, 0);

        // Draw
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    private void computeOutputVertices() {
        if (mPosVertices != null) {
            float imgAspectRatio = mTexWidth / (float)mTexHeight;
            float viewAspectRatio = mViewWidth / (float)mViewHeight;
            float relativeAspectRatio = viewAspectRatio / imgAspectRatio;

            float x0, y0, x1, y1;
            if (relativeAspectRatio > 1.0f) {
                x0 = -1.0f / relativeAspectRatio;
                y0 = -1.0f;
                x1 = 1.0f / relativeAspectRatio;
                y1 = 1.0f;
            } else {
                x0 = -1.0f;
                y0 = -relativeAspectRatio;
                x1 = 1.0f;
                y1 = relativeAspectRatio;
            }
            float[] coords = new float[] { x0, y0, x1, y0, x0, y1, x1, y1 };
            mPosVertices.put(coords).position(0);
        }
    }

}
