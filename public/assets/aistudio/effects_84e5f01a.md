# Liquid Glass Effects Reference

This document details the individual visual effects that constitute the Liquid Glass look. Each effect is implemented in the shader pipeline and can be tuned independently.

## 1. Refraction (Light Bending)

**Physics:** Simulates light passing through a medium with a different refractive index (IOR). The bending angle is calculated using Snell's Law.

**Shader Implementation:** `fragment-main.glsl` / `fragment-main.wgsl`

**Key Uniforms:**
- `u_refThickness` (float): Physical thickness of the glass edge in pixels. Controls where refraction stops.
- `u_refFactor` (float): Refractive Index (IOR). Typical glass: 1.4–1.5.
- `u_refDistance` (float): Scale factor for the UV offset. Controls intensity of the distortion.
- `u_refDispersion` (float): Chromatic dispersion strength (see below).

**Algorithm (GLSL):**
```glsl
// 1. Calculate normalized distance from edge (0 at edge, 1 at center)
float nmerged = -merged * u_resolution1x.y;
float x_R_ratio = 1.0 - nmerged / u_refThickness;

// 2. Snell's Law: sin(theta_t) = sin(theta_i) / IOR
// theta_i = asin(x_R_ratio^2) -- approximates incident angle based on depth
float thetaI = safeAsin(pow(x_R_ratio, 2.0));
float thetaT = safeAsin(1.0 / u_refFactor * sin(thetaI));

// 3. Deviation angle
float edgeFactor = -tan(thetaT - thetaI);
if (nmerged >= u_refThickness) edgeFactor = 0.0;

// 4. Offset UV by surface normal * edgeFactor
vec2 normal = getNormal(p1, p2, pixel);
vec2 refOffset = -normal * edgeFactor * u_refDistance * u_dpr * vec2(aspectCorrection, 1.0);
```

**Behavior:**
- Only active within `u_refThickness` pixels of the silhouette edge.
- Center of the shape samples the blurred background directly (no offset).
- Normal is derived from the SDF gradient (central difference).