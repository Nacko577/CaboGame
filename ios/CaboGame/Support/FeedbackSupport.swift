import Foundation
import UIKit

/// Address shown in Mail / mailto.
enum FeedbackSupport {
    static let recipientEmail = "f.alexandru577@gmail.com"

    static var diagnosticFooter: String {
        let ver = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "?"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "?"
        let os = UIDevice.current.systemVersion
        let model = UIDevice.current.model
        return "\n\n—\nVersion \(ver) (\(build))\niOS \(os), \(model)"
    }

    static func mailtoURL(subject: String = "Cabo feedback") -> URL? {
        var c = URLComponents()
        c.scheme = "mailto"
        c.path = recipientEmail
        c.queryItems = [
            URLQueryItem(name: "subject", value: subject),
            URLQueryItem(name: "body", value: diagnosticFooter),
        ]
        return c.url
    }
}
