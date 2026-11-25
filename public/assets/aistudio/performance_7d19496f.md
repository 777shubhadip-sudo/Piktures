# Performance Guide

Optimizing for 60fps at high resolution with multiple glass layers.

## General Optimization

1. **GPU-Bound Rendering**: The Liquid Glass effect is heavily GPU-bound (SDF field distance computations per pixel).
2. **Multi-Pass Pipeline**:
    - Avoid reallocating framebuffers. Resize only when necessary.
    - If multiple glass elements exist, attempt to share the background blur (render background blur once, then render all glass elements).
3. **SDF Complexity**:
    - The `continuousRoundedRectSDF` involves Bézier curve intersections. Optimize by minimizing `MAX_CONTOUR_POINTS` to the lowest usable value.
    - Reduce loop counts in `transitionDistance` if possible for simple shapes.
4. **Blur**:
    - The 2-pass separable blur is $O(2 * Radius * N_{pixels})$.
    - Use downsampled blur buffers (e.g., render blur at 0.5x resolution and upscale) if the UI is large or GPU is constrained.
5. **Texture Uploads**:
    - Avoid `texImage2D` video updates if content is static.
    - Use `createImageBitmap` for optimal texture uploads in modern browsers.

## WebGPU Specifics

1. **Storage Buffers**: Use `read-only-storage` for large uniform arrays (like contour points).
2. **Bind Group Caching**: Minimize creating new Bind Groups per frame.
3. **Pipeline Re-use**: Use the same pipeline for different shapes where possible (same shaders, different uniforms).

## macOS / Metal Specifics

1. **Texture Format**: Use `.rgba16Float` to preserve high-dynamic-range (HDR) colors.
2. **Metal Performance Shaders (MPS)**: Consider using `MPSImageGaussianBlur` instead of manual blur shaders for better hardware-optimized performance.
3. **Command Buffer Management**: Submit in small batches. Don't block the main thread.
4. **Drawing**: Always use `triangleStrip` with 4 vertices for fullscreen effects to minimize geometry setup.
