package com.p1software.p1lapchart;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;

@Controller
public class P1LapchartController {

  // e.g., http://localhost:8080/p1lapchartj/api/123.html
  // @see http://docs.spring.io/spring/docs/3.2.4.RELEASE/spring-framework-reference/htmlsingle/#mvc-ann-responsebody
  @RequestMapping(value="/api/{id}", method=RequestMethod.GET)
  @ResponseBody
  public String getByID(@PathVariable String id, Model model) {
	String url = getSource(id);  
    return "{\"id\":\"" + id + "\", \"url\":\"" + url + "\"}";
  }
  
  private String getSource(String id) {
    return "http://www.mylaps.com/api/eventlapchart?id=" + id;
  }

}
