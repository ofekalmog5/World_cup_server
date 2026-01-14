# STOMP Assignment 3 - Complete System Test Guide

## Overview

This is a comprehensive STOMP (Streaming Text Oriented Messaging Protocol) implementation with:
- **Java Server** (Port 7777): Thread-Per-Client server with STOMP 1.2 protocol
- **C++ Client**: Interactive client for connecting and communicating
- **Python Infrastructure**: Test clients and database server
- **SQLite Database**: Optional data persistence

---

## Quick Start (5 minutes)

### Step 1: Start the Java STOMP Server

**Option A - Using Batch File:**
```bash
start_server.bat
```

**Option B - Using Command Line:**
```bash
cd server
mvn exec:java "-Dexec.mainClass=bgu.spl.net.impl.stomp.StompServer" "-Dexec.args=7777 tpc"
```

**Expected Output:**
```
[INFO] Server started on port 7777
```

Server will listen on port 7777 and accept client connections.

### Step 2: Verify Server is Running (in another terminal)

```bash
python quick_test.py
```

**Expected Output:**
```
✓ Port 7777 is OPEN
✓ Sent CONNECT frame
✓ Received response: XX bytes
Response content:
CONNECTED
version:1.2
```

### Step 3: Run C++ Client

```bash
client\bin\StompWCIClient.exe
```

Or use batch file:
```bash
run_client.bat
```

**Interactive Commands:**
```
login 127.0.0.1:7777 testuser testpass
join Germany_Japan
taskkill /PID <PID> /F
exit
```

---

## System Components

### 1. Java STOMP Server

**Location:** `server/src/main/java/bgu/spl/net/impl/stomp/`

**Main Class:** `StompServer.java`

**Features:**
- STOMP 1.2 Protocol Implementation
- User Authentication (login/passcode)
- Pub/Sub Messaging (SUBSCRIBE/SEND)
- Connection Management (CONNECT/DISCONNECT)
- Error Handling (ERROR frames)
- Message Acknowledgment (RECEIPT frames)

**Build:**
```bash
cd server
mvn compile
```

**Run (TPC - Thread-Per-Client):**
```bash
mvn exec:java "-Dexec.mainClass=bgu.spl.net.impl.stomp.StompServer" "-Dexec.args=7777 tpc"
```

**Run (Reactor - Non-blocking):**
```bash
mvn exec:java "-Dexec.mainClass=bgu.spl.net.impl.stomp.StompServer" "-Dexec.args=7777 reactor"
```

### 2. C++ STOMP Client

**Location:** `client/bin/StompWCIClient.exe` (compiled)

**Source:** `client/src/`

**Components:**
- `StompClient.cpp` - Main client logic
- `StompProtocol.cpp` - Protocol handling
- `ConnectionHandler.cpp` - Socket management
- `event.cpp` - Event parsing

**Build:**
```bash
cd client
make StompWCIClient
```

**Run:**
```bash
client\bin\StompWCIClient.exe
```

**Dependencies:**
- Boost.ASIO (networking)
- pthread (threading)
- Windows sockets (ws2_32)

### 3. Python Test Infrastructure

#### quick_test.py
- **Purpose:** Minimal server connectivity test
- **Usage:** `python quick_test.py`
- **Tests:** Port 7777 listening, CONNECT-CONNECTED exchange

#### debug_client.py
- **Purpose:** Server response debugging
- **Usage:** `python debug_client.py`
- **Tests:** Full CONNECT frame and response parsing

#### test_client.py
- **Purpose:** Complete STOMP protocol testing
- **Usage:** `python test_client.py`
- **Features:** SUBSCRIBE, message receiving, DISCONNECT

#### full_test.py
- **Purpose:** Full integration test
- **Usage:** `python full_test.py`
- **Tests:** Server, Client binary, SQL server, interactive sessions

### 4. SQL Database Server

**Location:** `data/sql_server.py`

**Port:** 8888

**Database:** `stomp_server.db` (SQLite)

