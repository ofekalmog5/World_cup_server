#include <iostream>
#include <string>
#include "StompProtocol.h"

// Minimal test helper to avoid external test frameworks.
namespace {
    int failures = 0;

    void require(bool condition, const std::string &name, const std::string &details = "") {
        if (!condition) {
            ++failures;
            std::cerr << "[FAIL] " << name;
            if (!details.empty()) {
                std::cerr << ": " << details;
            }
            std::cerr << std::endl;
        }
    }

    void testLoginFrame() {
        StompProtocol protocol;
        const std::string frame = protocol.processInput("login localhost:7777 alice secret");
        const std::string expected =
            "CONNECT\n"
            "accept-version:1.2\n"
            "host:stomp.cs.bgu.ac.il\n"
            "login:alice\n"
            "passcode:secret\n\n";
        require(frame == expected, "login builds CONNECT frame");
    }

    void testJoinExitLogoutFrames() {
        StompProtocol protocol;
        protocol.processInput("login localhost:7777 alice secret");

        const std::string joinFrame = protocol.processInput("join general");
        require(joinFrame == "SUBSCRIBE\ndestination:/general\nid:0\nreceipt:0\n\n",
                "join builds SUBSCRIBE frame");

        const std::string exitFrame = protocol.processInput("exit general");
        require(exitFrame == "UNSUBSCRIBE\nid:0\nreceipt:1\n\n",
                "exit builds UNSUBSCRIBE frame");

        const std::string logoutFrame = protocol.processInput("logout");
        require(logoutFrame == "DISCONNECT\nreceipt:2\n\n", "logout builds DISCONNECT frame");
    }

    void testReportProducesSendFrame() {
        // Skipped - requires events JSON file
    }

    void testReceiptLogoutTerminates() {
        StompProtocol protocol;
        protocol.processInput("login localhost:7777 alice secret");
        protocol.processInput("logout");

        protocol.processResponse("RECEIPT\nreceipt-id:0\n\n");
        require(protocol.isTerminated(), "receipt for logout terminates client");
    }

    void testErrorTerminates() {
        StompProtocol protocol;
        protocol.processResponse("ERROR\nmessage:oops\n\n");
        require(protocol.isTerminated(), "ERROR frame terminates client");
    }
}

int main() {
    testLoginFrame();
    testJoinExitLogoutFrames();
    testReportProducesSendFrame();
    testReceiptLogoutTerminates();
    testErrorTerminates();

    if (failures == 0) {
        std::cout << "All StompProtocol tests passed" << std::endl;
        return 0;
    }

    std::cerr << failures << " test(s) failed" << std::endl;
    return 1;
}
