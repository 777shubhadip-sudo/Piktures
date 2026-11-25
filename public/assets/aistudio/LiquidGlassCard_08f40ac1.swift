import SwiftUI

struct LiquidGlassCard: View {
    var body: some View {
        ZStack {
            // Native material fallback or custom Metal view
            VisualEffectView(material: .hudWindow, blendingMode: .behindWindow)
            
            VStack(alignment: .leading, spacing: 12) {
                Text("Liquid Glass")
                    .font(.headline)
                    .foregroundColor(.primary)
                Text("Physically convincing refraction, dispersion, and Fresnel reflections.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding(20)
        }
        .frame(width: 320, height: 180)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(.white.opacity(0.3), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.15), radius: 25, x: 0, y: 12)
    }
}