**Tables:**
- `users` - username, password
- `logins` - login history with timestamps
- `reports` - event reports with metadata

**Start:**
```bash
python data/sql_server.py
```

---

## STOMP Protocol Reference

### Frame Format

All STOMP frames follow this structure:

```
COMMAND
header1: value1
header2: value2

[optional body]
\x00
```

**Key Points:**
- Space REQUIRED after colon in headers: `"header: value"`
- Null terminator (`\x00`) required at end
- Headers separated by newlines (`\n`)
- Blank line between headers and body

### Supported Commands

#### CONNECT
Authenticate and establish connection

**Request:**
```
CONNECT
accept-version:1.2
host:stomp.cs.bgu.ac.il
login:username
passcode:password

\x00
```

**Response (Success):**
```
CONNECTED
version:1.2

\x00
```

**Response (Failure):**
```
ERROR
message:Invalid credentials

\x00
```

#### SEND
Publish message to a destination

**Request:**
```
SEND
destination:channel_name

message content here
\x00
```

**Response:**
```
RECEIPT
receipt-id:ID

\x00
```

#### SUBSCRIBE
Subscribe to a channel

**Request:**
```
SUBSCRIBE
destination:channel_name
id:subscription_id

\x00
```

**Response:**
```
RECEIPT
receipt-id:ID

\x00
```

#### MESSAGE
Receive published message

**Format (from server):**
```
MESSAGE
destination:channel_name
message-id:ID

message content here
\x00
```

#### UNSUBSCRIBE
Unsubscribe from channel

**Request:**
```
UNSUBSCRIBE
id:subscription_id

\x00
```

#### DISCONNECT
Close connection

**Request:**
```
DISCONNECT
receipt:disconnect-receipt-123

\x00
```

**Response:**
```
RECEIPT
receipt-id:disconnect-receipt-123

\x00
```

#### ERROR
Error notification

**Format (from server):**
```
ERROR
message:Error description

\x00
```

---

## Testing Scenarios

### Scenario 1: Basic Server Connectivity

1. Start server: `start_server.bat`
2. Test connection: `python quick_test.py`
3. Expected: "Port 7777 is OPEN" and CONNECTED response

### Scenario 2: C++ Client Interactive

1. Start server: `start_server.bat`
2. Run client: `run_client.bat` or `client\bin\StompWCIClient.exe`
3. Commands:
   ```
   login 127.0.0.1:7777 testuser testpass
   join game1
   exit
   ```

### Scenario 3: Multi-Client Messaging

1. Start server: `start_server.bat`
2. Terminal 1: `python test_client.py` (subscribe to channel)
3. Terminal 2: `client\bin\StompWCIClient.exe` (send messages)
4. Verify: Messages appear in Terminal 1

### Scenario 4: Full Integration

1. Start server: `start_server.bat`
2. Run test: `python full_test.py`
3. Validates all components and server responses

---

## Troubleshooting

### Server Won't Start
**Problem:** "Error binding to port 7777"

**Solutions:**
- Check if another server is running: `netstat -ano | Select-String "7777"`
- Kill process using port: `taskkill /PID <pid> /F`
- Try different port: Add new port number to server start command

### Client Won't Connect
**Problem:** "Connection refused" or timeout

**Solutions:**
- Verify server is running: `python quick_test.py`
- Wait 2-3 seconds after server starts
- Check firewall settings
- Try localhost instead of 127.0.0.1

### Client Binary Not Found
**Problem:** `client\bin\StompWCIClient.exe` doesn't exist

**Solutions:**
```bash
cd client
make clean
make StompWCIClient
```

### Python Tests Timeout
**Problem:** `Timeout waiting for response`

**Solutions:**
- Ensure server started successfully
- Wait 3-5 seconds after starting server
- Check server terminal for error messages
- Try `python quick_test.py` for simpler test

### Port Detection Issues (Windows)
**Problem:** `netstat` shows port open but Python test says closed

**Solutions:**
- Add timeout: `ping localhost` before testing
- Use `netstat -ano | Select-String "7777"` to verify
- Restart server if recently changed

