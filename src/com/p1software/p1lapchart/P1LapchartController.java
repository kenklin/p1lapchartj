package com.p1software.p1lapchart;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
//import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Controller
public class P1LapchartController {

  // e.g., http://localhost:8080/p1lapchartj/api/123.html
  // @see http://docs.spring.io/spring/docs/3.2.4.RELEASE/spring-framework-reference/htmlsingle/#mvc-ann-responsebody
  @RequestMapping(value="/api/{id}", method=RequestMethod.GET)
  @ResponseBody
  public String getByID(@PathVariable String id, Model model) {
	String sourceurl = getSource(id);  

    RestTemplate restTemplate = new RestTemplate();
    String mylaps_json = restTemplate.getForObject(sourceurl, String.class);

    JsonNode json = enhance(mylaps_json, sourceurl);
System.out.println(json);
    
    return json.toString();
  }
  
  private String getSource(String id) {
    return "http://www.mylaps.com/api/eventlapchart?id=" + id;
  }

  // @see http://wiki.fasterxml.com/JacksonInFiveMinutes
  private JsonNode enhance(String mylaps_json, String source) {
	JsonNode rootNode = null;//String json = null;
    try {
      ObjectMapper m = new ObjectMapper();
      rootNode = m.readTree(mylaps_json);

      // Add data.meta
      ObjectNode metaObj = ((ObjectNode)rootNode).putObject("meta");
      metaObj.put("status", "0");
      metaObj.put("source", source);

      JsonNode lapchartNode = rootNode.path("lapchart");
      if (lapchartNode instanceof ObjectNode) {
        // Add data.laps by parsing data.lapchart.positions
/*
  	  data.p1laps = {};
  	  for (var position=0; position<data.lapchart.positions.length; position++) {
  		for (var lap=0; lap<data.lapchart.laps.length; lap++) {
  			if (data.lapchart.positions[position][lap] != undefined) {
  				var startNumber = data.lapchart.positions[position][lap].startNumber;
  				if (data.lapchart.positions[position][lap] != undefined && startNumber != undefined && startNumber != "") {
  					if (data.p1laps[startNumber] == undefined) {
  						data.p1laps[startNumber] = [];
  					}
  					data.p1laps[startNumber][lap] = position+1;
  				}
  			}
  		}
  	}
*/
        ObjectNode p1lapsObj = ((ObjectNode)rootNode).putObject("p1laps");
        ObjectNode positionsObj = ((ObjectNode)p1lapsObj).putObject("positions");
        int position = 0;
        for (JsonNode positionNode : lapchartNode.path("positions")) {
          if (positionNode.isArray()) {
            int lap = 0;
            for (JsonNode p : (ArrayNode)positionNode) {
              int startNumber = p.path("startNumber").asInt();
System.out.println(startNumber);
              if (p1lapsObj.isArray()) {
System.out.println();                
              }
              lap++;
            }
System.out.println();
          }
          position++;
        }
      
        // Delete properties from original mylaps.com JSON that we don't use
        ((ObjectNode)lapchartNode).remove("laps");
        ((ObjectNode)lapchartNode).remove("positions");
        JsonNode participantsNode = lapchartNode.path("participants");
        if (participantsNode.isArray()) {
          for (JsonNode participantNode : (ArrayNode)participantsNode) {
            if (participantNode.isObject()) {
    	      ((ObjectNode)participantNode).remove("color");
            }
          }
        }
      } // lapchartNode

    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return rootNode;
  }
}
