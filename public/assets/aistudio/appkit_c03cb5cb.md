# AppKit Implementation Reference

Low-level AppKit integration details for Liquid Glass UI.

## NSWindow & NSView Setup

```swift
import Cocoa
import MetalKit

class LiquidGlassWindowController: NSWindowController {
    convenience init() {
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 800, height: 600),
            styleMask: [.titled, .closable, .miniaturizable, .resizable, .fullSizeContentView],
            backing: .buffered,
            defer: false
        )
        window.titlebarAppearsTransparent = true
        window.titleVisibility = .hidden
        window.backgroundColor = .clear
        window.isOpaque = false
        window.hasShadow = true
        window.center()
        
        let metalView = LiquidGlassMetalView(frame: window.contentView!.bounds)
        metalView.autoresizingMask = [.width, .height]
        window.contentView?.addSubview(metalView)
        
        self.init(window: window)
    }
}
```

## Hosting Layer & Caching

- When using AppKit with SwiftUI (`NSHostingView`), ensure `wantsLayer = true` and `layer?.backgroundColor = NSColor.clear.cgColor`.
- For background caching, render static background elements once into a texture rather than redrawing every frame.
