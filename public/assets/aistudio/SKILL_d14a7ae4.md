# Liquid Glass UI Skill

---
name: liquid-glass-ui
version: 1.0.0
description: A comprehensive guide and framework for implementing physically-convincing Liquid Glass UI effects using shaders and native APIs.
tags: [ui, design, graphics, shaders, swiftui, webgl]
---

# Liquid Glass UI Skill

## Overview
This skill provides the architecture, shader code, and design principles to implement **Liquid Glass**—a UI design language characterized by high-fidelity refractions, dynamic light dispersion, Fresnel-based reflections, and soft-body geometry.

> **Note:** Liquid Glass is fundamentally different from standard CSS or native "Blur" materials. It relies on multi-pass rendering pipelines and custom shaders to simulate physical light behavior.

## Activation
Activate this skill when designing or implementing:
*   High-end macOS/iOS floating panels or interactive surfaces.
*   Web dashboards requiring high-performance, glass-like UI elements.
*   Any interface requiring physically realistic light refraction and material dispersion.

## Core Design Philosophy
1.  **Physical Authenticity:** Glass isn't just blur; it's a refractive material. Use SDF (Signed Distance Fields) for sharp, smooth edges.
2.  **Light Interaction:** Use dispersion (RGB shift), Fresnel (edge intensity), and Glare (directional highlights) to define material substance.
3.  **Dynamic Rendering:** Glass must react to its background. Use multi-pass rendering to capture the background, blur it, and re-composite through the glass.
4.  **Geometry:** Utilize superellipses for soft, organic corners.

## Implementation Decisions
*   **Web:** Use WebGL2 or WebGPU for multi-pass rendering (background pass -> blur -> composition).
*   **macOS:** Leverage SwiftUI `Material` for simple use cases, but bridge to Metal via `MTLView` for custom, high-fidelity Liquid Glass.

## Reusable Architecture
See `/references` for detailed breakdown of:
*   **Refraction:** SDF-normal mapping.
*   **Blur:** Separable Gaussian blur (performance optimized).
*   **Geometry:** Superellipse SDF functions.

## Performance & Accessibility
*   **Performance:** Offload all heavy calculations to the GPU. Ensure multi-pass rendering only triggers on change.
*   **Accessibility:** Always maintain contrast requirements. Glass transparency can hinder readability—provide a non-translucent fallback or solid high-contrast mode.

## Troubleshooting
*   **Visual Seams:** Often caused by floating point precision in shaders. Ensure SDF distance inputs are normalized.
*   **Performance Hit:** Ensure blur radius is optimized; reuse blur textures across multiple glass elements if possible.

---
For implementation details, refer to `references/` and `examples/`.
