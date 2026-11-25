# Web Implementation Guide

This document covers implementing Liquid Glass on the web using WebGL2 and WebGPU.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    MultiPassRenderer                         │
├─────────────┬─────────────┬─────────────┬───────────────────┤
│   bgPass    │  vBlurPass  │  hBlurPass  │     mainPass      │
│  (Scene)    │ (Vertical)  │ (Horizontal)│   (Composition)   │
└──────┬──────┴──────┬──────┴──────┬──────┴────────┬───────────┘
       │             │             │              │
       ▼             ▼             ▼              ▼
  ┌─────────┐   ┌──────────┐  ┌──────────┐  ┌───────────┐
  │Framebuffer│  │Framebuffer│  │Framebuffer│  │  Screen   │
  │ RGBA16F  │   │ RGBA16F   │  │ RGBA16F   │  │ (Canvas)  │
  └─────────┘   └──────────┘  └──────────┘  └───────────┘
```

## WebGL2 Implementation

### Core Classes (from `GLUtils.ts`)

```typescript
// ShaderProgram - Compiles and manages shaders
class ShaderProgram {
  constructor(gl: WebGL2RenderingContext, source: {vertex: string, fragment: string})
  setUniform(name: string, value: any): void
  use(): void
  dispose(): void
}

// FrameBuffer - Offscreen render target with RGBA16F color + depth
class FrameBuffer {
  constructor(gl: WebGL2RenderingContext, width: number, height: number)
  bind(): void
  unbind(): void
  getTexture(): WebGLTexture
  resize(width: number, height: number): void
  dispose(): void
}

// RenderPass - Single draw call with its own shader + FBO
class RenderPass {
  constructor(gl: WebGL2RenderingContext, shaderSource, outputToScreen: boolean)
  render(uniforms?: Record<string, any>): void
  getOutputTexture(): WebGLTexture | null
  resize(width: number, height: number): void
  dispose(): void
}

// MultiPassRenderer - Orchestrates 4 passes
class MultiPassRenderer implements IMultiPassRenderer {
  constructor(canvas: HTMLCanvasElement, configs: RenderPassConfig[])
  setUniform(name: string, value: any): void
  setUniforms(uniforms: Record<string, any>): void
  render(passUniforms?: Record<string, any>[]): void
  resize(width: number, height: number): void
  dispose(): void
}
```

### Pass Configuration

```typescript
const configs: RenderPassConfig[] = [
  { name: 'bgPass', shader: { vertex: VertexShader, fragment: FragmentBgShader } },
  { name: 'vBlurPass', shader: { vertex: VertexShader, fragment: FragmentBgVblurShader }, inputs: { u_prevPassTexture: 'bgPass' } },
  { name: 'hBlurPass', shader: { vertex: VertexShader, fragment: FragmentBgHblurShader }, inputs: { u_prevPassTexture: 'vBlurPass' } },
  { name: 'mainPass', shader: { vertex: VertexShader, fragment: FragmentMainShader }, inputs: { u_blurredBg: 'hBlurPass', u_bg: 'bgPass' }, outputToScreen: true }
];
```

### Critical Requirements

1. **EXT_color_buffer_float** extension for RGBA16F framebuffers
2. **High-precision uniforms**: Use `Float32Array` for all numeric uniforms
3. **DPR handling**: Canvas size = `cssWidth * dpr`; uniforms use physical pixels
4. **Texture management**: Images via `loadTextureFromURL()`; Videos via `createEmptyTexture()` + `updateVideoTexture()` per frame

### Vertex Shader (Fullscreen Quad)

```glsl
#version 300 es
in vec4 a_position;
out vec2 v_uv;
void main() {
  v_uv = (a_position.xy + 1.0) * 0.5;
  gl_Position = a_position;
}
```
Vertices: `[-1,-1, 1,-1, -1,1, 1,1]` (TRIANGLE_STRIP)

## WebGPU Implementation

### Core Classes (from `GPUUtils.ts`)

```typescript
// GPUMultiPassRenderer - WebGPU equivalent
class GPUMultiPassRenderer implements IMultiPassRenderer {
  constructor(canvas: HTMLCanvasElement, configs: RenderPassConfig[], device: GPUDevice)
  // Same interface as WebGL2 version
}
```

### Key Differences from WebGL2

| Aspect | WebGL2 | WebGPU |
|--------|--------|--------|
| Shader Language | GLSL 300 ES | WGSL |
| Uniforms | `gl.uniform*` | Uniform buffers (std140) |
| Textures | `gl.bindTexture` | Bind groups + `GPUTextureView` |
| Framebuffer | `WebGLFramebuffer` | `GPUTexture` with `RENDER_ATTACHMENT` |
| Blur Weights | Uniform array | Storage buffer (read-only) |
| Sampler | `gl.sampler*` | `GPUSampler` in bind group |

### Bind Group Layouts (Per Pass Type)

```typescript
// Background pass
{ uniform, texture, sampler }

