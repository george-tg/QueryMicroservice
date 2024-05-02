package com.example.querymicroservice.QueryService;


import com.example.querymicroservice.QueryRepository.ConditionRepository;
import com.example.querymicroservice.domain.Condition;
import com.example.querymicroservice.domain.Observation;
import com.example.querymicroservice.domain.Patient;
import com.example.querymicroservice.dtos.ConditionDTO;
import com.example.querymicroservice.dtos.ObservationDTO;
import com.example.querymicroservice.dtos.PatientDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConditionQueryService
{
    private static final Logger logger = LoggerFactory.getLogger(ConditionQueryService.class);

    @Autowired
    private ConditionRepository repository;
    @Autowired
    private ObjectMapper objectMapper;

    public ConditionDTO consumeReadEvent(Long id) {
        Condition condition = repository.findById(id).orElse(null);
        if (condition != null) {
            ConditionDTO conditionDTO = convertConditionToDTO(condition);
            logger.info("Getting condition: "+ condition.getId() +condition.getConditionName());
            return conditionDTO;
        } else {
            System.out.println("Condition not found for ID: " + id);
            return null;
        }
    }
    static ConditionDTO convertConditionToDTO(Condition condition) {

        return new ConditionDTO(condition.getId(), condition.getConditionName(), PatientQueryService.convertToDTO(condition.getPatient()));
    }



    @KafkaListener(topics = "delete_condition_event", groupId = "condition_group")
    public void handleDeleteConditionEvent(Long id) {
        repository.deleteById(id);
    }

    @KafkaListener(topics = "create_condition_event", groupId = "condition_group")
    public void handleCreateConditionEvent(String jsonPayload) {
        try {
            ConditionDTO conditionDTO = objectMapper.readValue(jsonPayload, ConditionDTO.class);
            List<String> scopes = conditionDTO.getAccessTokenUser().getScopes();
            if(scopes.size() == 1 && scopes.get(0).equals("condition")) {
                Condition condition = new Condition(conditionDTO.getConditionName(), new Patient(conditionDTO.getPatient().getId(), conditionDTO.getPatient().getFirstName(),conditionDTO.getPatient().getLastName(),conditionDTO.getPatient().getAge()));
                repository.save(condition);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing ConditionDTO to JSON while creating condition: " + e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "update_condition_event", groupId = "condition_group")
    public void handleUpdateConditionEvent(ConditionDTO conditionDTO)
    {
        Long id = conditionDTO.getId();

        Patient p = new Patient(conditionDTO.getPatient().getId(),conditionDTO.getPatient().getFirstName(),conditionDTO.getPatient().getLastName(),conditionDTO.getPatient().getAge());
        Condition condition = new Condition(conditionDTO.getConditionName(), p);
        Optional<Condition> existingCondition = repository.findById(id);
        if (existingCondition.isPresent()) {
            condition.setId(id);
            repository.save(condition);
        } else {
            throw new RuntimeException("Can't update condition " + id);
        }
    }


}
