#include "../include/StompProtocol.h"
#include "../include/event.h"
#include <sstream>
#include <iostream>
#include <fstream>
#include <chrono>
#include <thread>

StompProtocol::StompProtocol() : 
    currentUsername(""), 
    subscriptionCounter(0), 
    receiptCounter(0), 
    subIdToChannel(), 
    channelToSubId(), 
    receiptIdToCommand(), 
    gameReports(), 
    shouldTerminate(false) {}

    std::vector<std::string> StompProtocol::split(const std::string& str, char delimiter) {
    std::vector<std::string> tokens;
    std::string token;
    std::istringstream tokenStream(str);
    while (std::getline(tokenStream, token, delimiter)) {
        tokens.push_back(token);
    }
    return tokens;
}
std::string StompProtocol::processInput(std::string input) {
    std::lock_guard<std::mutex> lock(_mutex);
    std::vector<std::string> words = split(input, ' ');
    if (words.empty()) return "";

    std::string command = words[0];
    if (command != "login" && currentUsername == "") {
        std::cout << "Error: You must login before performing any other action." << std::endl;
        return ""; 
    }
    if (command == "login") {
        if (currentUsername != "") {
            std::cout << "Error: You are already logged in as " << currentUsername << std::endl;
            return "";
        }
        this->currentUsername = words[2];
        std::string frame = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:" + words[2] + "\npasscode:" + words[3] + "\n\n";
        return frame;
    }

    if (command == "join") {
        std::string gameName = words[1];
        int subId = subscriptionCounter++;
        int recId = receiptCounter++;
        
        subIdToChannel[subId] = gameName;
        channelToSubId[gameName] = subId;
        receiptIdToCommand[recId] = "Joined channel " + gameName;

        std::string frame = "SUBSCRIBE\ndestination:/" + gameName + "\nid:" + std::to_string(subId) + "\nreceipt:" + std::to_string(recId) + "\n\n";
        return frame;
    }

    if (command == "exit") {
        // Check if channel name provided (unsubscribe from channel)
        if (words.size() > 1) {
            std::string gameName = words[1];
            if (channelToSubId.count(gameName) == 0) {
                std::cout << "Error: You are not subscribed to channel " << gameName << std::endl;
                return "";
            }

            int subId = channelToSubId[gameName];
            int recId = receiptCounter++;
            receiptIdToCommand[recId] = "Exited channel " + gameName;

            std::string frame = "UNSUBSCRIBE\nid:" + std::to_string(subId) + "\nreceipt:" + std::to_string(recId) + "\n\n";
            return frame;
        } else {
            // No channel specified - disconnect from server (logout)
            int recId = receiptCounter++;
            receiptIdToCommand[recId] = "logout";
            this->currentUsername = "";  // Clear username on disconnect
            subIdToChannel.clear();       // Clear subscriptions
            channelToSubId.clear();
            std::cout << "You are now logged out." << std::endl;
            return "DISCONNECT\nreceipt:" + std::to_string(recId) + "\n\n";
        }
    }

    if (command == "logout") {
        int recId = receiptCounter++;
        receiptIdToCommand[recId] = "logout";
        this->currentUsername = "";  // Clear username on disconnect
        subIdToChannel.clear();       // Clear subscriptions
        channelToSubId.clear();
        std::cout << "You are now logged out." << std::endl;
        return "DISCONNECT\nreceipt:" + std::to_string(recId) + "\n\n";
    }
    if (command == "report") {
        names_and_events n_e = parseEventsFile(words[1]);
        std::string allFrames = "";
        
        for (const Event& e : n_e.events) {
            allFrames += "SEND\ndestination:/" + e.get_team_a_name() + "_" + e.get_team_b_name() + "\n\n";
            
            // Body: all the event data
            allFrames += "user:" + currentUsername + "\n";
            allFrames += "team a:" + e.get_team_a_name() + "\n";
            allFrames += "team b:" + e.get_team_b_name() + "\n";
            allFrames += "event name:" + e.get_name() + "\n";
            allFrames += "time:" + std::to_string(e.get_time()) + "\n";
            
            allFrames += "general game updates:\n";
            for (auto const& [key, val] : e.get_game_updates()) {
                allFrames += "    " + key + ":" + val + "\n";
            }
            
            allFrames += "team a updates:\n";
            for (auto const& [key, val] : e.get_team_a_updates()) {
                allFrames += "    " + key + ":" + val + "\n";
            }
            
            allFrames += "team b updates:\n";
            for (auto const& [key, val] : e.get_team_b_updates()) {
                allFrames += "    " + key + ":" + val + "\n";
            }
            
            allFrames += "description:\n" + e.get_description() + "\n";
            allFrames += '\0'; 
        }
        std::cout << "Events reported successfully" << std::endl;
        return allFrames;
    }
    if (command == "summary") {
    std::string gameName = words[1];
    std::string targetUser = words[2];
    std::string filePath = words[3];

    std::ofstream outFile(filePath);
    auto it = gameReports.find(gameName);
    if (it != gameReports.end()) {
        std::vector<Event>& events = it->second;
        if (!events.empty()) {
            outFile << events[0].get_team_a_name() << " vs " << events[0].get_team_b_name() << "\n";
        }

        outFile << "Game stats:\nGeneral stats:\n";
        std::map<std::string, std::string> lastGeneralStats;
        std::map<std::string, std::string> lastTeamAStats;
        std::map<std::string, std::string> lastTeamBStats;

        for (const Event& e : events) {
            if (e.get_event_owner() == targetUser) { 
                for (auto const& [key, val] : e.get_game_updates()) lastGeneralStats[key] = val;
                for (auto const& [key, val] : e.get_team_a_updates()) lastTeamAStats[key] = val;
                for (auto const& [key, val] : e.get_team_b_updates()) lastTeamBStats[key] = val;
            }
        }

        for (auto const& [key, val] : lastGeneralStats) outFile << "    " << key << ": " << val << "\n";
        outFile << "Team a stats:\n";
        for (auto const& [key, val] : lastTeamAStats) outFile << "    " << key << ": " << val << "\n";
        outFile << "Team b stats:\n";
        for (auto const& [key, val] : lastTeamBStats) outFile << "    " << key << ": " << val << "\n";
        outFile << "Game event reports:\n";
        for (const Event& e : events) {
            if (e.get_event_owner() == targetUser) {
                outFile << e.get_time() << " - " << e.get_name() << ":\n\n";
                outFile << e.get_description() << "\n\n";
            }
        }
    } else {
        outFile << "No events found for channel " << gameName << "\n";
    }
    outFile.close();
    return ""; 
}
    return "";
}
void StompProtocol::processResponse(std::string frame) {
    std::lock_guard<std::mutex> lock(_mutex);
    std::vector<std::string> lines = split(frame, '\n');
    if (lines.empty()) return;

    std::string stompCommand = lines[0];

    if (stompCommand == "CONNECTED") {
        std::cout << "Login successful" << std::endl;
    }
    else if (stompCommand == "RECEIPT") {
        for (std::string& line : lines) {
            if (line.find("receipt-id:") == 0) {
                int rId = std::stoi(line.substr(11));
                std::cout << receiptIdToCommand[rId] << std::endl;
                
                if (receiptIdToCommand[rId] == "logout") {
                    shouldTerminate = true;
                    currentUsername = "";
                    subscriptionCounter = 0;
                    receiptCounter = 0;
                    channelToSubId.clear();
                    subIdToChannel.clear();
                }
            }
        }
    }
    else if (stompCommand == "MESSAGE") {
    std::string gameName = "";
    for (const auto& line : lines) {
        if (line.find("destination:/") == 0) {
            gameName = line.substr(13);
            break;
        }
    }

    size_t bodyPos = frame.find("\n\n");
    if (bodyPos != std::string::npos) {
        std::string body = frame.substr(bodyPos + 2);

        // Remove null terminator if present
        if (!body.empty() && body.back() == '\0') {
            body.pop_back();
        }

        std::istringstream stream(body);
        std::string line;
        
        std::string user, teamA, teamB, eventName, description;
        int time = 0;
        std::map<std::string, std::string> genUpdates, teamAUpdates, teamBUpdates;
        
        std::string currentSection = "";

        while (std::getline(stream, line)) {
            if (line.empty()) continue;
            
            if (line.find("user:") == 0) { user = line.substr(5); }
            else if (line.find("team a:") == 0 && line.find("team a updates") == std::string::npos) { teamA = line.substr(7); }
            else if (line.find("team b:") == 0 && line.find("team b updates") == std::string::npos) { teamB = line.substr(7); }
            else if (line.find("event name:") == 0) { eventName = line.substr(11); }
            else if (line.find("time:") == 0) { time = std::stoi(line.substr(5)); }
            
            else if (line.find("general game updates:") == 0) currentSection = "gen";
            else if (line.find("team a updates:") == 0) currentSection = "a";
            else if (line.find("team b updates:") == 0) currentSection = "b";
            else if (line.find("description:") == 0) currentSection = "desc";
            
            else if (currentSection == "desc") description += line + "\n";
            else if (line.find("    ") == 0) { 
                size_t colon = line.find(':');
                std::string key = line.substr(4, colon - 4);
                std::string val = line.substr(colon + 1);
                if (currentSection == "gen") genUpdates[key] = val;
                else if (currentSection == "a") teamAUpdates[key] = val;
                else if (currentSection == "b") teamBUpdates[key] = val;
            }
        }

        Event e(teamA, teamB, eventName, time, genUpdates, teamAUpdates, teamBUpdates, description);
        e.set_event_owner(user); 
        gameReports[gameName].push_back(e);
    }
}
    else if (stompCommand == "ERROR") {
        std::cout << "Error from server: " << frame << std::endl;
        shouldTerminate = true;
    }
}
bool StompProtocol::isTerminated() const {
    std::lock_guard<std::mutex> lock(_mutex);
    return shouldTerminate;
}