# Accessibility Guide

Ensuring Liquid Glass interfaces remain usable for all users.

## Contrast & Legibility

1. **Transparency and Blur**:
   - Liquid Glass inherently reduces background contrast. 
   - **Requirement**: Ensure text placed inside or over glass has sufficient contrast (WCAG AA minimum: 4.5:1 for normal text, 3:1 for large text).
   - Use dynamic text colors (e.g., system label colors in macOS, white/black auto-switching).

2. **Reduce Transparency Fallback**:
   - Honor accessibility settings (`prefers-reduced-transparency` in CSS/macOS).
   - When reduced transparency is enabled:
     - Replace glass blur with solid or high-opacity neutral background.
     - Remove refraction/dispersion effects.
     - Maintain clear borders and shadows.

```css
@media (prefers-reduced-transparency: reduce) {
  .liquid-glass-element {
    background: var(--solid-panel-background);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
  }
}
```

```swift
// SwiftUI accessibility
@Environment(\.accessibilityReduceTransparency) var reduceTransparency

var body: some View {
    Group {
        if reduceTransparency {
            Color(.windowBackgroundColor)
        } else {
            GlassPanel()
        }
    }
}
```

## Motion Sensitivity

1. **Spring Animations & Follow Mouse**:
   - Honor `prefers-reduced-motion`.
   - Disable or damp spring animations and mouse tracking when users request reduced motion.

```typescript
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
if (reduceMotion) {
  // Disable followMouse and springs
}
```

## Screen Readers & Hit Testing

1. Glass effects are often drawn on `<canvas>` or `MTKView`.
2. **Requirement**: Ensure interactive glass elements (buttons, panels) have proper accessibility roles and aria-labels / accessibility descriptions attached to underlying DOM or accessibility elements.
3. Don't let decorative glass overlay block click events (`pointer-events: none` on non-interactive glass layers).
