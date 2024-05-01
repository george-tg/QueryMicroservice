package com.example.querymicroservice.QueryController;


import com.example.querymicroservice.QueryService.EncounterQueryService;
import com.example.querymicroservice.dtos.EncounterDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/encounter")
public class EncounterQueryController {
    @Autowired
    EncounterQueryService encounterQueryService;
    @GetMapping("/{id}")
    public ResponseEntity<EncounterDTO> getCondition( @PathVariable Long id) { //check here
        try {

            return ResponseEntity.ok(encounterQueryService.getEncounter(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
