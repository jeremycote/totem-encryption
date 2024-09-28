//
//  Network.swift
//  iosApp
//
//  Created by Jeremy Cote on 2024-09-28.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

func getIPAddressAndSubnetMask() -> (ip: String, subnet: String) {
    var address: String?
    var subnetMask: String?

    var ifaddr: UnsafeMutablePointer<ifaddrs>?
    if getifaddrs(&ifaddr) == 0 {
        var ptr = ifaddr
        while ptr != nil {
            guard let interface = ptr?.pointee else { continue }
            let name = String(cString: interface.ifa_name)
            let addrFamily = interface.ifa_addr.pointee.sa_family
            
            if name == "en0" && addrFamily == UInt8(AF_INET) {
                // Get the IP Address
                var addr = interface.ifa_addr.pointee
                var ipAddressString = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                getnameinfo(&addr, socklen_t(interface.ifa_addr.pointee.sa_len), &ipAddressString, socklen_t(ipAddressString.count), nil, socklen_t(0), NI_NUMERICHOST)
                address = String(cString: ipAddressString)
                
                // Get the Subnet Mask
                var netmask = interface.ifa_netmask.pointee
                var subnetMaskString = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                getnameinfo(&netmask, socklen_t(interface.ifa_netmask.pointee.sa_len), &subnetMaskString, socklen_t(subnetMaskString.count), nil, socklen_t(0), NI_NUMERICHOST)
                subnetMask = String(cString: subnetMaskString)
            }
            ptr = ptr?.pointee.ifa_next
        }
        freeifaddrs(ifaddr)
    }
    return (address ?? "", subnetMask ?? "")
}