---

## File Structure

```
Assignment 3 SPL/
├── server/                          # Java STOMP Server
│   ├── src/main/java/bgu/spl/net/impl/stomp/
│   │   ├── StompServer.java
│   │   ├── StompMessagingProtocolImpl.java
│   │   ├── StompMessageEncoderDecoder.java
│   │   └── SqlBridge.java
│   ├── pom.xml                      # Maven configuration
│   └── target/classes/              # Compiled classes
│
├── client/                          # C++ STOMP Client
│   ├── bin/
│   │   └── StompWCIClient.exe       # Compiled binary
│   ├── src/
│   │   ├── StompClient.cpp
│   │   ├── StompProtocol.cpp
│   │   ├── ConnectionHandler.cpp
│   │   └── event.cpp
│   ├── include/
│   │   ├── StompProtocol.h
│   │   ├── ConnectionHandler.h
│   │   ├── event.h
│   │   └── json.hpp
│   └── makefile
│
├── data/                            # Data & SQL
│   ├── sql_server.py               # SQLite server
│   ├── stomp_server.db             # Database file
│   └── *.json                      # Test data
│
├── quick_test.py                   # Quick connectivity test
├── debug_client.py                 # Debug test
├── test_client.py                  # Full protocol test
├── full_test.py                    # Integration test
├── start_server.bat                # Start server batch script
├── run_client.bat                  # Run client batch script
├── test_system.bat                 # System test batch script
├── TEST_GUIDE.md                   # This guide
└── README.md                       # This file
```

---

## How the `report` Command Works

The `report` command is a key feature that reads game events from a JSON file and publishes them to the STOMP server. Understanding the complete flow is essential for proper usage.

### Command Flow Overview

```
User: report client/data/test_game.json
  ↓
C++ Client: Parse JSON file
  ↓
C++ Client: Create SEND frames for each event
  ↓
Server: Receive and validate SEND frames
  ↓
Server: Broadcast MESSAGE frames to all subscribers
  ↓
All Subscribed Clients: Receive event notifications
```

### Client-Side Processing (C++)

**File:** `client/src/StompProtocol.cpp` (lines 76-99)

**Step 1: Parse JSON File**
```cpp
names_and_events n_e = parseEventsFile(words[1]);  // words[1] = file path
```

The `parseEventsFile()` function (in `client/src/event.cpp`):
- Opens the JSON file
- Uses `nlohmann::json` library to parse structure
- Extracts team names and event list
- Creates `Event` objects for each event

**Step 2: Create SEND Frames**

For each event, the client constructs a STOMP SEND frame:

```
SEND
destination:/TeamA_TeamB

user:currentUsername
team a:Germany
team b:Japan
event name:goal
time:1980
general game updates:
    active:true
    before halftime:true
team a updates:
    goals:1
    yellow cards:0
    red cards:0
    possession:65%
team b updates:
    goals:0
    yellow cards:0
    red cards:0
    possession:35%
description:
Germany scores!
\x00
```

**Destination Format:** `/TeamA_TeamB` (e.g., `/Germany_Japan`)

**Step 3: Send All Frames**

All SEND frames are concatenated and sent to the server in sequence.

### JSON File Requirements

**CRITICAL:** The C++ parser has strict requirements. Every event must include:

1. **Required Top-Level Fields:**
   - `"team a"`: Team A name (string)
   - `"team b"`: Team B name (string)
   - `"events"`: Array of event objects

2. **Required Fields in Each Event:**
   - `"event name"`: Event type (string)
   - `"time"`: Time in seconds (number)
   - `"event owner"`: Username who created event (string, can be empty `""`)
   - `"general game updates"`: Object with game state
   - `"team a updates"`: Object with Team A statistics
   - `"team b updates"`: Object with Team B statistics
   - `"description"`: Event description (string)

3. **Required Fields in Team Updates:**
   - `"goals"`: Goal count (string like `"0"` or `"1"`)
   - `"yellow cards"`: Yellow card count (string)
   - `"red cards"`: Red card count (string)
   - `"possession"`: Possession percentage (string like `"50%"`)

