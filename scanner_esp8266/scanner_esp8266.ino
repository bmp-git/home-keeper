#include <ESP8266WiFi.h>
#include <RCSwitch.h>
#include <map>
#include <array>
#include "sdk_structs.h"
#include "ieee80211_structs.h"

#define MAX_CHANNEL 14
#define WINDOW_WIFI_TIME_MS 200
#define SEND_WIFI_TIME_MS 5000

RCSwitch rf433 = RCSwitch();

typedef std::array<uint8_t, 6> mac_addr_t;
std::map<mac_addr_t, int8_t> mac_map;

unsigned int channel = 1;
unsigned int counter = 0;

int free_to_write = 1;

void wifi_sniffer_packet_handler(uint8_t *buff, uint16_t len)
{
  if (!free_to_write) {
    return;
  }
  
  const wifi_promiscuous_pkt_t *ppkt = (wifi_promiscuous_pkt_t *)buff;
  const wifi_ieee80211_packet_t *ipkt = (wifi_ieee80211_packet_t *)ppkt->payload;
  const wifi_ieee80211_mac_hdr_t *hdr = &ipkt->hdr;

  mac_addr_t source_mac_addr = {hdr->addr2[0], hdr->addr2[1], hdr->addr2[2], hdr->addr2[3], hdr->addr2[4], hdr->addr2[5]};
  std::map<mac_addr_t, int8_t>::iterator record = mac_map.find(source_mac_addr);
  if (record != mac_map.end()) {
    record->second = (int8_t)ppkt->rx_ctrl.rssi > (int8_t)record->second ? (int8_t)ppkt->rx_ctrl.rssi : (int8_t)record->second; 
  } else {
    mac_map.insert(std::make_pair(source_mac_addr, (int8_t)ppkt->rx_ctrl.rssi));
  }
}

void setup() { 
  Serial.begin(115200);
  delay(20);
  wifi_set_channel(channel);
  wifi_set_opmode(STATION_MODE);
  wifi_promiscuous_enable(0);
  WiFi.disconnect();
  wifi_set_promiscuous_rx_cb(wifi_sniffer_packet_handler);
  wifi_promiscuous_enable(1);
  rf433.enableReceive(2);
}

void loop() { 
   delay(1);
   counter++;
   if (counter % WINDOW_WIFI_TIME_MS == 0) { 
     channel++;
     channel = channel > MAX_CHANNEL ? 1 : channel; 
     wifi_set_channel(channel);
   }
   
   if (counter % SEND_WIFI_TIME_MS == 0) { 
     free_to_write = 0;
     Serial.print("{\"type\":\"wifi\",\"data\":[");
     int first = 1;
     for (auto const& x : mac_map) {
       if(!first) {
         Serial.print(",");
       }
       Serial.printf("{\"mac\":\"%02x%02x%02x%02x%02x%02x\",\"rssi\":%d}", x.first[0], x.first[1], x.first[2], x.first[3], x.first[4], x.first[5], x.second);
       first = 0;
     }
     Serial.println("]}");
     mac_map.clear();
     free_to_write = 1;
   }

   if (rf433.available()) {
      Serial.print("{\"type\":\"433\",\"data\":");
      Serial.printf("{\"code\":\"%06x\",\"pulselength\":%u,\"proto\":%u}", rf433.getReceivedValue(), rf433.getReceivedDelay(), rf433.getReceivedProtocol());
      Serial.println("}");
      rf433.resetAvailable();
   }   

} 
