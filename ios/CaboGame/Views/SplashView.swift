import SwiftUI

struct SplashView: View {
    @State private var logoScale: CGFloat = 0.97

    private let bgTop = Color(red: 0.047, green: 0.094, blue: 0.078) // #0C1814
    private let bgBottom = Color(red: 0.020, green: 0.039, blue: 0.031) // #050A08
    private let glowCenter = Color(red: 0.12, green: 0.56, blue: 0.39) // teal glow

    var body: some View {
        ZStack {
            LinearGradient(colors: [bgTop, bgBottom], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()

            RadialGradient(
                colors: [glowCenter.opacity(0.14), .clear],
                center: UnitPoint(x: 0.5, y: 0.42),
                startRadius: 0,
                endRadius: 520
            )
            .ignoresSafeArea()

            GeometryReader { geo in
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color.white.opacity(0.05))
                        .frame(width: 56, height: 78)
                        .rotationEffect(.degrees(-14))
                        .offset(x: -12, y: geo.size.height * 0.12)

                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color.white.opacity(0.045))
                        .frame(width: 52, height: 72)
                        .rotationEffect(.degrees(18))
                        .offset(x: geo.size.width - 64, y: geo.size.height * 0.08)

                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color.white.opacity(0.035))
                        .frame(width: 48, height: 66)
                        .rotationEffect(.degrees(22))
                        .offset(x: 14, y: geo.size.height - 100)

                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color.white.opacity(0.04))
                        .frame(width: 50, height: 70)
                        .rotationEffect(.degrees(-11))
                        .offset(x: geo.size.width - 56, y: geo.size.height - 88)
                }
            }

            VStack(spacing: 0) {
                Spacer()

                VStack(spacing: 20) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(
                                LinearGradient(
                                    colors: [
                                        Color(red: 0.20, green: 0.79, blue: 0.58),
                                        Color(red: 0.14, green: 0.60, blue: 0.45)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 46, height: 64)
                            .rotationEffect(.degrees(-10))
                            .offset(x: -10, y: 4)

                        RoundedRectangle(cornerRadius: 8)
                            .fill(
                                LinearGradient(
                                    colors: [
                                        Color(red: 0.96, green: 0.89, blue: 0.76),
                                        Color(red: 0.83, green: 0.72, blue: 0.59)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 46, height: 64)
                            .rotationEffect(.degrees(10))
                            .offset(x: 10, y: 4)
                    }
                    .frame(height: 72)

                    Text("CABO")
                        .font(.system(size: 52, weight: .black, design: .default))
                        .tracking(6)
                        .foregroundStyle(Color(red: 0.941, green: 0.898, blue: 0.80)) // #F0E5CC
                }
                .scaleEffect(logoScale)

                Spacer()
                    .frame(height: 36)

                SplashArcLoader()

                Spacer()
                    .frame(height: 16)

                Text("Shuffling…")
                    .font(.system(size: 14, weight: .medium, design: .default))
                    .foregroundStyle(Color.white.opacity(0.42))
                    .tracking(1)

                Spacer()
            }
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 1.4).repeatForever(autoreverses: true)) {
                logoScale = 1.03
            }
        }
    }
}

// MARK: - Thin rotating arc (matches Android splash loader)

private struct SplashArcLoader: View {
    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 60.0)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            let cycle: CGFloat = 0.9
            let rotation = CGFloat((t.truncatingRemainder(dividingBy: Double(cycle))) / Double(cycle)) * 360

            Canvas { cx, size in
                let inset: CGFloat = 4
                let r = min(size.width, size.height) / 2 - inset
                let c = CGPoint(x: size.width / 2, y: size.height / 2)
                let rect = CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)

                let track = Path(ellipseIn: rect)
                cx.stroke(track, with: .color(.white.opacity(0.12)), style: StrokeStyle(lineWidth: 2.6, lineCap: .round))

                var arc = Path()
                arc.addArc(
                    center: c,
                    radius: r,
                    startAngle: .degrees(Double(rotation)),
                    endAngle: .degrees(Double(rotation) + 88),
                    clockwise: false
                )
                cx.stroke(arc, with: .color(.white.opacity(0.92)), style: StrokeStyle(lineWidth: 3, lineCap: .round))
            }
            .frame(width: 44, height: 44)
        }
    }
}

#Preview {
    SplashView()
}
