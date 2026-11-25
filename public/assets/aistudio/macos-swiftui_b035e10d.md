# macOS SwiftUI Implementation

This document covers native Liquid Glass implementation using SwiftUI and AppKit.

## Native Materials (SwiftUI)

### Standard Glass (iOS 17+/macOS 14+)

```swift
// Basic glass panel
VisualEffectView(material: .hudWindow, blendingMode: .behindWindow)
    .ignoresSafeArea()

// Native modifier (iOS 17+)
.glassBackgroundEffect(in: RoundedRectangle(cornerRadius: 24))

// Container with glass
ZStack {
    Color.clear
        .background(.ultraThinMaterial)
}
```

### Advanced Material Configuration

```swift
struct GlassPanel: View {
    var body: some View {
        ZStack {
            VisualEffectView(
                material: .menu,
                blendingMode: .behindWindow
            )
            
            Rectangle()
                .fill(.white.opacity(0.08))
                .blendMode(.overlay)
            
            VStack {
                Rectangle()
                    .fill(.white.opacity(0.3))
                    .frame(height: 1)
                Spacer()
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(.white.opacity(0.15), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.1), radius: 20, x: 0, y: 10)
    }
}
```

### Window Background (macOS)

```swift
// For NSWindow / NSWindowController
window.backgroundColor = .clear
window.isOpaque = false
window.hasShadow = true
window.titlebarAppearsTransparent = true

// Full-window glass (macOS 13+)
let hostingView = NSHostingView(rootView: GlassContentView())
hostingView.layer?.backgroundColor = NSColor.clear.cgColor
window.contentView = hostingView
```

---

## Custom Metal Rendering (For True Liquid Glass)

Native materials lack refraction, dispersion, and dynamic glare. For full Liquid Glass, use Metal.

### Metal View Setup

```swift
import MetalKit

class LiquidGlassMetalView: MTKView {
    var renderer: LiquidGlassRenderer!
    
    override init(frame: CGRect, device: MTLDevice?) {
        super.init(frame: frame, device: device ?? MTLCreateSystemDefaultDevice())
        setup()
    }
    
    func setup() {
        framebufferOnly = false
        colorPixelFormat = .rgba16Float
        depthStencilPixelFormat = .depth24Stencil8
        sampleCount = 1
        preferredFramesPerSecond = 60
        
        renderer = LiquidGlassRenderer(device: device!)
        delegate = renderer
    }
}
```

### Renderer Structure (Matching Web Pipeline)

```swift
class LiquidGlassRenderer: NSObject, MTKViewDelegate {
    let device: MTLDevice
    let commandQueue: MTLCommandQueue
    
    // Pipelines, Textures, Uniforms...
    
    func draw(in view: MTKView) {
        guard let commandBuffer = commandQueue.makeCommandBuffer(),
              let currentDrawable = view.currentDrawable else { return }
        
        // Pass 1: Bg + Shadow -> Pass 2/3: Blur -> Pass 4: Composite
        // Follow the MultiPassRenderer logic
        
        commandBuffer.present(currentDrawable)
        commandBuffer.commit()
    }
}
```

### Metal Shaders (MSL)

```metal
// Ported WGSL/GLSL shaders
// Using: float2, float4, texture2d<float, access::sample>, sampler
fragment float4 mainFragment(...) {
    // ... logic
}
```