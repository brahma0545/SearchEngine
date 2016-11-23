package com.raudra.searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class SearchController {


    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public String serverTest() {
        return "satheesh";
    }


}
