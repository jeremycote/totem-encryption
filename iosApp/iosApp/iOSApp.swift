import SwiftUI
import ComposeApp

class SwiftInterfaceManager: SystemInterfaceManager {
    func getIP() -> String {
        let result = getIPAddressAndSubnetMask()
        return result.ip
    }
    
    func getMask() -> String {
        let result = getIPAddressAndSubnetMask()
        return result.subnet
    }
}

@main
struct iOSApp: App {
    
    let manager: SwiftInterfaceManager;
    
    init() {
        manager = SwiftInterfaceManager()
        KoinHelperKt.doInitDependencies(systemInterfaceManager: manager)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
