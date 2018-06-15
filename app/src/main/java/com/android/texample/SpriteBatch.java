package com.android.texample;

import javax.microedition.khronos.opengles.GL10;

public class SpriteBatch {

    //--Constants--//
    final static int VERTEX_SIZE = 4;                  // Vertex Size (in Components) ie. (X,Y,U,V)
    final static int VERTICES_PER_SPRITE = 4;          // Vertices Per Sprite
    final static int INDICES_PER_SPRITE = 6;           // Indices Per Sprite

    //--Members--//
    GL10 gl;                                           // GL Instance
    Vertices vertices;                                 // Vertices Instance Used for Rendering
    float[] vertexBuffer;                              // Vertex Buffer
    int bufferIndex;                                   // Vertex Buffer Start Index
    int maxSprites;                                    // Maximum Sprites Allowed in Buffer
    int numSprites;                                    // Number of Sprites Currently in Buffer

    //--Constructor--//
    // D: prepare the sprite batcher for specified maximum number of sprites
    // A: gl - the gl instance to use for rendering
    //    maxSprites - the maximum allowed sprites per batch
    public SpriteBatch(GL10 gl, int maxSprites) {
        this.gl = gl;                                   // Save GL Instance
        this.vertexBuffer = new float[maxSprites * VERTICES_PER_SPRITE * VERTEX_SIZE];  // Create Vertex Buffer
        this.vertices = new Vertices(gl, maxSprites * VERTICES_PER_SPRITE, maxSprites * INDICES_PER_SPRITE, false, true,
            false);  // Create Rendering Vertices
        this.bufferIndex = 0;                           // Reset Buffer Index
        this.maxSprites = maxSprites;                   // Save Maximum Sprites
        this.numSprites = 0;                            // Clear Sprite Counter

        short[] indices = new short[maxSprites * INDICES_PER_SPRITE];  // Create Temp Index Buffer
        int len = indices.length;                       // Get Index Buffer Length
        short j = 0;                                    // Counter
        for (int i = 0; i < len;
            i += INDICES_PER_SPRITE, j += VERTICES_PER_SPRITE) {  // FOR Each Index Set (Per Sprite)
            indices[i + 0] = (short) (j + 0);           // Calculate Index 0
            indices[i + 1] = (short) (j + 1);           // Calculate Index 1
            indices[i + 2] = (short) (j + 2);           // Calculate Index 2
            indices[i + 3] = (short) (j + 2);           // Calculate Index 3
            indices[i + 4] = (short) (j + 3);           // Calculate Index 4
            indices[i + 5] = (short) (j + 0);           // Calculate Index 5
        }
        vertices.setIndices(indices, 0, len);         // Set Index Buffer for Rendering
    }

    //--Begin Batch--//
    // D: signal the start of a batch. set the texture and clear buffer
    //    NOTE: the overloaded (non-texture) version assumes that the texture is already bound!
    // A: textureId - the ID of the texture to use for the batch
    // R: [none]
    public void beginBatch(int textureId) {
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);  // Bind the Texture
        numSprites = 0;                                 // Empty Sprite Counter
        bufferIndex = 0;                                // Reset Buffer Index (Empty)
    }

    public void beginBatch() {
        numSprites = 0;                                 // Empty Sprite Counter
        bufferIndex = 0;                                // Reset Buffer Index (Empty)
    }

    //--End Batch--//
    // D: signal the end of a batch. render the batched sprites
    // A: [none]
    // R: [none]
    public void endBatch() {
        if (numSprites > 0) {                        // IF Any Sprites to Render
            vertices.setVertices(vertexBuffer, 0, bufferIndex);  // Set Vertices from Buffer
            vertices.bind();                             // Bind Vertices
            vertices.draw(GL10.GL_TRIANGLES, 0, numSprites * INDICES_PER_SPRITE);  // Render Batched Sprites
            vertices.unbind();                           // Unbind Vertices
        }
    }

    //--Draw Sprite to Batch--//
    // D: batch specified sprite to batch. adds vertices for sprite to vertex buffer
    //    NOTE: MUST be called after beginBatch(), and before endBatch()!
    //    NOTE: if the batch overflows, this will render the current batch, restart it,
    //          and then batch this sprite.
    // A: x, y - the x,y position of the sprite (center)
    //    width, height - the width and height of the sprite
    //    region - the texture region to use for sprite
    // R: [none]
    public void drawSprite(float x, float y, float angle, float width, float height, TextureRegion region) {
        if (numSprites == maxSprites) {
            endBatch();
            numSprites = 0;
            bufferIndex = 0;
        }

        float halfWidth = width / 2.0f; // Half Width
        float halfHeight = height / 2.0f; // Half Height

        float rad = angle * ((1.0f / 180.0f) * (float) Math.PI); // Get Angle in Radians
        float cos = (float) Math.cos(rad); // Cosine of Angle
        float sin = (float) Math.sin(rad); // Sine of Angle

        float halfWidthCos = halfWidth * cos; // Cosine of Half Width
        float halfWidthSin = halfWidth * sin; // Sine of Half Width
        float halfHeightCos = halfHeight * cos; // Cosine of Half Height
        float halfHeightSin = halfHeight * sin; // Sine of Half Height

        vertexBuffer[bufferIndex++] = (-halfWidthCos) - (-halfHeightSin) + x;
        vertexBuffer[bufferIndex++] = (-halfWidthSin) + (-halfHeightCos) + y;
        vertexBuffer[bufferIndex++] = region.u1;
        vertexBuffer[bufferIndex++] = region.v2;

        vertexBuffer[bufferIndex++] = (halfWidthCos) - (-halfHeightSin) + x;
        vertexBuffer[bufferIndex++] = (halfWidthSin) + (-halfHeightCos) + y;
        vertexBuffer[bufferIndex++] = region.u2;
        vertexBuffer[bufferIndex++] = region.v2;

        vertexBuffer[bufferIndex++] = (halfWidthCos) - (halfHeightSin) + x;
        vertexBuffer[bufferIndex++] = (halfWidthSin) + (halfHeightCos) + y;
        vertexBuffer[bufferIndex++] = region.u2;
        vertexBuffer[bufferIndex++] = region.v1;

        vertexBuffer[bufferIndex++] = (-halfWidthCos) - (halfHeightSin) + x;
        vertexBuffer[bufferIndex++] = (-halfWidthSin) + (halfHeightCos) + y;
        vertexBuffer[bufferIndex++] = region.u1;
        vertexBuffer[bufferIndex++] = region.v1;

        numSprites++; // Increment Sprite Count
    }
}
