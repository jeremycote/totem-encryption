import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .onAppear(perform: {
                    let result = getIPAddressAndSubnetMask()
                    let ip = result.ip
                    let subnet = result.subnet
                    if ip != "" && subnet != "" {
                        print("IP Address: \(ip), Subnet Mask: \(subnet)")
                    } else {
                        print("Could not retrieve IP address or subnet mask.")
                    }
                })
    }
}



