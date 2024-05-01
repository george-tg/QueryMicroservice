package com.example.querymicroservice.QueryController;


import com.example.querymicroservice.QueryService.ConditionQueryService;
import com.example.querymicroservice.domain.Condition;
import com.example.querymicroservice.dtos.ConditionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/condition")
public class ConditionQueryController
{
    @Autowired
    private ConditionQueryService conditionQueryService;
    @GetMapping("/{id}")
    public ResponseEntity<ConditionDTO> getCondition(@PathVariable("id") Long id) { //check here
        try {

            return ResponseEntity.ok(conditionQueryService.consumeReadEvent(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
