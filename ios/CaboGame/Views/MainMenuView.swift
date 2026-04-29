import SwiftUI

struct MainMenuView: View {
    let onPlay: () -> Void
    
    var body: some View {
        ZStack {
            // Rich gradient background
            LinearGradient(
                colors: [
                    Color(red: 0.08, green: 0.12, blue: 0.10),
                    Color(red: 0.04, green: 0.08, blue: 0.06)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            // Decorative card shapes in background
            GeometryReader { geometry in
                ZStack {
                    // Top left card
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.white.opacity(0.03))
                        .frame(width: 60, height: 84)
                        .rotationEffect(.degrees(-15))
                        .offset(x: -20, y: geometry.size.height * 0.15)
                    
                    // Top right card
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.white.opacity(0.03))
                        .frame(width: 60, height: 84)
                        .rotationEffect(.degrees(20))
                        .offset(x: geometry.size.width - 40, y: geometry.size.height * 0.1)
                    
                    // Bottom left card
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.white.opacity(0.02))
                        .frame(width: 50, height: 70)
                        .rotationEffect(.degrees(25))
                        .offset(x: 10, y: geometry.size.height * 0.75)
                    
                    // Bottom right card
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.white.opacity(0.02))
                        .frame(width: 50, height: 70)
                        .rotationEffect(.degrees(-10))
                        .offset(x: geometry.size.width - 70, y: geometry.size.height * 0.8)
                }
            }
            
            VStack(spacing: 0) {
                Spacer()
                
                // Logo section
                VStack(spacing: 16) {
                    // Card icon above title
                    ZStack {
                        RoundedRectangle(cornerRadius: 6)
                            .fill(Color(red: 0.18, green: 0.75, blue: 0.55))
                            .frame(width: 44, height: 62)
                            .rotationEffect(.degrees(-8))
                            .offset(x: -12)
                        
                        RoundedRectangle(cornerRadius: 6)
                            .fill(Color(red: 0.95, green: 0.85, blue: 0.65))
                            .frame(width: 44, height: 62)
                            .rotationEffect(.degrees(8))
                            .offset(x: 12)
                    }
                    .padding(.bottom, 8)
                    
                    Text("CABO")
                        .font(.system(size: 56, weight: .black, design: .rounded))
                        .tracking(4)
                        .foregroundStyle(
                            LinearGradient(
                                colors: [
                                    Color(red: 0.95, green: 0.90, blue: 0.80),
                                    Color(red: 0.85, green: 0.78, blue: 0.65)
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                    
                }
                
                Spacer()
                    .frame(height: 80)
                
                // Action buttons
                VStack(spacing: 14) {
                    Button(action: onPlay) {
                        HStack(spacing: 10) {
                            Image(systemName: "play.fill")
                                .font(.system(size: 16, weight: .semibold))
                            Text("Play")
                                .font(.system(size: 18, weight: .bold, design: .rounded))
                        }
                        .foregroundColor(Color(red: 0.06, green: 0.10, blue: 0.08))
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            LinearGradient(
                                colors: [
                                    Color(red: 0.18, green: 0.75, blue: 0.55),
                                    Color(red: 0.14, green: 0.62, blue: 0.45)
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .shadow(color: Color(red: 0.18, green: 0.75, blue: 0.55).opacity(0.4), radius: 12, y: 6)
                    }
                    .buttonStyle(ScaleButtonStyle())
                    
                    NavigationLink {
                        HowToPlayView()
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "questionmark.circle")
                                .font(.system(size: 16, weight: .semibold))
                            Text("How To Play")
                                .font(.system(size: 16, weight: .semibold, design: .rounded))
                        }
                        .foregroundColor(.white.opacity(0.85))
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.white.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(Color.white.opacity(0.15), lineWidth: 1)
                        )
                    }
                    .buttonStyle(ScaleButtonStyle())
                }
                .padding(.horizontal, 32)
                
                Spacer()
                    .frame(height: 60)
                
                // Version text
                Text("v1.0")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color.white.opacity(0.25))
                    .padding(.bottom, 20)
            }
            .padding()
        }
        .navigationBarBackButtonHidden(true)
    }
}

// Custom button style for scale animation
struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.easeInOut(duration: 0.15), value: configuration.isPressed)
    }
}