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

    try {
      ObjectMapper m = new ObjectMapper();
      JsonNode rootNode = m.readTree(mylaps_json);

      ObjectNode lapchartNode = (ObjectNode)rootNode.path("lapchart");
      lapchartNode.remove("laps");
      lapchartNode.remove("positions");
System.out.println(rootNode.toString());
    } catch (IOException e) {
System.err.println(e);
    }
    
    String json = enhance(mylaps_json);
    
    return json;
  }
  
  private String getSource(String id) {
    return "http://www.mylaps.com/api/eventlapchart?id=" + id;
  }

  private String enhance(String mylaps) {
	return mylaps;
  }
}