**Valid Example:** `client/data/test_game.json`
```json
{
    "team a": "Germany",
    "team b": "Japan",
    "events": [
        {
            "event name": "kickoff",
            "time": 0,
            "event owner": "",
            "general game updates": {
                "active": "true",
                "before halftime": "true"
            },
            "team a updates": {
                "goals": "0",
                "yellow cards": "0",
                "red cards": "0",
                "possession": "50%"
            },
            "team b updates": {
                "goals": "0",
                "yellow cards": "0",
                "red cards": "0",
                "possession": "50%"
            },
            "description": "Game starts!"
        }
    ]
}
```

**Common Errors:**
- ❌ Empty objects `{}` in team updates → `type_error.302`
- ❌ Missing `"event owner"` field → `type_error.302`
- ❌ Null values instead of strings → `type_error.302`
- ❌ Missing any required field → Parser crash

### Server-Side Processing (Java)

**File:** `server/src/main/java/bgu/spl/net/impl/stomp/StompMessagingProtocolImpl.java` (lines 145-184)

**Step 1: Receive SEND Frame**

The `handleSend()` method processes incoming SEND frames:

```java
private void handleSend(String message) {
    Map<String, String> headers = parseHeaders(message);
    // ... validation ...
}
```

**Step 2: Validation Checks**

1. **User Authentication:** Is the user logged in?
   ```java
   if (this.currentUser == null) {
       sendError("Not Logged In", "You must send a CONNECT frame first", headers);
       return;
   }
   ```

2. **Destination Present:** Does the SEND frame have a destination?
   ```java
   String destination = headers.get("destination");
   if (destination == null) {
       sendError("Malformed Frame", "Missing destination header", headers);
       return;
   }
   ```

3. **🚨 Subscription Required:** Is the user subscribed to this channel?
   ```java
   if (!activeSubscriptions.containsValue(destination)) {
       sendError("Not Subscribed", "You cannot send messages to a channel you are not subscribed to", headers);
       return;
   }
   ```
   
   **This is the most common failure point!** You MUST use the `join` command before `report`.

**Step 3: Broadcast MESSAGE Frame**

If validation passes, the server:
1. Extracts the message body
2. Creates a MESSAGE frame with unique message ID
3. Broadcasts to ALL subscribers of that destination

```java
String messageFrame = "MESSAGE\n" +
                      "message-id:" + msgId + "\n" +
                      "destination:" + destination + "\n" +
                      "\n" +
                      body + "\n" +
                      "\u0000";

connections.send(destination, messageFrame);
```

**Step 4: Send Receipt**

If the client requested a receipt, the server sends:
```
RECEIPT
receipt-id:ID

\x00
```

### Proper Usage Sequence

**❌ WRONG (will fail):**
```
login 127.0.0.1:7777 testuser testpass
report client/data/test_game.json          ← ERROR: Not Subscribed
```

**✅ CORRECT:**
```
login 127.0.0.1:7777 testuser testpass
join Germany_Japan                         ← SUBSCRIBE to channel first
report client/data/test_game.json          ← Now sends successfully
```

**Channel Naming:**
- The destination is derived from team names: `/TeamA_TeamB`
- If `test_game.json` has `"team a": "Germany"` and `"team b": "Japan"`
- You must `join Germany_Japan` to receive those events

### Multi-Client Scenario

**Terminal 1 (Receiver):**
```
client\bin\StompWCIClient.exe
login 127.0.0.1:7777 alice password
join Germany_Japan
[Waiting... will receive MESSAGE frames]
```

**Terminal 2 (Sender):**
```
client\bin\StompWCIClient.exe
login 127.0.0.1:7777 bob password
join Germany_Japan
report client/data/test_game.json
[Events sent to server]
```

**Result:** Alice receives all events from Bob's report as MESSAGE frames.

### Debugging Report Issues

**Problem:** `type_error.302: type must be string, but is null`

