#pragma once
#include <string>
#include <map>
#include <vector>
#include <atomic>
#include "../include/ConnectionHandler.h"
#include "../include/event.h" 

class StompProtocol {
private:
    std::string currentUsername; 
    std::atomic<int> subscriptionCounter;
    std::atomic<int> receiptCounter;
    mutable std::mutex _mutex;
    std::map<int, std::string> subIdToChannel;
    std::map<std::string, int> channelToSubId; 
    std::map<int, std::string> receiptIdToCommand;

    std::map<std::string, std::vector<Event>> gameReports;
    
    bool shouldTerminate;

public:
    StompProtocol();
    
    std::vector<std::string> split(const std::string& str, char delimiter);

    std::string processInput(std::string input);
    void processResponse(std::string frame);
    bool isTerminated() const;
};