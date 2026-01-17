#!/usr/bin/env python3
"""
Full Integration Test: Java Server + C++ Client + Python SQL Server
"""
import socket
import subprocess
import time
import sys
import os
import signal
import threading

def is_port_open(port, timeout=1):
    """Check if a port is open"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        result = sock.connect_ex(('127.0.0.1', port))
        sock.close()
        return result == 0
    except:
        return False

def wait_for_port(port, timeout=5, check_interval=0.5):
    """Wait for port to open"""
    start_time = time.time()
    while time.time() - start_time < timeout:
        if is_port_open(port):
            return True
        time.sleep(check_interval)
    return False

def test_java_server():
    """Test Java STOMP Server"""
    print("\n" + "="*60)
    print("TEST 1: Java STOMP Server")
    print("="*60)
    
    # Wait for server to be ready
    print("[1/2] Waiting for server on port 7777...")
    if wait_for_port(7777, timeout=3):
        print("✓ Server is listening")
    else:
        print("⚠ Server may not be fully ready, continuing anyway...")
    
    # Test CONNECT frame
    print("[2/2] Testing CONNECT frame...")
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(2)
        sock.connect(('127.0.0.1', 7777))
        
        # Send CONNECT frame
        connect_frame = b"CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:testuser\npasscode:testpass\n\n\x00"
        sock.sendall(connect_frame)
        
        # Receive CONNECTED
        data = b''
        while b'\x00' not in data:
            chunk = sock.recv(1024)
            if not chunk:
                break
            data += chunk
        
        if b'CONNECTED' in data:
            print("✓ Server responds with CONNECTED frame")
            print(f"  Response: {data.decode('utf-8', errors='replace')[:100]}...")
            sock.close()
            return True
        else:
            print("✗ Server did not respond with CONNECTED")
            sock.close()
            return False
    except Exception as e:
        print(f"✗ Connection failed: {e}")
        return False

def test_sql_server():
    """Test SQL Server"""
    print("\n" + "="*60)
    print("TEST 2: Python SQL Server")
    print("="*60)
    
    if wait_for_port(8888, timeout=2):
        print("✓ SQL Server is listening on port 8888")
        return True
    else:
        print("⚠ SQL Server not running (this is optional)")
        return False

def test_cpp_client():
    """Test C++ Client Binary"""
    print("\n" + "="*60)
    print("TEST 3: C++ Client Binary")
    print("="*60)
    
    client_path = os.path.join(os.path.dirname(__file__), "client", "bin", "StompWCIClient.exe")
    
    if os.path.exists(client_path):
        print(f"✓ C++ Client binary exists: {client_path}")
        return True
    else:
        print(f"✗ C++ Client binary not found: {client_path}")
        return False

def interactive_cpp_test():
    """Interactive C++ Client Test"""
    print("\n" + "="*60)
    print("TEST 4: Interactive C++ Client Test")
    print("="*60)
    
    client_path = os.path.join(os.path.dirname(__file__), "client", "bin", "StompWCIClient.exe")
    
    if not os.path.exists(client_path):
        print("✗ Client binary not found, skipping interactive test")
        return False
    
    print("\nStarting C++ client for interactive testing...")
    print("Input format: <command>\n")
    print("Example commands:")
    print("  login 127.0.0.1:7777 testuser testpass")
    print("  join game_channel")
    print("  exit")
    print()
    
    try:
        # Start client process
        process = subprocess.Popen(
            [client_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1
        )
        
        # Send test commands
        test_commands = [
            "login 127.0.0.1:7777 testuser testpass\n",
            "join game1\n",
            "exit\n"
        ]
        
        print("Sending test commands...")
        for cmd in test_commands:
            print(f"→ {cmd.strip()}")
            try:
                process.stdin.write(cmd)
                process.stdin.flush()
            except BrokenPipeError:
                print("  (Client disconnected)")
                break
            time.sleep(0.5)
        
        # Give client time to process
        time.sleep(1)
        
        # Check if still running
        if process.poll() is None:
            print("✓ Client running successfully")
            process.terminate()
            try:
                process.wait(timeout=2)
            except subprocess.TimeoutExpired:
                process.kill()
            return True
        else:
            stdout, stderr = process.communicate()
            if stdout:
                print(f"Output: {stdout[:200]}")
            if stderr:
                print(f"Errors: {stderr[:200]}")
            return False
            
    except Exception as e:
        print(f"✗ Failed to run client: {e}")
        return False

def main():
    print("\n╔" + "="*58 + "╗")
    print("║" + " "*58 + "║")
    print("║" + "  FULL STOMP SYSTEM INTEGRATION TEST".center(58) + "║")
    print("║" + " "*58 + "║")
    print("╚" + "="*58 + "╝")
    
    results = {}
    
    # Run tests
    results['java_server'] = test_java_server()
    results['cpp_client'] = test_cpp_client()
    results['sql_server'] = test_sql_server()
    
    # Only run interactive test if server and client are ready
    if results['java_server'] and results['cpp_client']:
        response = input("\nRun interactive C++ client test? (y/n): ").strip().lower()
        if response == 'y':
            results['interactive'] = interactive_cpp_test()
    
    # Summary
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    
    for test_name, result in results.items():
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"{test_name:20} {status}")
    
    print("="*60)
    
    return all(results.values())

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
