#!/usr/bin/env python3
"""
Simple STOMP client test to debug server issues
"""
import socket
import time
import sys

def send_connect():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(2)
    
    try:
        print("Connecting to localhost:7777...")
        sock.connect(('localhost', 7777))
        print("Connected!")
        
        # Send CONNECT frame
        connect_frame = b"CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:testuser\npasscode:testpass\n\n\x00"
        print(f"\nSending CONNECT:\n{connect_frame.decode('utf-8', errors='replace')}")
        sock.sendall(connect_frame)
        
        # Try to receive response
        print("\nWaiting for response...")
        data = b''
        while b'\x00' not in data:
            chunk = sock.recv(1024)
            if not chunk:
                print("Connection closed by server")
                break
            data += chunk
            print(f"Received chunk: {chunk}")
        
        print(f"\nFull response:\n{data.decode('utf-8', errors='replace')}")
        
    except socket.timeout:
        print("Timeout waiting for response")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        sock.close()

if __name__ == '__main__':
    send_connect()