**Solution:** Check JSON file structure
- Ensure all required fields present
- No empty objects `{}`
- All values as strings (not `null`)
- Use `client/data/test_game.json` as template

**Problem:** Server sends ERROR: "Not Subscribed"

**Solution:** Use `join` before `report`
- Channel name must match: `/TeamA_TeamB`
- Example: `join Germany_Japan` for teams "Germany" and "Japan"

**Problem:** No messages received

**Solution:** Check subscriptions
- Both sender and receiver must join the same channel
- Server only broadcasts to active subscribers
- Use exact channel name (case-sensitive)

**Problem:** File not found

**Solution:** Use correct path
- Relative to current directory: `client/data/test_game.json`
- Or use absolute path: `C:/full/path/to/file.json`
- Check file exists with correct spelling

### Technical Implementation Notes

**C++ JSON Parser:**
- Uses `nlohmann::json` library (v3.11.2)
- Strict type checking (no automatic conversions)
- Field access with `.get<std::string>()` requires field exists
- Missing fields cause `type_error` exceptions

**Server Pub/Sub:**
- Uses `ConnectionsImpl<T>` with `ConcurrentHashMap`
- Maps destinations to connection IDs
- Thread-safe message broadcasting
- Subscription state per connection

**Message ID:**
- Server generates unique IDs using `AtomicInteger`
- Clients use message-id for acknowledgment
- IDs increment sequentially per server instance

---

## Performance Notes

### TPC vs Reactor
- **TPC (Thread-Per-Client):** One thread per connection. Good for few clients.
- **Reactor:** Single thread handling multiple connections. Better scalability.

### Connection Limits
- **TPC:** Limited by system thread count (typically 100-1000)
- **Reactor:** Limited by file descriptors (typically 10,000+)

### Message Latency
- STOMP protocol has minimal overhead
- Java server processes messages in real-time
- Network latency dominates total delay

---

## Implementation Details

### Key Architecture Decisions

1. **StompMessagingProtocol Interface**: All servers use this unified interface
2. **Connections<T> Infrastructure**: Pub/sub system for message broadcasting
3. **Unsubscribe-Before-Disconnect**: Prevents race conditions where messages arrive after RECEIPT
4. **STOMP 1.2 Compliance**: Exact format matching (header spacing critical)

### Thread Safety

- `ConnectionsImpl<T>` uses `ConcurrentHashMap`
- All public methods are thread-safe
- Database access synchronized via SqlBridge

### Error Handling

- Invalid frames: Send ERROR and disconnect
- Database errors: Send ERROR to client
- Network errors: Clean up connection silently
- Protocol violations: Send ERROR with description

---

## Development Notes

### Adding New Commands

1. Add handler in `StompMessagingProtocolImpl.process()`
2. Parse frame in `parseHeaders()` method
3. Implement logic with `connections.send()` for broadcast
4. Send RECEIPT if requested via `sendReceiptIfRequested()`
5. Test with `quick_test.py`

### Debugging Server

Enable verbose logging:
```bash
cd server
mvn exec:java "-Dexec.mainClass=bgu.spl.net.impl.stomp.StompServer" "-Dexec.args=7777 tpc" -X
```

### Debugging C++ Client

Add debug output in `StompProtocol.cpp`:
```cpp
std::cout << "Sending frame: " << frame << std::endl;
```

---

## References

- STOMP 1.2 Specification: http://stomp.github.io/stomp-specification-1.2.html
- Boost.ASIO Documentation: https://think-async.com/Asio/
- Maven Documentation: https://maven.apache.org/

---

## Support

For issues or questions:

1. Check `TEST_GUIDE.md` for detailed component information
2. Review STOMP protocol reference in this document
3. Check server logs for error messages
4. Test with `quick_test.py` for connectivity verification
5. Test with C++ client for full interaction testing

---

**Last Updated:** 2026-01-14  
**Status:** Production Ready  
**Components:** Server ✓ Client ✓ Database ✓ Tests ✓
