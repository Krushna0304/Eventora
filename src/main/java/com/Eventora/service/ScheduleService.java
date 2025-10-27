package com.Eventora.service;

import com.Eventora.entity.Event;
import com.Eventora.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class ScheduleService {

    @Autowired
    private EventRepository eventRepository;

}
