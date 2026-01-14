#!/usr/bin/env python3
"""
System Readiness Verification Script
Checks all components are compiled and ready for testing
"""
import os
import sys

def check_file(path, description):
    """Check if file exists"""
    exists = os.path.exists(path)
    symbol = "✓" if exists else "✗"
    status = "OK" if exists else "MISSING"
    print(f"  {symbol} {description:50} {status}")
    return exists

def check_directory(path, description):
    """Check if directory exists"""
    exists = os.path.isdir(path)
    symbol = "✓" if exists else "✗"
    status = "OK" if exists else "MISSING"
    print(f"  {symbol} {description:50} {status}")
    return exists

def main():
    print("\n╔════════════════════════════════════════════════════════════════╗")
    print("║                                                                ║")
    print("║        STOMP SYSTEM READINESS VERIFICATION                    ║")
    print("║                                                                ║")
    print("╚════════════════════════════════════════════════════════════════╝\n")
    
    results = {}
    
    print("─" * 70)
    print("1. JAVA STOMP SERVER")
    print("─" * 70)
    
    server_dir = "server"
    results['server_exists'] = check_directory(server_dir, "Server directory")
    results['pom_exists'] = check_file(
        os.path.join(server_dir, "pom.xml"),
        "Maven POM file"
    )
    results['stomp_server'] = check_file(
        os.path.join(server_dir, "target", "classes", "bgu", "spl", "net", "impl", "stomp", "StompServer.class"),
        "Compiled StompServer.class"
    )
    results['stomp_protocol'] = check_file(
        os.path.join(server_dir, "target", "classes", "bgu", "spl", "net", "impl", "stomp", "StompMessagingProtocolImpl.class"),
        "Compiled StompMessagingProtocolImpl.class"
    )
    results['encoder'] = check_file(
        os.path.join(server_dir, "target", "classes", "bgu", "spl", "net", "impl", "stomp", "StompMessageEncoderDecoder.class"),
        "Compiled StompMessageEncoderDecoder.class"
    )
    
    print("\n─" * 70)
    print("2. C++ STOMP CLIENT")
    print("─" * 70)
    
    client_dir = "client"
    results['client_exists'] = check_directory(client_dir, "Client directory")
    results['makefile'] = check_file(
        os.path.join(client_dir, "makefile"),
        "Client Makefile"
    )
    results['client_binary'] = check_file(
        os.path.join(client_dir, "bin", "StompWCIClient.exe"),
        "Compiled StompWCIClient.exe binary"
    )
    results['cpp_sources'] = (
        check_file(os.path.join(client_dir, "src", "StompClient.cpp"), "StompClient.cpp") and
        check_file(os.path.join(client_dir, "src", "StompProtocol.cpp"), "StompProtocol.cpp") and
        check_file(os.path.join(client_dir, "src", "ConnectionHandler.cpp"), "ConnectionHandler.cpp") and
        check_file(os.path.join(client_dir, "src", "event.cpp"), "event.cpp")
    )
    
    print("\n─" * 70)
    print("3. PYTHON INFRASTRUCTURE")
    print("─" * 70)
    
    results['quick_test'] = check_file("quick_test.py", "quick_test.py (connectivity test)")
    results['debug_client'] = check_file("debug_client.py", "debug_client.py (debug test)")
    results['test_client'] = check_file("test_client.py", "test_client.py (full test)")
    results['full_test'] = check_file("full_test.py", "full_test.py (integration test)")
    results['sql_server'] = check_file(os.path.join("data", "sql_server.py"), "sql_server.py (database)")
    
    print("\n─" * 70)
    print("4. BATCH SCRIPTS")
    print("─" * 70)
    
    results['start_server'] = check_file("start_server.bat", "start_server.bat")
    results['run_client'] = check_file("run_client.bat", "run_client.bat")
    results['test_system'] = check_file("test_system.bat", "test_system.bat")
    
    print("\n─" * 70)
    print("5. DOCUMENTATION")
    print("─" * 70)
    
    results['readme'] = check_file("README.md", "README.md")
    results['test_guide'] = check_file("TEST_GUIDE.md", "TEST_GUIDE.md")
    results['implementation'] = check_file("IMPLEMENTATION_SUMMARY.txt", "IMPLEMENTATION_SUMMARY.txt")
    
    print("\n" + "=" * 70)
    print("VERIFICATION SUMMARY")
    print("=" * 70)
    
    total = len(results)
    passed = sum(1 for v in results.values() if v)
    failed = total - passed
    
    print(f"\nTotal Checks: {total}")
    print(f"Passed: {passed} ✓")
    print(f"Failed: {failed} ✗")
    
    if failed == 0:
        print("\n" + "╔" + "=" * 68 + "╗")
        print("║" + " " * 68 + "║")
        print("║" + "  ✓ ALL COMPONENTS READY FOR TESTING".center(68) + "║")
        print("║" + " " * 68 + "║")
        print("╚" + "=" * 68 + "╝")
        
        print("\n" + "─" * 70)
        print("QUICK START COMMANDS")
        print("─" * 70)
        print("\n1. Start Server (in dedicated terminal):")
        print("   > start_server.bat")
        print("\n2. Verify Server (in another terminal, after 2-3 seconds):")
        print("   > python quick_test.py")
        print("\n3. Run Client (in another terminal):")
        print("   > run_client.bat")
        print("   or")
        print("   > client\\bin\\StompWCIClient.exe")
        print("\n4. Full Integration Test:")
        print("   > python full_test.py")
        
        return 0
    else:
        print("\n" + "╔" + "=" * 68 + "╗")
        print("║" + " " * 68 + "║")
        print("║" + "  ⚠ MISSING COMPONENTS - SEE BELOW".center(68) + "║")
        print("║" + " " * 68 + "║")
        print("╚" + "=" * 68 + "╝")
        
        print("\nMissing Components:")
        for name, result in results.items():
            if not result:
                print(f"  ✗ {name}")
        
        print("\nTo fix:")
        print("  1. Compile Java server: cd server && mvn compile")
        print("  2. Compile C++ client: cd client && make StompWCIClient")
        print("  3. Verify Python is installed: python --version")
        
        return 1

if __name__ == "__main__":
    sys.exit(main())
