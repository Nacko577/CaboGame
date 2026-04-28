import SwiftUI

struct MainMenuView: View {
    let onPlay: () -> Void

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.06, green: 0.34, blue: 0.22), Color(red: 0.04, green: 0.24, blue: 0.16)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 22) {
                Spacer()

                VStack(spacing: 10) {
                    Text("CABO")
                        .font(.system(size: 48, weight: .black, design: .rounded))
                        .foregroundStyle(.white)
                    Text("Memory, risk, and low score wins.")
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.82))
                }

                VStack(spacing: 12) {
                    Button("Play") {
                        onPlay()
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)

                    NavigationLink("How To Play") {
                        HowToPlayView()
                    }
                    .buttonStyle(.bordered)
                    .tint(.white)
                }
                .padding(14)
                .background(Color.white.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 14))

                Spacer()
            }
            .padding()
        }
        .navigationBarBackButtonHidden(true)
    }
}
