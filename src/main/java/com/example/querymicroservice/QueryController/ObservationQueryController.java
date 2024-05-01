package com.example.querymicroservice.QueryController;

import com.example.querymicroservice.QueryService.ObservationQueryService;
import com.example.querymicroservice.domain.Observation;
import com.example.querymicroservice.dtos.ObservationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/observation")
public class ObservationQueryController
{
    @Autowired
    ObservationQueryService observationQueryService;

    @GetMapping("/{id}")
    public ResponseEntity<ObservationDTO> getCondition(@PathVariable("id") Long id) { //check here
        try {
            return ResponseEntity.ok(observationQueryService.consumeReadEvent(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
