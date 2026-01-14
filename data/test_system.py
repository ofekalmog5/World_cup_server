#!/usr/bin/env python3
"""
Complete STOMP System Integration Test
Tests Java Server + C++ Client + Python SQL Server
"""
import subprocess
import socket
import time
import sys

def is_port_open(port):
    """Check if a port is open"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(1)
    try:
        result = sock.connect_ex(('localhost', port))
        sock.close()
        return result == 0
    except:
        return False

def wait_for_port(port, timeout=5):
    """Wait for a port to be open"""
    start = time.time()
    while time.time() - start < timeout:
        if is_port_open(port):
            print(f"✓ Port {port} is open")
            return True
        time.sleep(0.5)
    print(f"✗ Port {port} did not open within {timeout}s")
    return False

def test_server_response():
    """Test that server responds to CONNECT frame"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(2)
        sock.connect(('localhost', 7777))
        
        # Send CONNECT
        connect_frame = b"CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:testuser\npasscode:testpass\n\n\x00"
        sock.sendall(connect_frame)
        
        # Wait for CONNECTED response
        data = b''
        while b'\x00' not in data:
            chunk = sock.recv(1024)
            if not chunk:
                break
            data += chunk
        
        response = data.decode('utf-8', errors='replace')
        if 'CONNECTED' in response:
            print("✓ Server responds with CONNECTED frame")
            return True
        else:
            print(f"✗ Server response unexpected: {response[:50]}")
            return False
    except Exception as e:
        print(f"✗ Server connection test failed: {e}")
        return False
    finally:
        sock.close()

print("\n" + "="*60)
print("STOMP SYSTEM INTEGRATION TEST")
print("="*60)

print("\n[1/3] Checking STOMP Server...")
if wait_for_port(7777):
    if test_server_response():
        print("    ✓ Java STOMP Server is working")
    else:
        print("    ✗ Java STOMP Server not responding correctly")
        sys.exit(1)
else:
    print("    ✗ Java STOMP Server not running")
    print("    Start it with: mvn exec:java -Dexec.mainClass=bgu.spl.net.impl.stomp.StompServer -Dexec.args='7777 tpc'")
    sys.exit(1)

print("\n[2/3] Checking Python SQL Server...")
if wait_for_port(8888):
    print("    ✓ Python SQL Server is running")
else:
    print("    ⚠ Python SQL Server not found (optional)")

print("\n[3/3] Checking C++ Client...")
client_path = "client\\bin\\StompWCIClient.exe"
try:
    # Just check if file exists
    import os
    if os.path.exists(client_path):
        print(f"    ✓ C++ Client compiled: {client_path}")
    else:
        print(f"    ✗ C++ Client not found: {client_path}")
        sys.exit(1)
except Exception as e:
    print(f"    ✗ Error checking C++ client: {e}")
    sys.exit(1)

print("\n" + "="*60)
print("SYSTEM STATUS: ✓ READY FOR TESTING")
print("="*60)

print("\nTo test:")
print("1. Run C++ client: " + client_path)
print("2. Login with: login 127.0.0.1:7777 username password")
print("3. Try commands: join, exit, etc.")
print("\nServer is listening on:")
print("  - Java STOMP: localhost:7777")
print("  - Python SQL: localhost:8888 (if running)")
print("\n" + "="*60 + "\n")
