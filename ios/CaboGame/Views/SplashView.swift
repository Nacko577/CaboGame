import SwiftUI

struct SplashView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.07, green: 0.29, blue: 0.18), Color(red: 0.03, green: 0.14, blue: 0.10)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 14) {
                Image(systemName: "suit.club.fill")
                    .font(.system(size: 64, weight: .bold))
                    .foregroundStyle(.white.opacity(0.9))
                Text("CABO")
                    .font(.system(size: 44, weight: .heavy, design: .rounded))
                    .foregroundStyle(.white)
                ProgressView()
                    .tint(.white)
                    .scaleEffect(1.2)
            }
        }
    }
}
