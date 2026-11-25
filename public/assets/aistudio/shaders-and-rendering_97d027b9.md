# Shaders and Rendering Technical Reference

## Overview
This document details the shader pipeline, render passes, and low-level implementation of the Liquid Glass effect.

## WebGL2 Multi-Pass Pipeline

### Pass 1: Background (bgPass)
- Renders the background image/video with shadows
- Shader: `fragment-bg.glsl`
- Output: RGBA16F framebuffer
- Requires: `EXT_color_buffer_float` extension

### Pass 2: Vertical Blur (vBlurPass)
- Separable Gaussian blur in vertical direction
- Shader: `fragment-bg-vblur.glsl`
- Input: texture from bgPass
- Uniform: `u_blurRadius` (sigma = radius/3)
- Output: RGBA16F framebuffer

### Pass 3: Horizontal Blur (hBlurPass)
- Separable Gaussian blur in horizontal direction
- Shader: `fragment-bg-hblur.glsl`
- Input: texture from vBlurPass
- Uniform: `u_blurRadius`
- Output: RGBA16F framebuffer

### Pass 4: Main Composition (mainPass)
- Full Liquid Glass composition
- Shader: `fragment-main.glsl` / `fragment-main.wgsl`
- Inputs: `u_blurredBg` (from hBlurPass), `u_bg` (from bgPass)
- Output: Canvas screen
