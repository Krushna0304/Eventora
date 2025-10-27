package com.Eventora.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RestAPIService {
    @Autowired
    private RestTemplate restTemplate;

    public String classifyCity(String cityName)
    {
        return "small";
//        return restTemplate.getForObject("https://api.example.com/classify?city=" + cityName, String.class);
    }
}