// Blur passes  
{ uniform, texture, sampler, storage_buffer(weights) }

// Main pass
{ uniform, blurredBg_texture, bg_texture, sampler }
```

### Uniform Buffer Layout (Main Pass)

The 2272-byte buffer matches the WGSL `Uniforms` struct exactly (std140 alignment):
```typescript
// Offsets (bytes):
0:   u_resolution (vec2f)        8
8:   u_dpr (f32)                 4
12:  _pad0                       4
16:  u_mouse (vec2f)             8
24:  u_mouseSpring (vec2f)       8
32:  u_shapeWidth (f32)          4
36:  u_shapeHeight (f32)         4
40:  u_shapeRadius (f32)         4
44:  _pad                        4
48:  u_mergeRate (f32)           4
... // continues for all uniforms
224: u_contourPoints[128] (vec4f) 2048
```

### Shader Module Creation

```typescript
const shaderModule = device.createShaderModule({
  code: vertexWGSL + '\n' + fragmentWGSL  // Combined
});
```

## Performance Optimization

1. **Reuse renderers**: Don't recreate on every frame
2. **Batch uniform updates**: `setUniforms()` once per frame
3. **Minimize texture binds**: Multi-pass renderer handles chaining
4. **DPR clamping**: Cap at 2x for performance on mobile
5. **Blur radius**: Keep ≤ 50 for 60fps on integrated graphics

## CSS Fallback (Non-Shader)

For environments without WebGL/WebGPU, use CSS approximation:

```css
.liquid-glass-fallback {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;  /* Approximate superellipse */
  box-shadow: 
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    0 8px 32px rgba(0, 0, 0, 0.1);
}
```

**Limitations:** No refraction, no dispersion, no dynamic glare, no blob merging.

## Browser Compatibility

| Feature | Chrome | Firefox | Safari | Edge |
|---------|--------|---------|--------|------|
| WebGL2 | 56+ | 51+ | 15+ | 79+ |
| WebGPU | 113+ | Behind flag | 16.4+ | 113+ |
| EXT_color_buffer_float | ✓ | ✓ | ✓ | ✓ |
| RGBA16F renderable | ✓ | ✓ | ✓ | ✓ |

## React Integration Pattern

```typescript
const canvasRef = useRef<HTMLCanvasElement>(null);
const rendererRef = useRef<IMultiPassRenderer>(null);

useEffect(() => {
  const canvas = canvasRef.current;
  if (!canvas) return;
  
  const gpuResult = await detectWebGPU();
  
  if (gpuResult.supported) {
    rendererRef.current = new GPUMultiPassRenderer(canvas, configs, gpuResult.device);
  } else {
    rendererRef.current = new MultiPassRenderer(canvas, configs);
  }
  
  const render = () => {
    rendererRef.current?.setUniforms(globalUniforms);
    rendererRef.current?.render(passUniforms);
    requestAnimationFrame(render);
  };
  render();
  
  return () => rendererRef.current?.dispose();
}, []);
```