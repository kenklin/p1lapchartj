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

import com.fasterxml.jackson.core.JsonProcessingException;
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
	String url = getSource(id);  

    RestTemplate restTemplate = new RestTemplate();
    String mylaps_json = restTemplate.getForObject(url, String.class);

    String json = enhance(mylaps_json);
    
    return json;
  }
  
  private String getSource(String id) {
    return "http://www.mylaps.com/api/eventlapchart?id=" + id;
  }

  private String enhance(String mylaps_json) {
	String json = null;
    try {
      ObjectMapper m = new ObjectMapper();
      JsonNode rootNode = m.readTree(mylaps_json);

      // Delete properties from original mylaps.com JSON that we don't use
      JsonNode lapchartJson = rootNode.path("lapchart");
      ObjectNode lapchartObj = (ObjectNode)lapchartJson;
      lapchartObj.remove("laps");
      lapchartObj.remove("positions");
      try {
        ArrayNode participantsArray = (ArrayNode)lapchartJson.path("participants");
        for (JsonNode participantJson : participantsArray) {
    	  ObjectNode participantObj = (ObjectNode)participantJson;
    	  participantObj.remove("color");
        }
      } catch (Exception e) {
        // Might be com.fasterxml.jackson.databind.node.MissingNode
    	e.printStackTrace();
      }

      json = rootNode.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
System.out.println(json);
    
    return json;
  }
}
